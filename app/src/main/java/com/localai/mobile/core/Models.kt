package com.localai.mobile.core

import org.json.JSONArray
import org.json.JSONObject

// ---- Domain models ---------------------------------------------------------

data class ChatMessage(
    val role: String,          // "user" | "assistant" | "system"
    val content: String,
    val imageB64: String? = null,
    val ts: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String,
    val characterId: String,
    var title: String,
    val messages: MutableList<ChatMessage> = mutableListOf()
)

data class Character(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val greeting: String,
    val theme: String,
    val builtIn: Boolean = false,
    // granular permissions (mirrors original LocalAI)
    val allowCommands: Boolean = false,
    val allowWebSearch: Boolean = true,
    val allowImages: Boolean = true,
    val avatarEmoji: String = "🤖"
)

// ---- Serialization helpers -------------------------------------------------

object Serial {
    fun msgToJson(m: ChatMessage) = JSONObject().apply {
        put("role", m.role); put("content", m.content)
        m.imageB64?.let { put("image", it) }
        put("ts", m.ts)
    }

    fun msgFromJson(o: JSONObject) = ChatMessage(
        o.getString("role"), o.getString("content"),
        if (o.has("image")) o.getString("image") else null,
        o.optLong("ts", System.currentTimeMillis())
    )

    fun convToJson(c: Conversation) = JSONObject().apply {
        put("id", c.id); put("characterId", c.characterId); put("title", c.title)
        put("messages", JSONArray().also { arr -> c.messages.forEach { arr.put(msgToJson(it)) } })
    }

    fun convFromJson(o: JSONObject): Conversation {
        val conv = Conversation(o.getString("id"), o.getString("characterId"), o.getString("title"))
        val arr = o.getJSONArray("messages")
        for (i in 0 until arr.length()) conv.messages.add(msgFromJson(arr.getJSONObject(i)))
        return conv
    }

    fun charToJson(c: Character) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("description", c.description)
        put("systemPrompt", c.systemPrompt); put("greeting", c.greeting); put("theme", c.theme)
        put("builtIn", c.builtIn); put("allowCommands", c.allowCommands)
        put("allowWebSearch", c.allowWebSearch); put("allowImages", c.allowImages)
        put("avatarEmoji", c.avatarEmoji)
    }

    fun charFromJson(o: JSONObject) = Character(
        o.getString("id"), o.getString("name"), o.optString("description"),
        o.getString("systemPrompt"), o.optString("greeting"), o.optString("theme", "mono"),
        o.optBoolean("builtIn", false), o.optBoolean("allowCommands", false),
        o.optBoolean("allowWebSearch", true), o.optBoolean("allowImages", true),
        o.optString("avatarEmoji", "🤖")
    )
}
