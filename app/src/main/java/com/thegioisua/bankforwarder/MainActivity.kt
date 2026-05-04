package com.thegioisua.bankforwarder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doAfterTextChanged
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
    private lateinit var etDashboardShopSearch: TextInputEditText

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
    private var shopSearchQuery: String = ""
    private var dashboardShopQuery: String = ""
    private var latestTransactions: List<TransactionRecord> = emptyList()

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
        updateBatteryOptimizationStatus(findViewById(R.id.tvBatteryOptStatus))
    }

    private fun setupDashboard() {
        tvShopCount = findViewById(R.id.tvShopCount)
        tvTodayCount = findViewById(R.id.tvTodayCount)
        tvTodayAmount = findViewById(R.id.tvTodayAmount)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvClearLog = findViewById(R.id.tvClearLog)
        rvTransactions = findViewById(R.id.rvTransactions)
        etDashboardShopSearch = findViewById(R.id.etDashboardShopSearch)

        adapter = TransactionAdapter(mutableListOf())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        etDashboardShopSearch.doAfterTextChanged { editable ->
            dashboardShopQuery = editable?.toString()?.trim().orEmpty()
            renderTransactions()
        }

        tvClearLog.setOnClickListener {
            prefs.clearTransactions()
            refreshDashboard()
            Toast.makeText(this, "Đã xóa log giao dịch", Toast.LENGTH_SHORT).show()
        }
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val shops = ShopConfigManager(this).getShops()
        tvShopCount.text = "Đang theo dõi ${shops.size} shop"

        val (count, amount) = prefs.getTodayStats()
        tvTodayCount.text = "$count GD"
        tvTodayAmount.text = formatAmount(amount)

        latestTransactions = prefs.getTransactions()
        renderTransactions()

        val configured = shops.isNotEmpty() || prefs.getShopTargets().isNotEmpty() || prefs.botToken.isNotBlank()
        tvStatusBadge.text = if (configured) "● Hoạt động" else "○ Chưa cấu hình"
    }

    private fun renderTransactions() {
        val query = dashboardShopQuery.trim()
        val shopMap = ShopConfigManager(this).getShops().associateBy { it.prefix.uppercase() }
        val filtered = if (query.isBlank()) {
            latestTransactions
        } else {
            latestTransactions.filter { tx ->
                val prefix = tx.prefix.uppercase()
                val displayName = shopMap[prefix]?.displayName.orEmpty()
                prefix.contains(query, ignoreCase = true) ||
                    displayName.contains(query, ignoreCase = true) ||
                    tx.orderId.contains(query, ignoreCase = true)
            }
        }

        adapter.updateData(filtered)
        val isEmpty = filtered.isEmpty()
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty && query.isNotBlank()) {
            tvEmptyState.text = "Không tìm thấy shop/giao dịch phù hợp\nThử tên shop hoặc prefix khác"
        } else if (latestTransactions.isEmpty()) {
            tvEmptyState.text = "Chưa có giao dịch nào\nCấu hình và bật app để bắt đầu theo dõi"
        }
    }

    private fun setupSettings() {
        val shopConfigMgr = ShopConfigManager(this)
        shopConfigMgr.migrateFromLegacyFormat()
        shopConfigMgr.initializeDefaultShopsIfEmpty()

        // Shop list setup
        val rvShops = findViewById<RecyclerView>(R.id.rvShops)
        val tvShopListInfo = findViewById<TextView>(R.id.tvShopListInfo)
        val etShopSearch = findViewById<TextInputEditText>(R.id.etShopSearch)
        val adapter = ShopListAdapter(
            shops = shopConfigMgr.getShops().toMutableList(),
            onEdit = { shop ->
                ShopConfigDialog(this, shop) { updated ->
                    shopConfigMgr.updateShop(updated.id, updated.displayName, updated.prefix, updated.webhookUrl, updated.hmacSecret)
                    refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)
                    refreshDashboard()
                }.show()
            },
            onDelete = { shop ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa cửa hàng")
                    .setMessage("Xóa ${shop.displayName}?")
                    .setPositiveButton("Xóa") { _, _ ->
                        shopConfigMgr.deleteShop(shop.id)
                        refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)
                        refreshDashboard()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rvShops.layoutManager = LinearLayoutManager(this)
        rvShops.adapter = adapter
        refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)

        etShopSearch.doAfterTextChanged { editable ->
            shopSearchQuery = editable?.toString()?.trim().orEmpty()
            refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)
        }

        // Buttons
        findViewById<Button>(R.id.btnAddShop).setOnClickListener {
            ShopConfigDialog(this) { shop ->
                shopConfigMgr.addShop(shop.displayName, shop.prefix, shop.webhookUrl, shop.hmacSecret)
                refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)
                refreshDashboard()
            }.show()
        }

        findViewById<Button>(R.id.btnExport).setOnClickListener {
            val passwordInput = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle("Nhập mật khẩu để xuất")
                .setView(passwordInput)
                .setPositiveButton("Xuất") { _, _ ->
                    val pwd = passwordInput.text?.toString().orEmpty()
                    if (shopConfigMgr.verifyExportPassword(pwd)) {
                        val json = shopConfigMgr.exportJSON()
                        // Copy to clipboard
                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("shops.json", json)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "✓ JSON đã copy to clipboard", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "✗ Mật khẩu sai", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .create()
            dialog.show()
        }

        findViewById<Button>(R.id.btnImport).setOnClickListener {
            val editText = EditText(this).apply { inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE }
            val dialog = AlertDialog.Builder(this)
                .setTitle("Nhập JSON + mật khẩu")
                .setView(editText)
                .setPositiveButton("Nhập") { _, _ ->
                    val input = editText.text.toString().trim()
                    if (input.contains("|")) {
                        val parts = input.split("|", limit = 2)
                        val json = parts[0].trim()
                        val pwd = parts.getOrNull(1)?.trim() ?: ""
                        if (shopConfigMgr.verifyExportPassword(pwd)) {
                            if (shopConfigMgr.importJSON(json)) {
                                refreshShopList(rvShops, shopConfigMgr, shopSearchQuery, tvShopListInfo)
                                refreshDashboard()
                                Toast.makeText(this, "✓ Nhập thành công", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "✗ JSON không hợp lệ", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "✗ Mật khẩu sai", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Format: JSON|PASSWORD", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .create()
            dialog.show()
        }

        // Other settings (legacy)
        etSenderFilter = findViewById(R.id.etSenderFilter)
        etPackageFilter = findViewById(R.id.etPackageFilter)
        etBotToken = findViewById(R.id.etBotToken)
        etChatId = findViewById(R.id.etChatId)
        etPrefix = findViewById(R.id.etPrefix)
        switchSms = findViewById(R.id.switchSms)
        switchNoti = findViewById(R.id.switchNoti)
        val tvBatteryOptStatus = findViewById<TextView>(R.id.tvBatteryOptStatus)

        etSenderFilter.setText(prefs.smsSenderFilter)
        etPackageFilter.setText(prefs.bankPackageFilter)
        etBotToken.setText(prefs.botToken)
        etChatId.setText(prefs.chatId)
        etPrefix.setText(prefs.txPrefix)
        switchSms.isChecked = prefs.enabledSms
        switchNoti.isChecked = prefs.enabledNotification
        updateBatteryOptimizationStatus(tvBatteryOptStatus)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            try {
                prefs.smsSenderFilter = etSenderFilter.text.toString()
                prefs.bankPackageFilter = etPackageFilter.text.toString()
                prefs.botToken = etBotToken.text.toString()
                prefs.chatId = etChatId.text.toString()
                prefs.txPrefix = etPrefix.text.toString()
                prefs.enabledSms = switchSms.isChecked
                prefs.enabledNotification = switchNoti.isChecked
                Toast.makeText(this, "✓ Đã lưu cấu hình thành công", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            } catch (e: Exception) {
                Toast.makeText(this, "✗ Lưu cấu hình thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTestSend).setOnClickListener {
            val prefix = shopConfigMgr.getShops().firstOrNull()?.prefix ?: prefs.txPrefix.ifBlank { "TGS" }
            ForwardEngine.handleIncoming(context = this, source = "manual", sender = "TEST", body = "${prefix}159 10000")
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
            showBatteryPermissionGuide(tvBatteryOptStatus)
        }
    }

    private fun showBatteryPermissionGuide(statusView: TextView) {
        val message = "Buoc 1: Bam MO CAP QUYEN ben duoi.\n" +
            "Buoc 2: Tim app TGS Bank Forwarder.\n" +
            "Buoc 3: Chon Khong toi uu/Cho phep chay ngam.\n\n" +
            "Sau khi cap xong quay lai app de cap nhat trang thai."

        AlertDialog.Builder(this)
            .setTitle("Cap quyen chay ngam")
            .setMessage(message)
            .setPositiveButton("Mo cap quyen") { _, _ ->
                requestBatteryOptimizationPermission(statusView)
            }
            .setNegativeButton("De sau", null)
            .show()
    }

    private fun requestBatteryOptimizationPermission(statusView: TextView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            statusView.text = "Trạng thái pin: Thiết bị không cần quyền này"
            Toast.makeText(this, "Thiết bị không yêu cầu tắt tối ưu pin", Toast.LENGTH_SHORT).show()
            return
        }

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            updateBatteryOptimizationStatus(statusView)
            Toast.makeText(this, "App đã được cấp quyền chạy ngầm", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) Prefer system prompt for this app
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Da mo popup cap quyen. Hay bam Cho phep.", Toast.LENGTH_LONG).show()
            return
        } catch (_: Exception) {
        }

        // 2) Fallback to battery optimization settings list
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            Toast.makeText(this, "Da mo danh sach toi uu pin. Chon app va tat toi uu.", Toast.LENGTH_LONG).show()
            return
        } catch (_: Exception) {
        }

        // 3) Last fallback: app details page
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
        Toast.makeText(this, "Da mo thong tin app. Vao Pin/Battery de cho phep chay ngam.", Toast.LENGTH_LONG).show()
    }

    private fun updateBatteryOptimizationStatus(statusView: TextView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            statusView.text = "Trạng thái pin: Thiết bị không cần quyền này"
            return
        }

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val granted = pm.isIgnoringBatteryOptimizations(packageName)
        statusView.text = if (granted) {
            "Trạng thái pin: Da tat toi uu pin (cho phep chay ngam)"
        } else {
            "Trạng thái pin: Chua cap quyen chay ngam"
        }
    }

    private fun refreshShopList(
        rvShops: RecyclerView,
        shopConfigMgr: ShopConfigManager,
        query: String = "",
        tvInfo: TextView? = null
    ) {
        val all = shopConfigMgr.getShops()
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                    it.prefix.contains(query, ignoreCase = true)
            }
        }
        (rvShops.adapter as? ShopListAdapter)?.updateList(filtered)
        tvInfo?.text = "Hiển thị ${filtered.size}/${all.size} shop"
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
