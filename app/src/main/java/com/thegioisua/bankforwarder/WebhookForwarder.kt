package com.thegioisua.bankforwarder

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookForwarder {
    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun forward(webhookUrl: String, normalizedMessage: String, hmacSecret: String = "") {
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

        val bodyStr = payload.toString()
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder()
            .url(webhookUrl)
            .post(body)

        if (hmacSecret.isNotBlank()) {
            val timestamp = (now / 1000L).toString()
            val nonce = UUID.randomUUID().toString()
            val bodyHash = sha256Hex(bodyStr)
            val canonical = "$timestamp\n$nonce\n$bodyHash"
            val signature = hmacSha256Hex(hmacSecret, canonical)
            requestBuilder
                .header("X-TGS-Timestamp", timestamp)
                .header("X-TGS-Nonce", nonce)
                .header("X-TGS-Signature", signature)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Webhook forward failed: HTTP ${response.code}")
            }
        }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
