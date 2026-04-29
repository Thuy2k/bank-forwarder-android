package com.thegioisua.bankforwarder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    // Dashboard views
    private lateinit var tvShopCount: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var tvTodayAmount: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvClearLog: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var adapter: TransactionAdapter

    // Settings views
    private lateinit var etShopMappings: TextInputEditText
    private lateinit var etSenderFilter: TextInputEditText
    private lateinit var etPackageFilter: TextInputEditText
    private lateinit var etBotToken: TextInputEditText
    private lateinit var etChatId: TextInputEditText
    private lateinit var etPrefix: TextInputEditText
    private lateinit var switchSms: SwitchMaterial
    private lateinit var switchNoti: SwitchMaterial

    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = AppPrefs(this)
        setupDashboard()
        setupSettings()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun setupDashboard() {
        tvShopCount = findViewById(R.id.tvShopCount)
        tvTodayCount = findViewById(R.id.tvTodayCount)
        tvTodayAmount = findViewById(R.id.tvTodayAmount)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvClearLog = findViewById(R.id.tvClearLog)
        rvTransactions = findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter(mutableListOf())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        tvClearLog.setOnClickListener {
            prefs.clearTransactions()
            refreshDashboard()
            Toast.makeText(this, "Đã xóa log giao dịch", Toast.LENGTH_SHORT).show()
        }
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val shops = prefs.getShopTargets()
        tvShopCount.text = "Đang theo dõi ${shops.size} shop"

        val (count, amount) = prefs.getTodayStats()
        tvTodayCount.text = "$count GD"
        tvTodayAmount.text = formatAmount(amount)

        val txList = prefs.getTransactions()
        adapter.updateData(txList)
        tvEmptyState.visibility = if (txList.isEmpty()) View.VISIBLE else View.GONE
        rvTransactions.visibility = if (txList.isEmpty()) View.GONE else View.VISIBLE

        val configured = shops.isNotEmpty() || prefs.botToken.isNotBlank()
        tvStatusBadge.text = if (configured) "● Hoạt động" else "○ Chưa cấu hình"
    }

    private fun setupSettings() {
        etShopMappings = findViewById(R.id.etShopMappings)
        etSenderFilter = findViewById(R.id.etSenderFilter)
        etPackageFilter = findViewById(R.id.etPackageFilter)
        etBotToken = findViewById(R.id.etBotToken)
        etChatId = findViewById(R.id.etChatId)
        etPrefix = findViewById(R.id.etPrefix)
        switchSms = findViewById(R.id.switchSms)
        switchNoti = findViewById(R.id.switchNoti)

        etShopMappings.setText(prefs.shopMappingsRaw)
        etSenderFilter.setText(prefs.smsSenderFilter)
        etPackageFilter.setText(prefs.bankPackageFilter)
        etBotToken.setText(prefs.botToken)
        etChatId.setText(prefs.chatId)
        etPrefix.setText(prefs.txPrefix)
        switchSms.isChecked = prefs.enabledSms
        switchNoti.isChecked = prefs.enabledNotification

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            prefs.shopMappingsRaw = etShopMappings.text.toString()
            prefs.smsSenderFilter = etSenderFilter.text.toString()
            prefs.bankPackageFilter = etPackageFilter.text.toString()
            prefs.botToken = etBotToken.text.toString()
            prefs.chatId = etChatId.text.toString()
            prefs.txPrefix = etPrefix.text.toString()
            prefs.enabledSms = switchSms.isChecked
            prefs.enabledNotification = switchNoti.isChecked
            Toast.makeText(this, "✓ Đã lưu cấu hình", Toast.LENGTH_SHORT).show()
            refreshDashboard()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTestSend).setOnClickListener {
            val prefix = prefs.getShopTargets().firstOrNull()?.prefix ?: prefs.txPrefix.ifBlank { "TGS" }
            ForwardEngine.handleIncoming(
                context = this,
                source = "manual",
                sender = "TEST",
                body = "${prefix}159 10000"
            )
            Toast.makeText(this, "Đã gửi test: ${prefix}159 10000", Toast.LENGTH_SHORT).show()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNotificationAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSmsPermission).setOnClickListener {
            val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatteryOpt).setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun setupBottomNav() {
        val dashboardContainer = findViewById<View>(R.id.dashboardContainer)
        val settingsContainer = findViewById<View>(R.id.settingsContainer)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    dashboardContainer.visibility = View.VISIBLE
                    settingsContainer.visibility = View.GONE
                    refreshDashboard()
                    true
                }
                R.id.nav_settings -> {
                    dashboardContainer.visibility = View.GONE
                    settingsContainer.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun formatAmount(amount: Long): String {
        if (amount == 0L) return "0 đ"
        return "%,.0f đ".format(amount.toDouble()).replace(",", ".")
    }
}
