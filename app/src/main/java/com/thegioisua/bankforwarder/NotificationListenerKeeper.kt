package com.thegioisua.bankforwarder

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log

object NotificationListenerKeeper {
    private const val TAG = "NotiKeeper"

    fun requestRebind(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        try {
            val component = ComponentName(context, BankNotificationListenerService::class.java)
            NotificationListenerService.requestRebind(component)
            Log.d(TAG, "Requested rebind for notification listener")
        } catch (e: Exception) {
            Log.w(TAG, "Cannot request rebind: ${e.message}")
        }
    }
}
