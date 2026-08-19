package com.foggland.wearai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = PaperSecondary,
    onSecondary = PaperOnSecondary,
    secondaryContainer = PaperSecondaryContainer,
    onSecondaryContainer = PaperOnSecondaryContainer,
    tertiary = PaperTertiary,
    onTertiary = PaperOnTertiary,
    tertiaryContainer = PaperTertiaryContainer,
    onTertiaryContainer = PaperOnTertiaryContainer,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperInkVariant,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

/**
 * WearAI 主题：
 *  - 支持莫奈（Monet）动态取色（Android 12+）
 *  - 主色固定为品牌浅蓝 #2C5197（聊天框与设置高亮）
 *  - 支持手动深色（forceDark）或跟随系统自动切换
 */
@Composable
fun WearAITheme(
    forceDark: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = forceDark || isSystemInDarkTheme()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            base.copy(
                primary = if (darkTheme) BrandPrimaryDark else BrandPrimary,
                onPrimary = if (darkTheme) BrandOnPrimaryDark else BrandOnPrimary,
                primaryContainer = if (darkTheme) BrandPrimaryContainerDark else BrandPrimaryContainer,
                onPrimaryContainer = if (darkTheme) BrandOnPrimaryContainerDark else BrandOnPrimaryContainer,
            )
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
