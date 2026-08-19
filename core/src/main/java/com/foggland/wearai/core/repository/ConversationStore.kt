package com.foggland.wearai.core.repository

import android.content.Context
import android.content.SharedPreferences
import com.foggland.wearai.core.model.Conversation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 本地对话历史持久化：以列表形式保存多个会话，长期保留 AI 对话记忆。
 */
class ConversationStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("wearai_conversations", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<Conversation>>() {}.type

    fun loadAll(): List<Conversation> {
        val json = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val list: List<Conversation> = gson.fromJson(json, listType) ?: emptyList()
            list.map { it.copy(messages = it.messages.filter { m -> !m.isStreaming }) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(conversations: List<Conversation>) {
        // 过滤掉仍处于流式生成的中间态消息，只落盘已完成内容
        val cleaned = conversations.map { conv ->
            conv.copy(messages = conv.messages.filter { !it.isStreaming })
        }
        prefs.edit().putString(KEY_CONVERSATIONS, gson.toJson(cleaned)).apply()
    }

    fun getCurrentId(): String? = prefs.getString(KEY_CURRENT_ID, null)

    fun setCurrentId(id: String?) {
        if (id == null) {
            prefs.edit().remove(KEY_CURRENT_ID).apply()
        } else {
            prefs.edit().putString(KEY_CURRENT_ID, id).apply()
        }
    }

    companion object {
        private const val KEY_CONVERSATIONS = "conversations"
        private const val KEY_CURRENT_ID = "current_id"
    }
}
