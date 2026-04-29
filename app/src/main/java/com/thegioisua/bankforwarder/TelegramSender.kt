package com.thegioisua.bankforwarder

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object TelegramSender {
    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun sendMessage(botToken: String, chatId: String, text: String) {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Telegram send failed: HTTP ${response.code}")
            }
        }
    }
}
