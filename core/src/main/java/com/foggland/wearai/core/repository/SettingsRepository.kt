package com.foggland.wearai.core.repository

import android.content.Context
import android.content.SharedPreferences
import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.core.model.ModelConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * 设置与自定义模型管理的持久化仓库（基于 SharedPreferences）。
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("wearai_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _models = MutableStateFlow(loadModels())
    val models: StateFlow<List<ModelConfig>> = _models

    // ------------------------------------------------------------------
    // 设置
    // ------------------------------------------------------------------

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        persistSettings(next)
    }

    private fun loadSettings(): AppSettings {
        val storedPrompt = prefs.getString(KEY_SYSTEM_PROMPT, AppSettings.DEFAULT_SYSTEM_PROMPT)
            ?: AppSettings.DEFAULT_SYSTEM_PROMPT
        // 历史版本的默认提示词会让模型输出整篇 LaTeX 文档，这里自动升级为新默认值；
        // 用户自定义过的提示词保持不动。
        val systemPrompt = if (storedPrompt in AppSettings.LEGACY_DEFAULT_PROMPTS) {
            AppSettings.DEFAULT_SYSTEM_PROMPT
        } else {
            storedPrompt
        }
        return AppSettings(
            apiKey = prefs.getString(KEY_API_KEY, AppSettings.DEFAULT_API_KEY) ?: AppSettings.DEFAULT_API_KEY,
            endpointUrl = prefs.getString(KEY_ENDPOINT, AppSettings.DEFAULT_ENDPOINT) ?: AppSettings.DEFAULT_ENDPOINT,
            systemPrompt = systemPrompt,
            activeModelId = prefs.getString(KEY_ACTIVE_MODEL, AppSettings.DEFAULT_MODEL_CALLING_NAME)
                ?: AppSettings.DEFAULT_MODEL_CALLING_NAME,
            titleModelId = prefs.getString(KEY_TITLE_MODEL, AppSettings.DEFAULT_TITLE_MODEL_CALLING_NAME)
                ?: AppSettings.DEFAULT_TITLE_MODEL_CALLING_NAME,
            deepThinking = prefs.getBoolean(KEY_DEEP_THINKING, true),
            streaming = prefs.getBoolean(KEY_STREAMING, true),
            darkMode = prefs.getBoolean(KEY_DARK_MODE, false),
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.95f).toDouble(),
            topP = prefs.getFloat(KEY_TOP_P, 0.7f).toDouble(),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4096),
            uiScale = prefs.getFloat(KEY_UI_SCALE, AppSettings.DEFAULT_UI_SCALE),
            themeId = prefs.getString(KEY_THEME_ID, AppSettings.DEFAULT_THEME_ID)
                ?: AppSettings.DEFAULT_THEME_ID,
            customSeed = prefs.getInt(KEY_CUSTOM_SEED, AppSettings.DEFAULT_CUSTOM_SEED),
            backgroundPath = prefs.getString(KEY_BACKGROUND_PATH, null),
            backgroundDim = prefs.getFloat(KEY_BACKGROUND_DIM, AppSettings.DEFAULT_BACKGROUND_DIM),
            floatingBars = prefs.getBoolean(KEY_FLOATING_BARS, true),
            capsuleRadius = prefs.getFloat(KEY_CAPSULE_RADIUS, AppSettings.DEFAULT_CAPSULE_RADIUS)
                .coerceIn(AppSettings.MIN_CAPSULE_RADIUS, AppSettings.MAX_CAPSULE_RADIUS),
            capsuleBorder = prefs.getBoolean(KEY_CAPSULE_BORDER, false),
            glassEnabled = prefs.getBoolean(KEY_GLASS_ENABLED, false),
            glassAlpha = prefs.getFloat(KEY_GLASS_ALPHA, AppSettings.DEFAULT_GLASS_ALPHA),
            glassBlurRadius = prefs.getFloat(KEY_GLASS_BLUR, AppSettings.DEFAULT_GLASS_BLUR),
            translucentBars = prefs.getBoolean(KEY_TRANSLUCENT_BARS, false),
            translucentAlpha = prefs.getFloat(KEY_TRANSLUCENT_ALPHA, AppSettings.DEFAULT_GLASS_ALPHA),
        )
    }

    private fun persistSettings(s: AppSettings) {
        prefs.edit()
            .putString(KEY_API_KEY, s.apiKey)
            .putString(KEY_ENDPOINT, s.endpointUrl)
            .putString(KEY_SYSTEM_PROMPT, s.systemPrompt)
            .putString(KEY_ACTIVE_MODEL, s.activeModelId)
            .putString(KEY_TITLE_MODEL, s.titleModelId)
            .putBoolean(KEY_DEEP_THINKING, s.deepThinking)
            .putBoolean(KEY_STREAMING, s.streaming)
            .putBoolean(KEY_DARK_MODE, s.darkMode)
            .putFloat(KEY_TEMPERATURE, s.temperature.toFloat())
            .putFloat(KEY_TOP_P, s.topP.toFloat())
            .putInt(KEY_MAX_TOKENS, s.maxTokens)
            .putFloat(KEY_UI_SCALE, s.uiScale)
            .putBoolean(KEY_UI_SCALE_AUTO, s.uiScaleAuto)
            .putString(KEY_THEME_ID, s.themeId)
            .putInt(KEY_CUSTOM_SEED, s.customSeed)
            .putString(KEY_BACKGROUND_PATH, s.backgroundPath)
            .putFloat(KEY_BACKGROUND_DIM, s.backgroundDim)
            .putBoolean(KEY_FLOATING_BARS, s.floatingBars)
            .putFloat(KEY_CAPSULE_RADIUS, s.capsuleRadius)
            .putBoolean(KEY_CAPSULE_BORDER, s.capsuleBorder)
            .putBoolean(KEY_GLASS_ENABLED, s.glassEnabled)
            .putFloat(KEY_GLASS_ALPHA, s.glassAlpha)
            .putFloat(KEY_GLASS_BLUR, s.glassBlurRadius)
            .putBoolean(KEY_TRANSLUCENT_BARS, s.translucentBars)
            .putFloat(KEY_TRANSLUCENT_ALPHA, s.translucentAlpha)
            .apply()
    }

    // ------------------------------------------------------------------
    // 自定义模型管理
    // ------------------------------------------------------------------

    fun addModel(name: String, callingName: String, apiKey: String, endpointUrl: String, multimodal: Boolean = false) {
        val cleanName = name.trim()
        val cleanCalling = callingName.trim()
        if (cleanName.isEmpty() || cleanCalling.isEmpty()) return
        val next = _models.value + ModelConfig(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            callingName = cleanCalling,
            apiKey = apiKey.trim(),
            endpointUrl = endpointUrl.trim(),
            multimodal = multimodal,
        )
        _models.value = next
        persistModels(next)
    }

    fun updateModel(id: String, name: String, callingName: String, apiKey: String, endpointUrl: String, multimodal: Boolean = false) {
        val cleanName = name.trim()
        val cleanCalling = callingName.trim()
        if (cleanName.isEmpty() || cleanCalling.isEmpty()) return
        val next = _models.value.map {
            if (it.id == id) {
                it.copy(
                    name = cleanName,
                    callingName = cleanCalling,
                    apiKey = apiKey.trim(),
                    endpointUrl = endpointUrl.trim(),
                    multimodal = multimodal,
                )
            } else {
                it
            }
        }
        _models.value = next
        persistModels(next)
    }

    fun deleteModel(id: String) {
        val next = _models.value.filterNot { it.id == id }
        _models.value = next
        persistModels(next)
        // 若删除的是当前模型或标题模型，则切回列表第一个
        val fallback = next.firstOrNull()?.id ?: AppSettings.DEFAULT_MODEL_CALLING_NAME
        update { s ->
            s.copy(
                activeModelId = if (s.activeModelId == id) fallback else s.activeModelId,
                titleModelId = if (s.titleModelId == id) fallback else s.titleModelId,
            )
        }
    }

    private fun loadModels(): List<ModelConfig> {
        val json = prefs.getString(KEY_MODELS, null) ?: return DEFAULT_MODELS
        val type = object : TypeToken<List<ModelConfig>>() {}.type
        return try {
            val list: List<ModelConfig> = gson.fromJson(json, type) ?: emptyList()
            list.map { it.copy(apiKey = it.apiKey.orEmpty(), endpointUrl = it.endpointUrl.orEmpty()) }
                .ifEmpty { DEFAULT_MODELS }
        } catch (e: Exception) {
            DEFAULT_MODELS
        }
    }

    private fun persistModels(models: List<ModelConfig>) {
        prefs.edit().putString(KEY_MODELS, gson.toJson(models)).apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_ENDPOINT = "endpoint_url"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_ACTIVE_MODEL = "active_model_id"
        private const val KEY_TITLE_MODEL = "title_model_id"
        private const val KEY_DEEP_THINKING = "deep_thinking"
        private const val KEY_STREAMING = "streaming"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_UI_SCALE = "ui_scale"
    private const val KEY_UI_SCALE_AUTO = "ui_scale_auto"
    private const val KEY_THEME_ID = "theme_id"
    private const val KEY_CUSTOM_SEED = "theme_custom_seed"
    private const val KEY_BACKGROUND_PATH = "theme_background_path"
    private const val KEY_BACKGROUND_DIM = "theme_background_dim"
    private const val KEY_FLOATING_BARS = "floating_bars"
    private const val KEY_CAPSULE_RADIUS = "capsule_radius"
    private const val KEY_CAPSULE_BORDER = "capsule_border"
    private const val KEY_GLASS_ENABLED = "glass_enabled"
    private const val KEY_GLASS_ALPHA = "glass_alpha"
    private const val KEY_GLASS_BLUR = "glass_blur_radius"
    private const val KEY_TRANSLUCENT_BARS = "translucent_bars"
    private const val KEY_TRANSLUCENT_ALPHA = "translucent_alpha"
        private const val KEY_MODELS = "models"

        /** 预置模型列表。默认模型为 glm-4.7-flash。 */
        val DEFAULT_MODELS: List<ModelConfig> = listOf(
            ModelConfig(
                id = "glm-4.7-flash",
                name = "GLM-4.7-Flash",
                callingName = "glm-4.7-flash",
                apiKey = AppSettings.DEFAULT_API_KEY,
                endpointUrl = AppSettings.DEFAULT_ENDPOINT,
            ),
            ModelConfig(
                id = "glm-4.6v-flash",
                name = "GLM-4.6V-Flash",
                callingName = "glm-4.6v-flash",
                apiKey = AppSettings.DEFAULT_API_KEY,
                endpointUrl = AppSettings.DEFAULT_ENDPOINT,
            ),
            ModelConfig(
                id = "glm-4.5-flash",
                name = "GLM-4.5-Flash",
                callingName = "glm-4.5-flash",
                apiKey = AppSettings.DEFAULT_API_KEY,
                endpointUrl = AppSettings.DEFAULT_ENDPOINT,
            ),
            ModelConfig(
                id = "glm-4-flash",
                name = "GLM-4-Flash",
                callingName = "glm-4-flash",
                apiKey = AppSettings.DEFAULT_API_KEY,
                endpointUrl = AppSettings.DEFAULT_ENDPOINT,
            ),
        )
    }
}
