package com.example.team4uu.data.remote.dto


data class StylizeResponse(
    val sprites: List<String>,
    val sprite_map: Map<String, String>,
    val failed: List<String>,
    val elapsed_ms: Long,
    val timing: Map<String, Any>
)