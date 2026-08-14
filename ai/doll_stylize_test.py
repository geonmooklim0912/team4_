"""
인형 사진 -> 유아틱 2D 캐릭터 변환 비교 테스트

사용법:
    # 1) 프로젝트 루트에 .env 파일 만들고 키 넣기
    #    GEMINI_API_KEY=AIza...
    # 2) 실행
    ../.venv/bin/python doll_stylize_test.py 인형사진.jpg

    # 사용 가능한 모델 확인만 하고 싶을 때
    ../.venv/bin/python doll_stylize_test.py --list-models

결과: out/ 폴더에 단계별 PNG + report.json (소요시간 기록)
"""

import argparse
import io
import json
import os
import sys
import time
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "out"


# ---------------------------------------------------------------------------
# .env 로더 (python-dotenv 의존성 없이)
# ---------------------------------------------------------------------------
def load_env():
    for candidate in (ROOT / ".env", ROOT.parent / ".env"):
        if not candidate.exists():
            continue
        for line in candidate.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            os.environ.setdefault(key.strip(), val.strip().strip("\"'"))


# ---------------------------------------------------------------------------
# 프롬프트 - 품질의 8할입니다. 결과 보면서 계속 튜닝하세요.
# ---------------------------------------------------------------------------
STYLE_PROMPT = """\
Convert this photo of a plush toy into a 2D children's picture-book illustration.

CRITICAL - keep the exact same character:
- Same colors, same proportions, same distinctive features (ears, patterns, accessories)
- A child must instantly recognize it as their own toy

Style:
- Soft rounded shapes, thick clean outlines, flat pastel shading
- Cute and friendly, suitable for a 3-6 year old
- No photo texture, no fabric fuzz, no realistic lighting

Ignore and remove these - they are manufacturing artifacts, not part of the character:
- Care labels, brand tags, sewn-in fabric tags, barcodes, any printed text
- Dust, stains, seam lines, price stickers

Composition:
- Full body, front-facing, centered, standing pose
- Plain solid white background
- No shadow, no props, no text, no watermark
"""

# v3 - v1 기반 + 최소한의 캐릭터화. "재설계 금지"를 명시해 v2의 실패(정체성 상실)를 차단.
PROMPT_V3 = """\
Convert this photo of a plush toy into a 2D children's picture-book illustration.

CRITICAL - keep the exact same character:
- Same colors, same proportions, same distinctive features (ears, patterns, accessories)
- A child must instantly recognize it as their own toy

Add ONLY these two things to make it feel alive:
- A small friendly smiling mouth
- Small white highlights in the existing eyes

Keep everything else EXACTLY as in the photo:
- Same body shape, same proportions, same silhouette
- Same flippers/limbs - do NOT turn them into arms, hands, or humanlike legs
- Same shell/body pattern - do NOT invent a new pattern
- Do NOT redesign the character
- Do NOT turn it into a generic cartoon mascot
- Do NOT change the head size or make it chibi

Style:
- Soft rounded shapes, thick clean outlines, flat pastel shading
- Cute and friendly, suitable for a 3-6 year old
- No photo texture, no fabric fuzz, no realistic lighting

Ignore and remove these - they are manufacturing artifacts, not part of the character:
- Care labels, brand tags, sewn-in fabric tags, barcodes, any printed text
- Dust, stains, seam lines, price stickers

Composition:
- Full body, centered, plain solid white background
- No shadow, no props, no text, no watermark
"""

# v2 - 캐릭터화 강화안(실패: 정체성 상실). 결과물: ai/out/02_flash_v2.png
# 큰 눈 + 웃는 입 + 치비 비율 + "서 있는 자세" 강제. 쓰려면 STYLE_PROMPT 를 이걸로 교체.
PROMPT_V2 = """\
Convert this photo of a plush toy into a 2D children's picture-book illustration.

CRITICAL - keep the exact same character:
- Same colors, same proportions, same distinctive features (patterns, shell, fins, accessories)
- A child must instantly recognize it as their own toy
- Keep the shell/body pattern crisp and clearly defined, not blurred away

Bring it to life as a friendly character:
- Big round friendly eyes with white highlights
- A small smiling mouth
- Slightly chibi proportions (a little oversized head)
- It should look happy and approachable, like a cartoon friend for a 3-6 year old

Pose - IMPORTANT:
- Redraw the character as if it is ALIVE and STANDING UPRIGHT on its hind legs
- Facing the viewer straight on, at eye level
- NOT lying flat, NOT seen from above, NOT a top-down view

Style:
- Soft rounded shapes, thick clean outlines, flat pastel shading
- No photo texture, no fabric fuzz, no realistic lighting

Ignore and remove these - they are manufacturing artifacts, not part of the character:
- Care labels, brand tags, sewn-in fabric tags, barcodes, any printed text
- Dust, stains, seam lines, price stickers

Composition:
- Full body, centered, plain solid white background
- No shadow, no props, no text, no watermark
"""

