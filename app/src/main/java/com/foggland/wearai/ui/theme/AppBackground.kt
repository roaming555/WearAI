package com.foggland.wearai.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

/** 背景图对应的「预模糊」版本，供有色毛玻璃当作背景层使用。 */
val LocalBlurredBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

/**
 * 应用根背景。
 *
 * 没有设置背景图时显示主题背景色；设置了背景图时铺满屏幕并叠加可调的暗色蒙层，
 * 保证前景文字在任意图片上都可读。
 */
@Composable
fun AppBackground(
    backgroundPath: String?,
    dim: Float,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val image = remember(backgroundPath) { loadBackgroundBitmap(backgroundPath) }

    CompositionLocalProvider(
        LocalBlurredBackdrop provides null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // 蒙层：深色模式下偏黑，浅色模式下偏主题背景色，避免纯黑压暗画面
                val scrim = if (colorScheme.background.luminanceIsDark()) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrim.copy(alpha = dim.coerceIn(0f, 0.9f))),
                )
            }
            content()
        }
    }
}

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** 按需降采样解码背景图，避免大图占用过多内存。 */
private fun loadBackgroundBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    return try {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val maxDimension = 2048
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, options)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/** 把用户选择的图片复制到应用私有目录，返回保存后的路径；失败返回 null。 */
fun importBackgroundImage(filesDir: File, source: java.io.InputStream): String? {
    return try {
        val dir = File(filesDir, "theme").apply { mkdirs() }
        val target = File(dir, "background_${System.currentTimeMillis()}.img")
        target.outputStream().use { output -> source.use { it.copyTo(output) } }
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        // 只保留最新一张背景图（含其历史模糊版本）
        dir.listFiles()?.filter { it.absolutePath != target.absolutePath }?.forEach { it.delete() }
        target.absolutePath
    } catch (e: Exception) {
        null
    }
}
