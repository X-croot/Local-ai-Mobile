package com.localai.mobile.core

import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * API-key-free web search via the DuckDuckGo HTML endpoint (same approach as
 * the original LocalAI). Results are fed to the model as context.
 */
object WebSearch {
    data class Result(val title: String, val url: String, val snippet: String)

    fun search(query: String, limit: Int = 5): List<Result> {
        val q = URLEncoder.encode(query, "UTF-8")
        val doc = Jsoup.connect("https://html.duckduckgo.com/html/?q=$q")
            .userAgent("Mozilla/5.0 (Android) LocalAI")
            .timeout(15000)
            .get()
        val out = ArrayList<Result>()
        for (el in doc.select("div.result")) {
            val a = el.selectFirst("a.result__a") ?: continue
            val title = a.text()
            val url = a.attr("href")
            val snippet = el.selectFirst(".result__snippet")?.text() ?: ""
            if (title.isNotBlank()) out.add(Result(title, url, snippet))
            if (out.size >= limit) break
        }
        return out
    }

    fun asContext(results: List<Result>): String =
        results.joinToString("\n") { "- ${it.title}: ${it.snippet} (${it.url})" }
}