# 모션용 포즈 시트 (기획서: 통통 튀기 / 반 접히기 / 오뚝이)
POSE_PROMPTS = {
    "jump": "Same character, same illustration style. Now in a joyful mid-air jumping pose with arms up. Plain solid white background, no shadow.",
    "bow": "Same character, same illustration style. Now bent forward at the waist, like folding in half. Plain solid white background, no shadow.",
    "tilt": "Same character, same illustration style. Now tilted 20 degrees to one side, like a roly-poly toy. Plain solid white background, no shadow.",
}

GEMINI_MODELS = ["gemini-3.1-flash-image", "gemini-3-pro-image"]


# ---------------------------------------------------------------------------
# 배경 제거
# ---------------------------------------------------------------------------
_bg_session = None


def remove_bg(img: Image.Image) -> Image.Image:
    global _bg_session
    from rembg import new_session, remove

    if _bg_session is None:
        # birefnet-general: 일반 사물에 강함 (u2net보다 경계 품질 우수)
        _bg_session = new_session("birefnet-general")
    return remove(img, session=_bg_session)


def flatten_on_white(img: Image.Image) -> Image.Image:
    """투명 PNG를 AI 입력용 흰 배경 RGB로 합성.
    알파를 그대로 넣으면 모델이 검은 배경으로 해석하는 경우가 있음."""
    if img.mode != "RGBA":
        return img.convert("RGB")
    white = Image.new("RGB", img.size, (255, 255, 255))
    white.paste(img, mask=img.split()[-1])
    return white


# ---------------------------------------------------------------------------
# Gemini
# ---------------------------------------------------------------------------
def _gemini_client():
    from google import genai

    key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
    if not key:
        raise RuntimeError("GEMINI_API_KEY 없음 (.env 확인)")
    return genai.Client(api_key=key)


def list_models():
    client = _gemini_client()
    print("\n계정에서 사용 가능한 이미지 관련 모델:\n")
    for m in client.models.list():
        name = m.name.replace("models/", "")
        if "image" in name or "imagen" in name:
            print(f"  {name}")


def run_gemini(src: Image.Image, prompt: str, model: str) -> Image.Image:
    from google.genai import types

    client = _gemini_client()
    buf = io.BytesIO()
    src.save(buf, format="PNG")

    resp = client.models.generate_content(
        model=model,
        contents=[
            types.Part.from_bytes(data=buf.getvalue(), mime_type="image/png"),
            prompt,
        ],
    )

    cand = resp.candidates[0]
    for part in cand.content.parts:
        if part.inline_data:
            return Image.open(io.BytesIO(part.inline_data.data))
    raise RuntimeError(f"이미지 없음 (finish_reason={cand.finish_reason})")


# ---------------------------------------------------------------------------
# OpenAI (백업 후보 - 키 있을 때만 실행)
# ---------------------------------------------------------------------------
def run_openai(src: Image.Image, prompt: str, model: str = "gpt-image-2") -> Image.Image:
    import base64

    from openai import OpenAI

    client = OpenAI()
    buf = io.BytesIO()
    src.save(buf, format="PNG")
    buf.name = "doll.png"
    buf.seek(0)

    resp = client.images.edit(
        model=model,
        image=buf,
        prompt=prompt.replace("Plain solid white background", "Transparent background"),
        background="transparent",
        size="1024x1024",
    )
    return Image.open(io.BytesIO(base64.b64decode(resp.data[0].b64_json)))


# ---------------------------------------------------------------------------
def timed(log: list, label: str, fn, *args, **kwargs):
    t0 = time.perf_counter()
    try:
        result = fn(*args, **kwargs)
        dt = time.perf_counter() - t0
        print(f"  [OK]   {label:<32} {dt:6.2f}s")
        log.append({"step": label, "ok": True, "seconds": round(dt, 2)})
        return result
    except Exception as e:
        dt = time.perf_counter() - t0
        print(f"  [FAIL] {label:<32} {dt:6.2f}s  {type(e).__name__}: {e}")
        log.append({"step": label, "ok": False, "seconds": round(dt, 2), "error": f"{type(e).__name__}: {e}"})
        return None


