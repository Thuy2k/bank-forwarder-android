package com.thegioisua.bankforwarder

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.view.setPadding

class ShopConfigDialog(
    context: Context,
    private val existingShop: ShopConfig? = null,
    private val onSave: (ShopConfig) -> Unit
) : Dialog(context) {

    private lateinit var etDisplayName: TextInputEditText
    private lateinit var etPrefix: TextInputEditText
    private lateinit var etWebhookUrl: TextInputEditText
    private lateinit var etHmacSecret: TextInputEditText

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32)

            // Title
            addView(TextView(context).apply {
                text = if (existingShop == null) "Thêm cửa hàng mới" else "Cập nhật cửa hàng"
                textSize = 18f
                setTextColor(context.resources.getColor(android.R.color.black, null))
                setPadding(0, 0, 0, 24)
            })

            // Display Name
            addView(TextInputLayout(context, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                hint = "Tên cửa hàng (VD: Cửa hàng Nam Định)"
                addView(TextInputEditText(context).apply {
                    etDisplayName = this
                    setText(existingShop?.displayName ?: "")
                })
            })

            // Prefix
            addView(TextInputLayout(context, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                hint = "Mã ngân hàng (VD: TGSNAMDINH)"
                addView(TextInputEditText(context).apply {
                    etPrefix = this
                    setText(existingShop?.prefix ?: "")
                })
            })

            // Webhook URL
            addView(TextInputLayout(context, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                hint = "Webhook URL"
                addView(TextInputEditText(context).apply {
                    etWebhookUrl = this
                    // Nếu edit existing & webhook có data: hiện *** để giấu
                    setText(if (existingShop?.webhookUrl?.isNotBlank() == true) "***" else "")
                    minLines = 3
                })
            })

            // HMAC Secret
            addView(TextInputLayout(context, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 24 }
                hint = "HMAC Secret (tùy chọn)"
                addView(TextInputEditText(context).apply {
                    etHmacSecret = this
                    // Nếu edit existing & hmac có data: hiện *** để giấu
                    setText(if (existingShop?.hmacSecret?.isNotBlank() == true) "***" else "")
                })
            })

            // Buttons
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(Button(context).apply {
                    text = "Hủy"
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
                    setOnClickListener { dismiss() }
                })

                addView(Button(context).apply {
                    text = "Lưu"
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        val displayName = etDisplayName.text.toString().trim()
                        val prefix = etPrefix.text.toString().trim()
                        var webhookUrl = etWebhookUrl.text.toString().trim()
                        var hmacSecret = etHmacSecret.text.toString().trim()

                        // Nếu nhập *** (giấu): dùng giá trị cũ
                        if (webhookUrl == "***") {
                            webhookUrl = existingShop?.webhookUrl ?: ""
                        }
                        if (hmacSecret == "***") {
                            hmacSecret = existingShop?.hmacSecret ?: ""
                        }

                        when {
                            displayName.isBlank() -> Toast.makeText(context, "✗ Tên cửa hàng không được để trống", Toast.LENGTH_SHORT).show()
                            prefix.isBlank() -> Toast.makeText(context, "✗ Mã ngân hàng không được để trống", Toast.LENGTH_SHORT).show()
                            webhookUrl.isBlank() -> Toast.makeText(context, "✗ Webhook URL không được để trống", Toast.LENGTH_SHORT).show()
                            else -> {
                                val shop = ShopConfig(
                                    id = existingShop?.id ?: java.util.UUID.randomUUID().toString(),
                                    displayName = displayName,
                                    prefix = prefix,
                                    webhookUrl = webhookUrl,
                                    hmacSecret = hmacSecret
                                )
                                onSave(shop)
                                dismiss()
                            }
                        }
                    }
                })
            })
        }
        setContentView(layout)
    }
}
