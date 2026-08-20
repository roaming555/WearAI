package com.foggland.wearai.core.model

/**
 * 全局配置。
 */
data class AppSettings(
    /** API Key（Bearer 鉴权） */
    val apiKey: String = DEFAULT_API_KEY,
    /** 对话补全接入地址（完整 endpoint） */
    val endpointUrl: String = DEFAULT_ENDPOINT,
    /** 系统提示词 */
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    /** 当前选中的模型（ModelConfig.id） */
    val activeModelId: String = DEFAULT_MODEL_CALLING_NAME,
    /** 标题总结所用的模型（ModelConfig.id） */
    val titleModelId: String = DEFAULT_TITLE_MODEL_CALLING_NAME,
    /** 是否开启深度思考（thinking.type = enabled/disabled） */
    val deepThinking: Boolean = true,
    /** 是否开启流式输出（stream = true/false） */
    val streaming: Boolean = true,
    /** 深色模式：true 强制深色，false 跟随系统 */
    val darkMode: Boolean = false,
    /** 采样温度 temperature，控制随机性（0~2，默认 0.95） */
    val temperature: Double = 0.95,
    /** 核采样 top_p（0~1，默认 0.7） */
    val topP: Double = 0.7,
    /** 最大输出 token 数 max_tokens */
    val maxTokens: Int = 4096,
    /** 全局界面缩放倍数（软件层面等比缩放，用于适配小屏普通手表） */
    val uiScale: Float = 1.0f,
) {
    companion object {
        const val DEFAULT_API_KEY = "74a49e6b944a456eaf089f6ea8593302.yhD4E7YQKnRbRp6L"
        const val DEFAULT_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        const val DEFAULT_MODEL_CALLING_NAME = "glm-4.7-flash"
        const val DEFAULT_TITLE_MODEL_CALLING_NAME = "glm-4-flash"
        const val DEFAULT_SYSTEM_PROMPT = "你是 WearAI，一个运行在手机与智能手表上的 AI 助手。请使用简体中文，简洁、准确地回答用户的问题。如需输出公式，请通过标准Latex格式输出"

        const val DEFAULT_UI_SCALE = 1.0f
        const val MIN_UI_SCALE = 0.3f
        const val MAX_UI_SCALE = 1.6f

        /** 从完整 endpoint 推导 API 根地址，用于图像/视频等其它接口。 */
        fun apiRoot(endpointUrl: String): String {
            return endpointUrl
                .removeSuffix("/")
                .removeSuffix("/chat/completions")
        }
    }
}
