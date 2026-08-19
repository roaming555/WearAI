package com.foggland.wearai.core.model

/**
 * 自定义模型配置。
 *
 * @param id 本地唯一标识（预置模型直接使用调用名作为 id）
 * @param name 显示名称（如 “GLM-4.7-Flash”）
 * @param callingName 调用名（API 请求中 model 字段实际使用的值，如 “glm-4.7-flash”）
 */
data class ModelConfig(
    val id: String,
    val name: String,
    val callingName: String,
    /** 该模型专用的 API Key（留空则使用全局默认） */
    val apiKey: String = "",
    /** 该模型专用的接入地址（留空则使用全局默认） */
    val endpointUrl: String = "",
)
