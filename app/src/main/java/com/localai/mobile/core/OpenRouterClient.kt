package com.localai.mobile.core

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouter client. The user supplies their own free API key. Handles model
 * listing (free-first, uncensored detection) and chat completion. Surfaces a
 * clear "key expired / invalid" signal so the UI can prompt the user to update
 * the key instead of silently failing.
 */
class OpenRouterClient(private val tokenProvider: () -> String?) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val base = "https://openrouter.ai/api/v1"
    private val json = "application/json".toMediaType()

    class KeyError(val kind: String, msg: String) : Exception(msg)

    data class Model(val id: String, val name: String, val free: Boolean, val uncensored: Boolean)

    fun listModels(): List<Model> {
        val token = tokenProvider() ?: throw KeyError("missing", "No API key set")
        val req = Request.Builder().url("$base/models")
            .header("Authorization", "Bearer $token").build()
        http.newCall(req).execute().use { resp ->
            checkAuth(resp.code)
            val body = resp.body?.string() ?: "{}"
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            val out = ArrayList<Model>()
            for (i in 0 until data.length()) {
                val m = data.getJSONObject(i)
                val id = m.getString("id")
                val name = m.optString("name", id)
                val pricing = m.optJSONObject("pricing")
                val free = id.endsWith(":free") ||
                    (pricing?.optString("prompt", "1") == "0")
                val lower = "$id $name".lowercase()
                val unc = listOf("uncensored", "abliterated", "dolphin", "roleplay", "nsfw")
                    .any { lower.contains(it) }
                out.add(Model(id, name, free, unc))
            }
            // free first, then uncensored
            return out.sortedWith(compareByDescending<Model> { it.free }.thenByDescending { it.uncensored })
        }
    }

    /** Non-streaming chat completion. Returns assistant text. */
    fun chat(model: String, messages: List<ChatMessage>): String {
        val token = tokenProvider() ?: throw KeyError("missing", "No API key set")
        val msgArr = JSONArray()
        messages.forEach { m ->
            msgArr.put(JSONObject().put("role", m.role).put("content", m.content))
        }
        val payload = JSONObject()
            .put("model", model)
            .put("messages", msgArr)
        val req = Request.Builder().url("$base/chat/completions")
            .header("Authorization", "Bearer $token")
            .header("HTTP-Referer", "https://localai.app")
            .header("X-Title", "LocalAI Mobile")
            .post(payload.toString().toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            checkAuth(resp.code)
            val body = resp.body?.string() ?: throw Exception("Empty response")
            if (!resp.isSuccessful) throw Exception("OpenRouter ${resp.code}: $body")
            val choices = JSONObject(body).optJSONArray("choices") ?: JSONArray()
            if (choices.length() == 0) throw Exception("No choices returned")
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun checkAuth(code: Int) {
        when (code) {
            401 -> throw KeyError("invalid", "API key is invalid.")
            402 -> throw KeyError("expired", "API key has no remaining credit.")
            429 -> throw KeyError("ratelimit", "Rate limited. Try later or update key.")
        }
    }
}
