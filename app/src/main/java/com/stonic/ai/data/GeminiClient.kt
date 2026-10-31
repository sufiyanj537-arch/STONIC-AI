package com.stonic.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient {

    suspend fun reply(
        apiKey: String,
        prompt: String,
        history: List<ChatMessage>,
        assistantName: String,
        language: String,
        personality: String
    ): String = withContext(Dispatchers.IO) {

        require(apiKey.isNotBlank()) {
            "Settings mein Gemini API key add karo."
        }

        val conversation = StringBuilder()

        history.takeLast(12).forEach { msg ->
            if (msg.role == "user" || msg.role == "model") {
                val speaker = if (msg.role == "user") "Boss" else assistantName
                conversation.append("$speaker: ${msg.text}\n")
            }
        }

        conversation.append("Boss: $prompt")

        val system = """
            You are $assistantName, a friendly female AI assistant.
            Address the user as Boss naturally.
            Personality: $personality.
            Reply in $language unless the user requests another language.
            Be useful, honest and concise.
            Never claim to perform phone actions you cannot perform.
        """.trimIndent()

        val body = JSONObject()
            .put("model", "gemini-3.8-flash")
            .put("input", conversation.toString())
            .put("system_instruction", system)
            .put("store", false)

        val connection = (URL(
            "https://generativelanguage.googleapis.com/v1beta/interactions"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
            doOutput = true
        }

        try {
            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream.bufferedReader().use { it.readText() }

            if (code !in 200..299) {
                val detail = try {
                    JSONObject(response)
                        .getJSONObject("error")
                        .optString("message", "API request failed ($code)")
                } catch (_: Exception) {
                    "API request failed ($code): $response"
                }

                throw IllegalStateException(detail)
            }

            val json = JSONObject(response)
            val steps = json.optJSONArray("steps")
                ?: throw IllegalStateException("Gemini response mein steps nahi mile.")

            val answer = StringBuilder()

            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)

                if (step.optString("type") == "model_output") {
                    val content = step.optJSONArray("content") ?: continue

                    for (j in 0 until content.length()) {
                        val item = content.getJSONObject(j)

                        if (item.optString("type") == "text") {
                            answer.append(item.optString("text"))
                        }
                    }
                }
            }

            answer.toString().trim().ifBlank {
                throw IllegalStateException("Gemini ne empty response diya.")
            }

        } finally {
            connection.disconnect()
        }
    }
}
