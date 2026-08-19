package com.foggland.wearai.core.model

/**
 * Token 用量统计（对应接口返回的 usage 字段）。
 */
data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)
