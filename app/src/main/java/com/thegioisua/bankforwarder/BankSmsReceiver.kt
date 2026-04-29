package com.thegioisua.bankforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class BankSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = AppPrefs(context)
        if (!prefs.enabledSms) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress.orEmpty()
            val body = sms.displayMessageBody.orEmpty()
            if (body.isBlank()) continue

            val senderFilter = prefs.smsSenderFilter
            if (senderFilter.isNotBlank() && !sender.contains(senderFilter, ignoreCase = true)) {
                continue
            }

            Log.d("BankSmsReceiver", "SMS from $sender")
            ForwardEngine.handleIncoming(context, "sms", sender, body)
        }
    }
}
