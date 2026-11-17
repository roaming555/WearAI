package com.foggland.wearai.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 聊天图片的本地存储与压缩。
 *
 * 多模态请求会把图片以 base64 塞进 JSON，体积过大会导致请求失败或消耗大量 token，
 * 因此在导入阶段就做两件事：**限制原始文件大小** + **限制像素尺寸并重新压缩成 JPEG**。
 */
object ImageStore {

    /** 原图大小上限：4MB（超过直接拒绝） */
    const val MAX_BYTES = 4L * 1024 * 1024

    /** 压缩后最长边像素上限，避免 base64 后体积爆炸 */
    private const val MAX_DIMENSION = 1024

    /** JPEG 压缩质量（0~100） */
    private const val JPEG_QUALITY = 85

    sealed class ImportResult {
        data class Success(val path: String, val bytes: Long) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    /** 把用户选中的图片导入私有目录，返回保存后的绝对路径。 */
    fun import(context: Context, uri: Uri): ImportResult {
        return try {
            val realSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (realSize > MAX_BYTES) {
                return ImportResult.Failure("图片超过 4MB（${realSize / 1024 / 1024}MB）")
            }

            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return ImportResult.Failure("无法读取这张图片")

            val longest = maxOf(original.width, original.height)
            val scale = if (longest > MAX_DIMENSION) MAX_DIMENSION.toFloat() / longest else 1f
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                original
            }

            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            if (scaled !== original) scaled.recycle()
            original.recycle()

            if (file.length() == 0L) {
                file.delete()
                ImportResult.Failure("图片保存失败")
            } else {
                ImportResult.Success(file.absolutePath, file.length())
            }
        } catch (t: Throwable) {
            ImportResult.Failure(t.message ?: "导入图片失败")
        }
    }

    /** 解码缩略图用于气泡内展示（按需降采样）。 */
    fun loadThumbnail(path: String, targetPx: Int = 512): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > targetPx || bounds.outHeight / sample > targetPx) {
                sample *= 2
            }
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (t: Throwable) {
            null
        }
    }
}
