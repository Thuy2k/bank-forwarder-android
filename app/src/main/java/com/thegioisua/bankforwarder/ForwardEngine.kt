package com.thegioisua.bankforwarder

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

object ForwardEngine {
    private const val TAG = "ForwardEngine"

    private data class RoutedMessage(
        val target: ShopTarget,
        val parsed: ParsedTransfer
    )

    private fun findTarget(body: String, targets: List<ShopTarget>): RoutedMessage? {
        for (target in targets) {
            val parsed = BankMessageParser.parse(body, target.prefix)
            if (parsed != null) {
                return RoutedMessage(target, parsed)
            }
        }
        return null
    }

    fun handleIncoming(context: Context, source: String, sender: String, body: String) {
        val prefs = AppPrefs(context)
        val mappedTargets = prefs.getShopTargets()
        val targets = if (mappedTargets.isNotEmpty()) {
            mappedTargets
        } else {
            val token = prefs.botToken
            val chatId = prefs.chatId
            val prefix = prefs.txPrefix.ifBlank { "TGS" }.uppercase()
            if (token.isBlank() || chatId.isBlank()) emptyList() else listOf(
                ShopTarget(prefix = prefix, botToken = token, chatId = chatId, webhookUrl = "")
            )
        }

        if (targets.isEmpty()) {
            Log.w(TAG, "Skip forwarding: missing bot token/chat id")
            return
        }

        val routed = findTarget(body, targets) ?: run {
            Log.d(TAG, "Skip forwarding: cannot parse transfer from $source")
            return
        }

        val signature = "$source|$sender|${routed.target.prefix}|${routed.parsed.normalizedMessage}"
        if (prefs.shouldSkipDuplicate(signature)) {
            Log.d(TAG, "Skip duplicate: $signature")
            return
        }

        thread {
            var status = "failed"
            try {
                if (routed.target.webhookUrl.isNotBlank()) {
                    WebhookForwarder.forward(routed.target.webhookUrl, routed.parsed.normalizedMessage)
                } else {
                    TelegramSender.sendMessage(
                        routed.target.botToken,
                        routed.target.chatId,
                        routed.parsed.normalizedMessage
                    )
                }
                status = "success"
                Log.i(TAG, "Forwarded from $source: ${routed.parsed.normalizedMessage}")
            } catch (e: Exception) {
                Log.e(TAG, "Forward failed: ${e.message}", e)
            } finally {
                prefs.saveTransaction(
                    TransactionRecord(
                        timeMs = System.currentTimeMillis(),
                        orderId = routed.parsed.orderId,
                        amount = routed.parsed.amount,
                        prefix = routed.target.prefix,
                        bank = sender,
                        source = source,
                        status = status
                    )
                )
            }
        }
    }
}