def main():
    load_env()

    ap = argparse.ArgumentParser()
    ap.add_argument("image", nargs="?", help="인형 사진 경로")
    ap.add_argument("--list-models", action="store_true", help="사용 가능한 모델만 출력")
    args = ap.parse_args()

    if args.list_models:
        list_models()
        return
    if not args.image:
        ap.error("인형 사진 경로가 필요합니다")

    OUT.mkdir(exist_ok=True)
    log: list = []

    src_path = Path(args.image)
    original = Image.open(src_path).convert("RGB")
    print(f"\n입력: {src_path.name}  {original.size[0]}x{original.size[1]}")
    if min(original.size) < 768:
        print("  ⚠️  해상도가 낮습니다. 1024px 이상 권장")

    # 원본이 크면 축소 - rembg 속도 + API 업로드 비용 절감
    MAX_EDGE = 1536
    if max(original.size) > MAX_EDGE:
        ratio = MAX_EDGE / max(original.size)
        new_size = (round(original.size[0] * ratio), round(original.size[1] * ratio))
        original = original.resize(new_size, Image.LANCZOS)
        print(f"  → 리사이즈: {new_size[0]}x{new_size[1]}")

    # [1] 원본 누끼 -> 배경 오염 방지
    print("\nSTEP 1. 원본 배경 제거")
    cut = timed(log, "rembg (birefnet-general)", remove_bg, original)
    if cut is None:
        ai_input = original
    else:
        cut.save(OUT / "01_cutout.png")
        ai_input = flatten_on_white(cut)
        ai_input.save(OUT / "01_cutout_white.png")

    # [2] 스타일 변환
    print("\nSTEP 2. 스타일 변환")
    results: dict = {}
    for model in GEMINI_MODELS:
        img = timed(log, model, run_gemini, ai_input, STYLE_PROMPT, model)
        if img:
            img.save(OUT / f"02_{model}.png")
            results[model] = img

    if os.environ.get("OPENAI_API_KEY"):
        img = timed(log, "gpt-image-2", run_openai, ai_input, STYLE_PROMPT)
        if img:
            img.save(OUT / "02_gpt-image-2.png")
            results["gpt-image-2"] = img
    else:
        print("  [SKIP] OPENAI_API_KEY 없음 - Gemini만 비교")

    if not results:
        print("\n❌ 스타일 변환 전부 실패. 위 에러 메시지 확인 필요.")
        (OUT / "report.json").write_text(json.dumps(log, ensure_ascii=False, indent=2))
        sys.exit(1)

    # [3] 결과물 누끼 -> AR 빌보드용 투명 PNG
    print("\nSTEP 3. 결과물 배경 제거 (AR 빌보드용)")
    for name, img in results.items():
        out = timed(log, f"rembg <- {name}", remove_bg, img)
        if out:
            out.save(OUT / f"03_{name}_alpha.png")

    # [4] 포즈 시트 - 캐릭터 일관성 검증
    base_model = GEMINI_MODELS[0]
    if base_model in results:
        print("\nSTEP 4. 모션용 포즈 생성 (캐릭터 일관성 검증)")
        base = results[base_model]
        for pose, prompt in POSE_PROMPTS.items():
            img = timed(log, f"pose: {pose}", run_gemini, base, prompt, base_model)
            if img:
                img.save(OUT / f"04_pose_{pose}.png")
                alpha = remove_bg(img)
                alpha.save(OUT / f"04_pose_{pose}_alpha.png")

    (OUT / "report.json").write_text(json.dumps(log, ensure_ascii=False, indent=2))

    total = sum(e["seconds"] for e in log)
    print(f"\n{'='*52}")
    print(f"완료 -> {OUT}")
    print(f"총 소요: {total:.1f}s")
    print(f"{'='*52}")
    print("\n평가 체크리스트:")
    print("  [ ] 아이가 자기 인형이라고 알아볼 수 있는가?  (정체성)")
    print("  [ ] 그림체가 유아 친화적인가?")
    print("  [ ] 누끼 경계가 깔끔한가?  (털/실루엣)")
    print("  [ ] 포즈 4장의 캐릭터가 서로 동일한가?  (일관성)")


if __name__ == "__main__":
    main()
