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
    /** 首次启动自动适配屏幕（用户手动调过后置为 false） */
    val uiScaleAuto: Boolean = true,
    /** 主题预设 ID：brand/sakura/forest/sunset/violet/mono/dynamic/custom */
    val themeId: String = DEFAULT_THEME_ID,
    /** 自定义主题的种子颜色（ARGB） */
    val customSeed: Int = DEFAULT_CUSTOM_SEED,
    /** 自定义背景图片的本地文件绝对路径；null 表示无背景图 */
    val backgroundPath: String? = null,
    /** 背景图片的暗色蒙层不透明度（0~0.9），保证文字可读 */
    val backgroundDim: Float = DEFAULT_BACKGROUND_DIM,
    /** 是否启用「悬浮胶囊」样式的顶栏与输入栏（灵动岛风格） */
    val floatingBars: Boolean = true,
    /** 悬浮胶囊的圆角半径（dp），范围见 MIN/MAX_CAPSULE_RADIUS */
    val capsuleRadius: Float = DEFAULT_CAPSULE_RADIUS,    /** 是否给悬浮胶囊描边，提升在复杂背景/深色模式下的辨识度 */
    val capsuleBorder: Boolean = false,
    /** 高级材质：是否启用「有色毛玻璃」（导航栏 / 输入区 / 胶囊半透明透出内容） */
    val glassEnabled: Boolean = false,
    /** 毛玻璃浓度（0.35~0.95）：越大越乳白、越少透出背景 */
    val glassAlpha: Float = DEFAULT_GLASS_ALPHA,
    /** 高级材质实时模糊半径 */
    val glassBlurRadius: Float = DEFAULT_GLASS_BLUR,
    /** 显示栏：普通半透明（不依赖背景图） */
    val translucentBars: Boolean = false,
    /** 显示栏：半透明度 */
    val translucentAlpha: Float = DEFAULT_GLASS_ALPHA,
) {
    companion object {
        const val DEFAULT_API_KEY = "74a49e6b944a456eaf089f6ea8593302.yhD4E7YQKnRbRp6L"
        const val DEFAULT_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        const val DEFAULT_MODEL_CALLING_NAME = "glm-4.7-flash"
        const val DEFAULT_TITLE_MODEL_CALLING_NAME = "glm-4-flash"
        const val DEFAULT_SYSTEM_PROMPT = "你是 WearAI，一个运行在手机与智能手表上的 AI 助手。输出规范（必须严格遵守）：\n" +
            "1. 使用简体中文，回答简洁、准确。\n" +
            "2. 正文一律使用 Markdown 排版：标题用 ## ，无序列表用 - ，加粗用 **文本** ，行内代码用反引号，多行代码用三反引号代码块并在首行标注语言。\n" +
            "3. 数学公式只能使用 LaTeX 数学语法：行内公式写作 \$...\$ ，独立公式写作 \$\$...\$\$ 。\n" +
            "4. 严禁输出 LaTeX 文档结构：\\documentclass、\\usepackage、\\begin{document}、\\end{document}、\\section、\\subsection、\\begin{itemize}、\\item、\\maketitle 等一律不要出现。\n" +
            "5. 普通说明文字不要包进代码块，只有在展示代码时才使用代码块。"

        /** 历史版本的默认提示词；命中时会自动升级为 [DEFAULT_SYSTEM_PROMPT]。 */
        val LEGACY_DEFAULT_PROMPTS = setOf(
            "你是 WearAI，一个运行在手机与智能手表上的 AI 助手。请使用简体中文，简洁、准确地回答用户的问题。",
            "你是 WearAI，一个运行在手机与智能手表上的 AI 助手。请使用简体中文，简洁、准确地回答用户的问题。如需输出公式，请通过标准Latex格式输出",
            "你是 WearAI，一个运行在手机与智能手表上的 AI 助手。请使用简体中文，简洁、准确地回答用户的问题。正文请使用 Markdown 排版。如需输出数学公式，请使用 LaTeX 数学语法：行内公式用 \$...\$，独立公式用 \$\$...\$\$；不要输出整篇 LaTeX 文档（不要使用 \\documentclass、\\begin{document}、\\section、\\begin{itemize} 等文档结构命令）",
        )

        const val DEFAULT_UI_SCALE = 1.0f
        const val MIN_UI_SCALE = 0.3f
        const val MAX_UI_SCALE = 1.6f

        // ---- 主题 ----
        const val DEFAULT_THEME_ID = "mist"
        const val THEME_DYNAMIC = "dynamic"
        const val THEME_CUSTOM = "custom"
        const val DEFAULT_BACKGROUND_DIM = 0.55f
        val DEFAULT_CUSTOM_SEED = 0xFF2C5197.toInt()

        const val DEFAULT_CAPSULE_RADIUS = 28f
        const val MIN_CAPSULE_RADIUS = 8f
        const val MAX_CAPSULE_RADIUS = 28f

        /** 标题总结选「无」：不调用 AI，直接用日期时间命名 */
        const val TITLE_MODEL_NONE = "none"

        const val DEFAULT_GLASS_ALPHA = 0.72f
        const val MIN_GLASS_ALPHA = 0.35f
        const val MAX_GLASS_ALPHA = 0.95f

        const val DEFAULT_GLASS_BLUR = 12f
        const val MIN_GLASS_BLUR = 2f
        const val MAX_GLASS_BLUR = 30f

        /** 从完整 endpoint 推导 API 根地址，用于图像/视频等其它接口。 */
        fun apiRoot(endpointUrl: String): String {
            return endpointUrl
                .removeSuffix("/")
                .removeSuffix("/chat/completions")
        }
    }
}
