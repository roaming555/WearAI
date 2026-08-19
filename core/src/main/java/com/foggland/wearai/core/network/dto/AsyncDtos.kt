package com.foggland.wearai.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * 图像生成（异步）请求体。
 * 对应 POST /api/paas/v4/images/generations
 * 模型示例：cogview-4、cogview-3-flash、cogview-3-plus。
 */
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    val size: String? = null,
    val n: Int? = null,
)

/**
 * 视频生成（异步）请求体。
 * 对应 POST /api/paas/v4/videos/generations
 * 模型示例：cogvideox-2、cogvideox-flash、cogvideox-2-turbo。
 */
data class VideoGenerationRequest(
    val model: String,
    val prompt: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    val size: String? = null,
    val duration: Int? = null,
    @SerializedName("with_audio") val withAudio: Boolean? = null,
)

/**
 * 异步任务响应（提交任务与查询异步结果共用结构）。
 * task_status：PROCESSING / SUCCESS / FAIL。
 */
data class AsyncTaskResponse(
    val id: String? = null,
    val model: String? = null,
    @SerializedName("task_status") val taskStatus: String? = null,
    val error: ErrorDto? = null,
    // 图像结果
    val data: List<AsyncImageItem>? = null,
    // 视频结果
    @SerializedName("video_result") val videoResult: List<AsyncVideoItem>? = null,
)

data class AsyncImageItem(
    val url: String? = null,
)

data class AsyncVideoItem(
    val url: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
)
