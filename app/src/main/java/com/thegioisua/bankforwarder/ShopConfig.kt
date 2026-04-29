package com.thegioisua.bankforwarder

import org.json.JSONObject

data class ShopConfig(
    val id: String,          // Unique ID (UUID or prefix)
    val displayName: String, // User-friendly name (e.g., "Cửa hàng Nam Định")
    val prefix: String,      // Bank message prefix (e.g., "TGSNAMDINH")
    val webhookUrl: String,  // Webhook endpoint
    val hmacSecret: String   // HMAC secret (can be empty)
) {
    fun toJSON(): JSONObject = JSONObject().apply {
        put("id", id)
        put("displayName", displayName)
        put("prefix", prefix)
        put("webhookUrl", webhookUrl)
        put("hmacSecret", hmacSecret)
    }

    companion object {
        fun fromJSON(obj: JSONObject): ShopConfig = ShopConfig(
            id = obj.optString("id", ""),
            displayName = obj.optString("displayName", ""),
            prefix = obj.optString("prefix", ""),
            webhookUrl = obj.optString("webhookUrl", ""),
            hmacSecret = obj.optString("hmacSecret", "")
        )
    }
}
