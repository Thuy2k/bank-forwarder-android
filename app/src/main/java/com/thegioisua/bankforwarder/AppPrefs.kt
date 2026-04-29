package com.thegioisua.bankforwarder

import android.content.Context

data class ShopTarget(
    val prefix: String,
    val botToken: String,
    val chatId: String,
    val webhookUrl: String
)

class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("bank_forwarder", Context.MODE_PRIVATE)

    var shopMappingsRaw: String
        get() = sp.getString("shop_mappings_raw", "")?.trim().orEmpty()
        set(value) = sp.edit().putString("shop_mappings_raw", value.trim()).apply()

    var botToken: String
        get() = sp.getString("bot_token", "")?.trim().orEmpty()
        set(value) = sp.edit().putString("bot_token", value.trim()).apply()

    var chatId: String
        get() = sp.getString("chat_id", "")?.trim().orEmpty()
        set(value) = sp.edit().putString("chat_id", value.trim()).apply()

    var txPrefix: String
        get() = sp.getString("tx_prefix", "TGS")?.trim().orEmpty()
        set(value) = sp.edit().putString("tx_prefix", value.trim()).apply()

    var enabledSms: Boolean
        get() = sp.getBoolean("enabled_sms", true)
        set(value) = sp.edit().putBoolean("enabled_sms", value).apply()

    var enabledNotification: Boolean
        get() = sp.getBoolean("enabled_noti", false)
        set(value) = sp.edit().putBoolean("enabled_noti", value).apply()

    var smsSenderFilter: String
        get() = sp.getString("sms_sender_filter", "")?.trim().orEmpty()
        set(value) = sp.edit().putString("sms_sender_filter", value.trim()).apply()

    var bankPackageFilter: String
        get() = sp.getString("bank_package_filter", "com.vnpay,mbbank,vcb")?.trim().orEmpty()
        set(value) = sp.edit().putString("bank_package_filter", value.trim()).apply()

    fun getShopTargets(): List<ShopTarget> {
        val raw = shopMappingsRaw
        if (raw.isBlank()) return emptyList()

        return raw
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split('|').map { it.trim() }
                if (parts.size < 2) return@mapNotNull null
                val prefix = parts[0].uppercase()
                if (prefix.isBlank()) return@mapNotNull null

                // New mode: PREFIX|WEBHOOK_URL
                if (parts.size == 2 && parts[1].startsWith("http", ignoreCase = true)) {
                    return@mapNotNull ShopTarget(
                        prefix = prefix,
                        botToken = "",
                        chatId = "",
                        webhookUrl = parts[1]
                    )
                }

                // Legacy mode: PREFIX|BOT_TOKEN|CHAT_ID
                if (parts.size >= 3) {
                    val token = parts[1]
                    val chatId = parts[2]
                    if (token.isBlank() || chatId.isBlank()) return@mapNotNull null
                    return@mapNotNull ShopTarget(
                        prefix = prefix,
                        botToken = token,
                        chatId = chatId,
                        webhookUrl = ""
                    )
                }

                null
            }
    }

    fun shouldSkipDuplicate(signature: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val lastSig = sp.getString("last_sig", "") ?: ""
        val lastMs = sp.getLong("last_sig_ms", 0L)
        val duplicated = (lastSig == signature && (nowMs - lastMs) < 120_000)
        if (!duplicated) {
            sp.edit().putString("last_sig", signature).putLong("last_sig_ms", nowMs).apply()
        }
        return duplicated
    }
}
