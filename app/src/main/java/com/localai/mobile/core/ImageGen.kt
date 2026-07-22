package com.localai.mobile.core

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Image generation. Uses a user-configurable OpenAI-compatible image endpoint.
 * Defaults to OpenRouter's image route; the user can point it at any provider
 * (Stability, Together, local ComfyUI OpenAI-bridge, etc.) from Settings.
 *
 * Returns a base64 PNG so it can be stored inside the encrypted vault and never
 * touch external storage (privacy-first, no traces).
 */
class ImageGen(
    private val endpoint: String,
    private val apiKey: String,
    private val model: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
    private val json = "application/json".toMediaType()

    /** Returns base64 (no data-uri prefix). Throws on failure. */
    fun generate(prompt: String): String {
        val payload = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("n", 1)
            .put("size", "1024x1024")
            .put("response_format", "b64_json")
        val req = Request.Builder().url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: throw Exception("Empty image response")
            if (!resp.isSuccessful) throw Exception("Image API ${resp.code}: $body")
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            if (data.length() == 0) throw Exception("No image returned")
            val first = data.getJSONObject(0)
            return when {
                first.has("b64_json") -> first.getString("b64_json")
                first.has("url") -> downloadAsB64(first.getString("url"))
                else -> throw Exception("Unexpected image payload")
            }
        }
    }

    private fun downloadAsB64(url: String): String {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            val bytes = r.body?.bytes() ?: throw Exception("Image download failed")
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }
}
