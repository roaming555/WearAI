package com.foggland.wearai.ui.theme

import android.content.res.Configuration
import com.foggland.wearai.core.model.AppSettings

/**
 * 根据屏幕尺寸自动推荐全局界面缩放。
 *
 * 思路：Android 的 `dp` 已经做了物理尺寸归一化，所以**不需要**按 dpi 缩放
 * （那会把高密度屏放大两遍）。真正需要补偿的是「设备宽度的差异」：
 * 窄屏（360dp 左右的小手机）内容会显得挤，宽屏（大屏手机 / 折叠屏）
 * 又有富余，因此以 [REFERENCE_WIDTH_DP] 为基准做温和的等比补偿，
 * 并把结果限制在一个保守区间内，避免出现极端值。
 */
object UiScaleAuto {

    /** 设计基准宽度：主流手机的内容宽度中位数 */
    private const val REFERENCE_WIDTH_DP = 392f

    /**
     * 允许下探到 0.60：手表类设备（如 320×360 @180dpi，
     * smallestScreenWidthDp 约 284dp）需要 ~0.70 的比例才不会显得拥挤。
     */
    private const val MIN_AUTO = 0.60f

    private const val MAX_AUTO = 1.15f

    /** 计算推荐缩放（不含用户手动覆盖）。 */
    fun suggest(configuration: Configuration): Float {
        // 用 smallestScreenWidthDp：与方向无关，且天然区分手表 / 手机 / 平板
        val basis = configuration.smallestScreenWidthDp
        if (basis <= 0) return 1f
        val raw = basis / REFERENCE_WIDTH_DP
        return raw.coerceIn(
            maxOf(MIN_AUTO, AppSettings.MIN_UI_SCALE),
            minOf(MAX_AUTO, AppSettings.MAX_UI_SCALE),
        )
    }

    /** 把自动推荐值四舍五入到 0.01，便于展示与持久化。 */
    fun suggestRounded(configuration: Configuration): Float {
        return (suggest(configuration) * 100f).toInt() / 100f
    }
}
