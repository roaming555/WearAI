package com.foggland.wearai.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 对话补全请求体（对应 POST /api/paas/v4/chat/completions）。
 *
 * 字段说明：
 *  - model       模型调用名，必填
 *  - messages    消息数组，必填；role ∈ system/user/assistant/tool
 *  - stream      是否流式输出
 *  - temperature 采样温度（0~2，默认 0.95）
 *  - top_p       核采样（0~1，默认 0.7）
 *  - max_tokens  最大输出 token 数
 *  - thinking    深度思考开关 {"type": "enabled" | "disabled"}
 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageDto>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerializedName("top_p") val topP: Double? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val thinking: ThinkingDto? = null,
    val stop: List<String>? = null,
)

data class MessageDto(
    val role: String,
    val content: String,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
)

data class ThinkingDto(
    val type: String,
)

/** 非流式 / 流式 chunk 的响应结构（两者复用，流式时 delta 非空）。 */
data class ChatCompletionResponse(
    val id: String? = null,
    @SerializedName("object") val objectType: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val error: ErrorDto? = null,
)

data class ChoiceDto(
    val index: Int = 0,
    val message: MessageDto? = null,
    val delta: DeltaDto? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

data class DeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
)

data class UsageDto(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0,
)

data class ErrorDto(
    val code: String? = null,
    val message: String? = null,
)
