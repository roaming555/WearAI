package com.foggland.wearai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.ui.theme.UiScaleAuto

/**
 * 当前应用的有效界面缩放（已考虑「自动适配」）。
 *
 * 页面内的缩放由 MainActivity 提供；弹窗是独立窗口，需要自己算一次，
 * 不能直接用 settings.uiScale（自动模式下它不是生效值）。
 */
@Composable
fun effectiveUiScale(settings: AppSettings): Float {
    val configuration = LocalConfiguration.current
    return if (settings.uiScaleAuto) {
        UiScaleAuto.suggest(configuration)
    } else {
        settings.uiScale
    }
}

/**
 * 在弹窗内部重新提供按全局缩放换算过的 [LocalDensity]。
 *
 * Compose 的 `Dialog` 使用独立窗口与 Composition 上下文，
 * 页面级的 `LocalDensity` 覆盖**不会**传进去，因此每个弹窗内部都要重新提供一次。
 *
 * 用法：`Dialog(...) { ScaledDialogContent(settings) { /* 弹窗内容 */ } }`
 */
@Composable
fun ScaledDialogContent(settings: AppSettings, content: @Composable () -> Unit) {
    ScaledDialogContent(scale = effectiveUiScale(settings), content = content)
}

/** 按指定缩放值重新提供 LocalDensity。 */
@Composable
fun ScaledDialogContent(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale),
        content = content,
    )
}
