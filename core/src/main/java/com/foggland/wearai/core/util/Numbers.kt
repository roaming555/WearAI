package com.foggland.wearai.core.util

import kotlin.math.pow
import kotlin.math.round

/**
 * 将浮点数四舍五入到指定小数位，返回“干净”的双精度值。
 *
 * 智谱接口对 temperature / top_p 等参数限制最多 2 位小数（错误码 1210）。
 * 直接发送 Float→Double 的原始值（如 0.949999988079071）会触发 400 错误，
 * 因此发送前需统一取整到 2 位小数。
 */
fun Double.roundTo(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return round(this * factor) / factor
}

fun Float.roundTo(decimals: Int): Float = toDouble().roundTo(decimals).toFloat()
