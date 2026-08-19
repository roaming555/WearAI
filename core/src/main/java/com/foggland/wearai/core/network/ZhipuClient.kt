package com.foggland.wearai.core.network

import com.foggland.wearai.core.model.Usage
import com.foggland.wearai.core.network.dto.AsyncTaskResponse
import com.foggland.wearai.core.network.dto.ChatCompletionRequest
import com.foggland.wearai.core.network.dto.ChatCompletionResponse
import com.foggland.wearai.core.network.dto.ImageGenerationRequest
import com.foggland.wearai.core.network.dto.UsageDto
import com.foggland.wearai.core.network.dto.VideoGenerationRequest
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 智谱 AI 开放平台 HTTP 客户端。
 *
 * 覆盖四类调用方式：
 *  1. 对话补全（流式 / 非流式）：POST {endpoint}/chat/completions
 *  2. 图像生成（异步）：POST {root}/images/generations
 *  3. 视频生成（异步）：POST {root}/videos/generations
 *  4. 查询异步结果：GET {root}/async-result/{id}
 *
 * 鉴权统一使用 Authorization: Bearer {apiKey}。
 */
class ZhipuClient {

    private val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ---------------------------------------------------------------------
    // 对话补全
    // ---------------------------------------------------------------------

    /**
     * 发送对话补全请求。
     *
     * @param onDelta 流式增量回调（正文增量、深度思考增量）。非流式时会回调一次完整内容。
     * @return 汇总结果（finishReason + usage）。
     * @throws IOException 网络或服务端错误。
     */
    @Throws(IOException::class)
    fun chat(
        request: ChatCompletionRequest,
        apiKey: String,
        endpointUrl: String,
        onDelta: (content: String, reasoning: String) -> Unit,
    ): StreamSummary {
        val body = gson.toJson(request)
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.body?.string() ?: response.message}")
            }
            return if (request.stream) {
                parseSse(response, onDelta)
            } else {
                parseJson(response, onDelta)
            }
        }
    }

    /** 非流式：解析完整 JSON，一次性回调内容。 */
    private fun parseJson(response: Response, onDelta: (String, String) -> Unit): StreamSummary {
        val text = response.body?.string().orEmpty()
        val parsed = try {
            gson.fromJson(text, ChatCompletionResponse::class.java)
        } catch (e: JsonSyntaxException) {
            throw IOException("解析响应失败：${e.message}")
        }
        parsed.error?.let { throw IOException("${it.code}：${it.message}") }
        val message = parsed.choices.firstOrNull()?.message
        val content = message?.content.orEmpty()
        val reasoning = message?.reasoningContent.orEmpty()
        if (content.isNotEmpty() || reasoning.isNotEmpty()) {
            onDelta(content, reasoning)
        }
        return StreamSummary(
            finishReason = parsed.choices.firstOrNull()?.finishReason,
            usage = parsed.usage?.toDomain(),
        )
    }

    /** 流式：逐行读取 SSE（`data: {...}` / `data: [DONE]`），解析增量。 */
    private fun parseSse(response: Response, onDelta: (String, String) -> Unit): StreamSummary {
        val source = response.body?.source()
            ?: throw IOException("响应体为空")
        var finishReason: String? = null
        var usage: Usage? = null
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.isBlank()) continue
            if (!line.startsWith("data:")) continue
            val payload = line.removePrefix("data:").trim()
            if (payload == "[DONE]") break
            val parsed = try {
                gson.fromJson(payload, ChatCompletionResponse::class.java)
            } catch (e: JsonSyntaxException) {
                continue // 忽略无法解析的行
            }
            parsed.error?.let { throw IOException("${it.code}：${it.message}") }
            val choice = parsed.choices.firstOrNull() ?: continue
            val delta = choice.delta
            if (delta != null) {
                val c = delta.content.orEmpty()
                val r = delta.reasoningContent.orEmpty()
                if (c.isNotEmpty() || r.isNotEmpty()) {
                    onDelta(c, r)
                }
            }
            choice.finishReason?.let { finishReason = it }
            parsed.usage?.let { usage = it.toDomain() }
        }
        return StreamSummary(finishReason, usage)
    }

    // ---------------------------------------------------------------------
    // 图像 / 视频生成（异步）与异步结果查询
    // ---------------------------------------------------------------------

    @Throws(IOException::class)
    fun createImage(request: ImageGenerationRequest, apiKey: String, apiRoot: String): AsyncTaskResponse {
        return postJson("$apiRoot/images/generations", apiKey, request)
    }

    @Throws(IOException::class)
    fun createVideo(request: VideoGenerationRequest, apiKey: String, apiRoot: String): AsyncTaskResponse {
        return postJson("$apiRoot/videos/generations", apiKey, request)
    }

    @Throws(IOException::class)
    fun queryAsyncResult(taskId: String, apiKey: String, apiRoot: String): AsyncTaskResponse {
        val httpRequest = Request.Builder()
            .url("$apiRoot/async-result/$taskId")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.body?.string() ?: response.message}")
            }
            val text = response.body?.string().orEmpty()
            val parsed = gson.fromJson(text, AsyncTaskResponse::class.java)
            parsed.error?.let { throw IOException("${it.code}：${it.message}") }
            return parsed
        }
    }

    private fun postJson(url: String, apiKey: String, payload: Any): AsyncTaskResponse {
        val body = gson.toJson(payload)
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.body?.string() ?: response.message}")
            }
            val text = response.body?.string().orEmpty()
            val parsed = gson.fromJson(text, AsyncTaskResponse::class.java)
            parsed.error?.let { throw IOException("${it.code}：${it.message}") }
            return parsed
        }
    }

    private fun UsageDto.toDomain() = Usage(
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
    )
}
