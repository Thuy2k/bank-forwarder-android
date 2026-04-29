package com.thegioisua.bankforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BankNotificationListenerService : NotificationListenerService() {
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
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val content = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(" | ")

        if (content.isBlank()) return

        Log.d("BankNotiService", "Notification from $pkg")
        ForwardEngine.handleIncoming(this, "notification", pkg, content)
    }
}
