package com.thegioisua.bankforwarder

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class ShopConfigManager(context: Context) {
    private val sp = context.getSharedPreferences("bank_forwarder", Context.MODE_PRIVATE)
    private val TAG = "ShopConfigManager"

    fun getShops(): List<ShopConfig> {
        val raw = sp.getString("shops_json", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ShopConfig.fromJSON(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            Log.e(TAG, "Parse shops JSON failed", e)
            emptyList()
        }
    }

    fun saveShops(shops: List<ShopConfig>) {
        val arr = JSONArray()
        shops.forEach { arr.put(it.toJSON()) }
        sp.edit().putString("shops_json", arr.toString()).apply()
    }

    fun addShop(displayName: String, prefix: String, webhookUrl: String, hmacSecret: String) {
        val shops = getShops().toMutableList()
        val id = UUID.randomUUID().toString()
        shops.add(ShopConfig(id, displayName, prefix, webhookUrl, hmacSecret))
        saveShops(shops)
    }

    fun updateShop(id: String, displayName: String, prefix: String, webhookUrl: String, hmacSecret: String) {
        val shops = getShops().toMutableList()
        val index = shops.indexOfFirst { it.id == id }
        if (index >= 0) {
            shops[index] = ShopConfig(id, displayName, prefix, webhookUrl, hmacSecret)
            saveShops(shops)
        }
    }

    fun deleteShop(id: String) {
        val shops = getShops().toMutableList()
        shops.removeAll { it.id == id }
        saveShops(shops)
    }

    fun exportJSON(): String {
        val shops = getShops()
        val arr = JSONArray()
        shops.forEach { arr.put(it.toJSON()) }
        return arr.toString(2) // Pretty print
    }

    fun importJSON(jsonString: String): Boolean {
        return try {
            val arr = JSONArray(jsonString)
            val shops = mutableListOf<ShopConfig>()
            for (i in 0 until arr.length()) {
                shops.add(ShopConfig.fromJSON(arr.getJSONObject(i)))
            }
            saveShops(shops)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import JSON failed", e)
            false
        }
    }

    fun initializeDefaultShopsIfEmpty() {
        if (getShops().isEmpty()) {
            saveShops(listOf(
                ShopConfig(
                    id = UUID.randomUUID().toString(),
                    displayName = "TGS Demo (Cửa hàng chính)",
                    prefix = "TGS",
                    webhookUrl = "https://cuahangdemo.quantri.thegioisua.com/wp-json/ttck/v1/telegram-webhook?token=TGS_WEBHOOK_2026",
                    hmacSecret = ""
                ),
                ShopConfig(
                    id = UUID.randomUUID().toString(),
                    displayName = "Nam Định Store",
                    prefix = "TGSNAMDINH",
                    webhookUrl = "https://namdinh.quantri.thegioisua.com/wp-json/ttck/v1/telegram-webhook?token=TGS_WEBHOOK_2027",
                    hmacSecret = ""
                )
            ))
        }
    }

    fun verifyExportPassword(password: String): Boolean {
        // Password: "Thuy!@#"
        val expectedHash = sha256Hex("Thuy!@#")
        val providedHash = sha256Hex(password)
        return expectedHash == providedHash
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun migrateFromLegacyFormat() {
        val legacyRaw = sp.getString("shop_mappings_raw", "")?.trim().orEmpty()
        if (legacyRaw.isNotBlank() && getShops().isEmpty()) {
            val shops = mutableListOf<ShopConfig>()
            legacyRaw.split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .forEach { line ->
                    val parts = line.split('|').map { it.trim() }
                    if (parts.size >= 2) {
                        val prefix = parts[0]
                        val webhookUrl = parts[1]
                        val hmacSecret = if (parts.size >= 3) parts[2] else ""
                        if (prefix.isNotBlank() && webhookUrl.isNotBlank()) {
                            shops.add(ShopConfig(
                                id = UUID.randomUUID().toString(),
                                displayName = "Shop - $prefix",
                                prefix = prefix,
                                webhookUrl = webhookUrl,
                                hmacSecret = hmacSecret
                            ))
                        }
                    }
                }
            if (shops.isNotEmpty()) {
                saveShops(shops)
            }
        }
    }
}
