package com.example.team4uu.data.remote.dto


// 기본값을 둔 이유: Moshi 리플렉션 어댑터는 기본값 없는 필드가 응답에서 빠지면
// "Required value 'timing' missing" 으로 인형 등록을 통째로 실패시킨다.
// 서버는 지금 다섯 개를 다 보내지만(server/routes/stylize.py), 개발용 SPRITE_SET=0
// 이나 향후 응답 변경으로 하나만 빠져도 등록이 죽는다.
data class StylizeResponse(
    val sprites: List<String>,
    val sprite_map: Map<String, String>,
    val failed: List<String> = emptyList(),
    val elapsed_ms: Long = 0,
    val timing: Map<String, Any> = emptyMap()
)