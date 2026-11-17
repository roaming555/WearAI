package com.foggland.wearai.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// 品牌主色（浅蓝 #2C5197，保留用于图标/强调等少量场景）
// ---------------------------------------------------------------------------

val BrandPrimary = Color(0xFF2C5197)
val BrandOnPrimary = Color(0xFFFFFFFF)
val BrandPrimaryContainer = Color(0xFFD6E3F7)
val BrandOnPrimaryContainer = Color(0xFF08244A)

// ---------------------------------------------------------------------------
// 纸色（未启用主题系统时的基础色）
// ---------------------------------------------------------------------------

val PaperBackground = Color(0xFFFAF7F0)
val PaperSurface = Color(0xFFFCFAF6)
val PaperSurfaceVariant = Color(0xFFEFE8DB)
val PaperOutline = Color(0xFF7C7669)
val PaperOutlineVariant = Color(0xFFD6CEC0)

val PaperInk = Color(0xFF211F1A)
val PaperInkVariant = Color(0xFF5B574E)

val PaperSecondary = Color(0xFF595D6E)
val PaperOnSecondary = Color(0xFFFFFFFF)
val PaperSecondaryContainer = Color(0xFFE0E2F0)
val PaperOnSecondaryContainer = Color(0xFF161A2E)
val PaperTertiary = Color(0xFF735573)
val PaperOnTertiary = Color(0xFFFFFFFF)
val PaperTertiaryContainer = Color(0xFFF8D8F5)
val PaperOnTertiaryContainer = Color(0xFF2C112C)

val DarkBackground = Color(0xFF14120F)
val DarkSurface = Color(0xFF1C1915)
val DarkSurfaceVariant = Color(0xFF2A2721)
val DarkOnSurfaceVariant = Color(0xFFD4CEC2)

// ---------------------------------------------------------------------------
// 印象派邻近色主题
//
// 设计取向：低对比、邻近色相、低饱和、带灰雾感，模拟户外柔光与空气透视。
// 每套主题给出「工具栏 / 消息 / 输入区」三个区域各自的颜色，三色互为邻近色，
// 不做互补对冲，保证界面安静、耐看。
// ---------------------------------------------------------------------------

/** 一套印象派邻近色主题的生成参数。 */
data class ImpressionistPalette(
    val id: String,
    val label: String,
    /** 基础色相（0~360） */
    val baseHue: Float,
    /** 邻近色相偏移，越大三色区分越明显 */
    val hueSpread: Float = 22f,
    /** 浅色模式的基准饱和度（压低但保留色彩倾向，呈现柔光灰雾感） */
    val lightSaturation: Float = 0.30f,
    /** 深色模式的基准饱和度 */
    val darkSaturation: Float = 0.22f,
)

val IMPRESSIONIST_PALETTES = listOf(
    ImpressionistPalette("mist", "晨雾蓝", 206f, 26f, 0.30f),
    ImpressionistPalette("lavender", "薰衣草暮", 268f, 24f, 0.27f),
    ImpressionistPalette("celadon", "青瓷", 170f, 22f, 0.26f),
    ImpressionistPalette("wheat", "麦浪", 40f, 22f, 0.33f),
    ImpressionistPalette("rose", "玫瑰灰", 8f, 24f, 0.28f),
    ImpressionistPalette("sage", "鼠尾草", 128f, 22f, 0.24f),
    ImpressionistPalette("graphite", "石墨", 218f, 12f, 0.10f),
)

/** 三个界面区域的配色。文字色由所在底色亮度推导，避免深色模式下出现黑字看不清。 */
data class SurfaceTrio(
    val topBar: Color,
    val onTopBar: Color,
    val message: Color,
    val onMessage: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val input: Color,
    val onInput: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val onBackground: Color,
    val outline: Color,
)

/** 计算颜色的色相（0~360）与饱和度（0~1），无 API 级别依赖，兼容 Android 5.0。 */
fun Color.hueAndSaturation(): Pair<Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta <= 0.0001f) return 0f to 0f
    val lightness = (max + min) / 2f
    val saturation = if (lightness > 0.5f) {
        delta / (2f - max - min)
    } else {
        delta / (max + min)
    }
    val hue = when (max) {
        r -> (60f * ((g - b) / delta) + 360f) % 360f
        g -> 60f * ((b - r) / delta) + 120f
        else -> 60f * ((r - g) / delta) + 240f
    }
    return hue to saturation
}

private fun tone(hue: Float, saturation: Float, lightness: Float): Color = Color.hsl(
    hue = ((hue % 360f) + 360f) % 360f,
    saturation = saturation.coerceIn(0f, 1f),
    lightness = lightness.coerceIn(0f, 1f),
)

/** 按底色亮度挑选浅墨或深墨，保证文字始终清晰可读。 */
private fun inkOn(surface: Color, hue: Float): Color {
    val luminance = 0.299f * surface.red + 0.587f * surface.green + 0.114f * surface.blue
    return if (luminance < 0.52f) {
        tone(hue, 0.20f, 0.955f) // 浅墨
    } else {
        tone(hue, 0.40f, 0.150f) // 深墨
    }
}

/** 解析出当前明暗模式下的三区配色。 */
fun ImpressionistPalette.resolve(dark: Boolean): SurfaceTrio {
    val h = baseHue
    val spread = hueSpread
    val s = if (dark) darkSaturation else lightSaturation

    val topBar = if (dark) tone(h - spread, s * 0.90f, 0.170f) else tone(h - spread, s * 0.85f, 0.885f)
    val message = if (dark) tone(h, s, 0.220f) else tone(h, s, 0.930f)
    val userBubble = if (dark) {
        tone(h + spread * 0.55f, s * 1.15f, 0.290f)
    } else {
        tone(h + spread * 0.55f, s * 1.15f, 0.855f)
    }
    val input = if (dark) tone(h + spread, s * 0.95f, 0.252f) else tone(h + spread, s * 0.95f, 0.870f)
    val background = if (dark) tone(h, s * 0.60f, 0.125f) else tone(h, s * 0.45f, 0.966f)
    val accent = if (dark) tone(h, s * 1.70f, 0.685f) else tone(h, s * 1.90f, 0.430f)
    val outline = if (dark) tone(h, s * 0.80f, 0.430f) else tone(h, s * 0.90f, 0.720f)

    return SurfaceTrio(
        topBar = topBar,
        onTopBar = inkOn(topBar, h),
        message = message,
        onMessage = inkOn(message, h),
        userBubble = userBubble,
        onUserBubble = inkOn(userBubble, h),
        input = input,
        onInput = inkOn(input, h),
        accent = accent,
        onAccent = inkOn(accent, h),
        background = background,
        onBackground = inkOn(background, h),
        outline = outline,
    )
}

/** 自定义主题可选的颜色。 */
val CUSTOM_SEED_PALETTE = listOf(
    Color(0xFF3E6E9E),
    Color(0xFF2F7F86),
    Color(0xFF3F7F62),
    Color(0xFF6E8A46),
    Color(0xFFB08144),
    Color(0xFFB5705A),
    Color(0xFFA9617E),
    Color(0xFF7E6BA8),
    Color(0xFF6D6F9E),
    Color(0xFF8A7A6D),
)
