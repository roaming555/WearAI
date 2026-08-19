package com.foggland.wearai.core.network

import com.foggland.wearai.core.model.Usage

/**
 * 流式对话过程中对外回调的事件。
 */
sealed interface ChatStreamEvent {
    /** 一次增量：正文与深度思考内容可能分别到达。 */
    data class Delta(val content: String, val reasoning: String) : ChatStreamEvent

    /** 流结束（携带 finish_reason 与 usage）。 */
    data class Finished(val finishReason: String?, val usage: Usage?) : ChatStreamEvent

    /** 请求失败。 */
    data class Failed(val message: String, val cause: Throwable? = null) : ChatStreamEvent
}

/** 一次完整对话（流式或非流式）的汇总结果。 */
data class StreamSummary(
    val finishReason: String? = null,
    val usage: Usage? = null,
)
