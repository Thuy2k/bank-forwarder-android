package com.thegioisua.bankforwarder

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object WebhookForwarder {
    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun forward(webhookUrl: String, normalizedMessage: String) {
        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("update_id", now)
            put("message", JSONObject().apply {
                put("message_id", now)
                put("date", (now / 1000L).toInt())
                put("chat", JSONObject().apply {
                    put("id", 1)
                    put("type", "private")
                })
                put("text", normalizedMessage)
            })
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Webhook forward failed: HTTP ${response.code}")
            }
        }
    }
}
