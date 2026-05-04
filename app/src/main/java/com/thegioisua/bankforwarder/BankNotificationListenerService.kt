package com.thegioisua.bankforwarder

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import android.util.Log

class BankNotificationListenerService : NotificationListenerService() {
    private fun appendPart(parts: LinkedHashSet<String>, value: CharSequence?) {
        val normalized = value?.toString()?.replace(Regex("""\s+"""), " ")?.trim().orEmpty()
        if (normalized.isNotBlank()) {
            parts.add(normalized)
        }
    }

    private fun buildNotificationContent(extras: Bundle): String {
        val parts = linkedSetOf<String>()

        appendPart(parts, extras.getCharSequence(Notification.EXTRA_TITLE))
        appendPart(parts, extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        appendPart(parts, extras.getCharSequence(Notification.EXTRA_TEXT))
        appendPart(parts, extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        appendPart(parts, extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        appendPart(parts, extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach { appendPart(parts, it) }

        for (key in extras.keySet()) {
            when (val value = extras.get(key)) {
                is CharSequence -> appendPart(parts, value)
                is Array<*> -> value.filterIsInstance<CharSequence>().forEach { appendPart(parts, it) }
            }
        }

        return parts.joinToString(" | ")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = AppPrefs(this)
        if (!prefs.enabledNotification) return

        val pkg = sbn.packageName.orEmpty()
        val packageFilter = prefs.bankPackageFilter
        if (packageFilter.isNotBlank()) {
            val allowed = packageFilter.split(',').map { it.trim() }.filter { it.isNotBlank() }
            if (allowed.isNotEmpty() && allowed.none { pkg.contains(it, ignoreCase = true) }) {
                return
            }
        }

        val extras = sbn.notification.extras
        val content = buildNotificationContent(extras)

        if (content.isBlank()) return

        Log.d("BankNotiService", "Notification from $pkg: $content")
        ForwardEngine.handleIncoming(this, "notification", pkg, content)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("BankNotiService", "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("BankNotiService", "Notification listener disconnected, requesting rebind")
        NotificationListenerKeeper.requestRebind(this)
    }
}
