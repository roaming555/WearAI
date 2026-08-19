package com.foggland.wearai.core.model

/**
 * 一个对话会话。
 *
 * @param id 会话唯一标识
 * @param title 会话标题（默认“新对话”，首次回答后由 AI 自动总结生成）
 * @param messages 会话消息列表
 * @param createdAt 创建时间戳
 * @param updatedAt 最后更新时间戳
 */
data class Conversation(
    val id: String,
    val title: String = DEFAULT_TITLE,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val DEFAULT_TITLE = "新对话"
    }
}
