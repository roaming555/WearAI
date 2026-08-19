package com.foggland.wearai.core.model

/**
 * 一条聊天消息（领域模型）。
 *
 * @param role 角色：system / user / assistant
 * @param content 正文内容（assistant 流式输出时逐步累加）
 * @param reasoningContent 深度思考内容（开启“深度思考”后模型返回的推理过程）
 * @param isStreaming 是否正在流式生成中（运行时标记，不参与发送）
 * @param isError 是否为错误消息（运行时标记）
 * @param finishReason 生成结束原因（stop / length / tool_calls 等）
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String = "",
    val reasoningContent: String = "",
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val finishReason: String? = null,
)
