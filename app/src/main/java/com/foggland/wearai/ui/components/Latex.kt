package com.foggland.wearai.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * 使用 JLaTeXMath 将 LaTeX 公式渲染为位图；解析失败返回 null。
 *
 * @param textSizePx 公式字号（像素）
 */
fun renderLatex(latex: String, textSizePx: Int, color: Color): ImageBitmap? {
    return try {
        val drawable = JLatexMathDrawable.builder(latex)
            .textSize(textSizePx.toFloat())
            .color(color.toArgb())
            .build()
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
