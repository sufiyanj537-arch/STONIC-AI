package com.stonic.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient {
    suspend fun reply(apiKey: String, prompt: String, history: List<ChatMessage>, assistantName: String, language: String, personality: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Settings mein Gemini API key add karo." }
        val contents = JSONArray()
        history.takeLast(12).forEach { msg ->
            if (msg.role == "user" || msg.role == "model") {
                contents.put(JSONObject().put("role", msg.role).put("parts", JSONArray().put(JSONObject().put("text", msg.text))))
            }
        }
        contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        val system = "You are $assistantName, a friendly female AI assistant. Address the user as Boss naturally. Personality: $personality. Reply in $language unless the user requests another language. Be useful, honest and concise. Never claim to perform phone actions you cannot perform."
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.75).put("maxOutputTokens", 1200))
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 45000
            setRequestProperty("Content-Type", "application/json; charset=utf-8"); setRequestProperty("x-goog-api-key", apiKey); doOutput = true
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val detail = try { JSONObject(response).getJSONObject("error").optString("message", "API request failed ($code)") } catch (_: Exception) { "API request failed ($code)" }
                throw IllegalStateException(detail)
            }
            val json = JSONObject(response)
            json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
        } finally { connection.disconnect() }
    }
}
