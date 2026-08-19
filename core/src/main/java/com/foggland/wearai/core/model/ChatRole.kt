package com.foggland.wearai.core.model

/**
 * 对话消息的角色。对应智谱对话补全接口中 messages[].role 字段。
 */
enum class ChatRole(val wireName: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
}
