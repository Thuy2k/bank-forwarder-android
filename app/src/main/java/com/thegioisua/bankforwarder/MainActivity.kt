package com.thegioisua.bankforwarder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private lateinit var etToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var etPrefix: EditText
    private lateinit var etShopMappings: EditText
    private lateinit var etSenderFilter: EditText
    private lateinit var etPackageFilter: EditText
    private lateinit var cbSms: CheckBox
    private lateinit var cbNoti: CheckBox
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etToken = findViewById(R.id.etBotToken)
        etChatId = findViewById(R.id.etChatId)
        etPrefix = findViewById(R.id.etPrefix)
        etShopMappings = findViewById(R.id.etShopMappings)
        etSenderFilter = findViewById(R.id.etSmsSenderFilter)
        etPackageFilter = findViewById(R.id.etPackageFilter)
        cbSms = findViewById(R.id.cbSms)
        cbNoti = findViewById(R.id.cbNotification)
        tvStatus = findViewById(R.id.tvStatus)

        val prefs = AppPrefs(this)
        etToken.setText(prefs.botToken)
        etChatId.setText(prefs.chatId)
        etPrefix.setText(prefs.txPrefix)
        etShopMappings.setText(prefs.shopMappingsRaw)
        etSenderFilter.setText(prefs.smsSenderFilter)
        etPackageFilter.setText(prefs.bankPackageFilter)
        cbSms.isChecked = prefs.enabledSms
        cbNoti.isChecked = prefs.enabledNotification

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.botToken = etToken.text.toString()
            prefs.chatId = etChatId.text.toString()
            prefs.txPrefix = etPrefix.text.toString()
            prefs.shopMappingsRaw = etShopMappings.text.toString()
            prefs.smsSenderFilter = etSenderFilter.text.toString()
            prefs.bankPackageFilter = etPackageFilter.text.toString()
            prefs.enabledSms = cbSms.isChecked
            prefs.enabledNotification = cbNoti.isChecked
            val count = prefs.getShopTargets().size
            tvStatus.text = if (count > 0) {
                "Da luu cau hinh $count shop"
            } else {
                "Da luu cau hinh (single shop fallback)"
            }
            Toast.makeText(this, "Da luu cau hinh", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTestSend).setOnClickListener {
            val firstPrefix = AppPrefs(this).getShopTargets().firstOrNull()?.prefix
            val prefix = firstPrefix ?: etPrefix.text.toString().ifBlank { "TGS" }
            ForwardEngine.handleIncoming(
                context = this,
                source = "manual",
                sender = "manual",
                body = "${prefix}159 10000"
            )
            tvStatus.text = "Da gui test: ${prefix}159 10000"
        }

        findViewById<Button>(R.id.btnOpenNotiAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnOpenSmsPermission).setOnClickListener {
            val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        }

        findViewById<Button>(R.id.btnDisableBatteryOpt).setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}
