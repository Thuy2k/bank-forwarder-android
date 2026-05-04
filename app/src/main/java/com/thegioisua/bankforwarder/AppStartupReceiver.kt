package com.thegioisua.bankforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppStartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action.orEmpty()
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = AppPrefs(context)
            if (prefs.enabledNotification) {
                NotificationListenerKeeper.requestRebind(context)
                KeepAliveService.start(context)
            }
        }
    }
}
