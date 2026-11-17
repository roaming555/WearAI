package com.foggland.wearai.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.foggland.wearai.core.model.AppSettings

/** 当前生效的三区配色（工具栏 / 消息 / 输入区）。 */
val LocalSurfaceTrio = staticCompositionLocalOf<SurfaceTrio> {
    ImpressionistPalette("mist", "晨雾蓝", 206f).resolve(dark = false)
}

/** 设备是否支持莫奈动态取色（Android 12+）。 */
val dynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** 由三区配色推导 Material3 配色方案，供开关、滑块、Chip 等组件复用。 */
private fun colorSchemeFromTrio(trio: SurfaceTrio, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = trio.accent,
        onPrimary = trio.onAccent,
        primaryContainer = trio.userBubble,
        onPrimaryContainer = trio.onUserBubble,
        secondary = trio.accent,
        onSecondary = trio.onAccent,
        secondaryContainer = trio.message,
        onSecondaryContainer = trio.onMessage,
        tertiary = trio.accent,
        onTertiary = trio.onAccent,
        tertiaryContainer = trio.input,
        onTertiaryContainer = trio.onInput,
        background = trio.background,
        onBackground = trio.onBackground,
        surface = trio.input,
        onSurface = trio.onInput,
        surfaceVariant = trio.message,
        onSurfaceVariant = trio.onMessage,
        surfaceTint = trio.accent,
        outline = trio.outline,
        outlineVariant = trio.outline.copy(alpha = 0.42f),
        inverseSurface = trio.onBackground,
        inverseOnSurface = trio.background,
        inversePrimary = trio.accent,
    )
}

/** 动态取色模式下，把系统配色映射成三区配色（仍是低对比的邻近色）。 */
private fun dynamicTrio(context: Context, dark: Boolean): SurfaceTrio {
    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return SurfaceTrio(
        topBar = scheme.surfaceContainerHigh,
        onTopBar = scheme.onSurface,
        message = scheme.surfaceContainerLow,
        onMessage = scheme.onSurface,
        userBubble = scheme.secondaryContainer,
        onUserBubble = scheme.onSecondaryContainer,
        input = scheme.surfaceContainer,
        onInput = scheme.onSurface,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        background = scheme.background,
        onBackground = scheme.onBackground,
        outline = scheme.outline,
    )
}

/**
 * WearAI 主题。
 *
 * @param forceDark 手动强制深色（否则跟随系统）
 * @param themeId 主题 ID；支持 [AppSettings.THEME_DYNAMIC]（动态取色）与 [AppSettings.THEME_CUSTOM]（自定义主色）
 * @param customSeed 自定义主色的种子颜色（ARGB）
 */
@Composable
fun WearAITheme(
    forceDark: Boolean = false,
    themeId: String = AppSettings.DEFAULT_THEME_ID,
    customSeed: Int = AppSettings.DEFAULT_CUSTOM_SEED,
    content: @Composable () -> Unit,
) {
    val dark = forceDark || isSystemInDarkTheme()
    val context = LocalContext.current

    val trio = when {
        themeId == AppSettings.THEME_DYNAMIC && dynamicColorSupported -> dynamicTrio(context, dark)

        themeId == AppSettings.THEME_CUSTOM -> {
            val hue = Color(customSeed).hueAndSaturation().first
            ImpressionistPalette("custom", "自定义", hue, 22f).resolve(dark)
        }

        else -> {
            val palette = IMPRESSIONIST_PALETTES.firstOrNull { it.id == themeId }
                ?: IMPRESSIONIST_PALETTES.first()
            palette.resolve(dark)
        }
    }

    CompositionLocalProvider(
        LocalSurfaceTrio provides trio,
        LocalSmallRadius provides AppSmallRadius,
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFromTrio(trio, dark),
            typography = Typography,
            shapes = appShapes(AppSmallRadius),
            content = content,
        )
    }
}
