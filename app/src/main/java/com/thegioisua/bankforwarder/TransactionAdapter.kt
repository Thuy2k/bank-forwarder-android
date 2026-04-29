package com.thegioisua.bankforwarder

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(private val items: MutableList<TransactionRecord>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBankCircle: TextView = view.findViewById(R.id.tvBankCircle)
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvBankInfo: TextView = view.findViewById(R.id.tvBankInfo)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val bankName = getBankName(item.bank)
        val bankAbbrev = getBankAbbrev(item.bank)
        val bankColor = getBankColor(item.bank)

        holder.tvBankCircle.text = bankAbbrev
        holder.tvBankCircle.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bankColor))
        holder.tvOrderId.text = "Đơn #${item.orderId}"
        holder.tvBankInfo.text = "${item.prefix} · $bankName · ${item.source.uppercase()}"
        holder.tvTime.text = formatTime(item.timeMs)
        holder.tvAmount.text = "+${formatAmount(item.amount)}"

        if (item.status == "success") {
            holder.tvStatus.text = "✓ Thành công"
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvStatus.text = "✗ Lỗi"
            holder.tvStatus.setTextColor(Color.parseColor("#C62828"))
            holder.tvAmount.setTextColor(Color.parseColor("#C62828"))
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TransactionRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun getBankName(sender: String): String = when {
        sender.contains("MB", ignoreCase = true) && !sender.contains("MOMO", ignoreCase = true) -> "MB Bank"
        sender.contains("VCB", ignoreCase = true) || sender.contains("VIETCOMBANK", ignoreCase = true) -> "Vietcombank"
        sender.contains("TCB", ignoreCase = true) || sender.contains("TECHCOMBANK", ignoreCase = true) -> "Techcombank"
        sender.contains("VIB", ignoreCase = true) -> "VIB"
        sender.contains("BIDV", ignoreCase = true) -> "BIDV"
        sender.contains("ACB", ignoreCase = true) -> "ACB"
        sender.contains("VPB", ignoreCase = true) || sender.contains("VPBANK", ignoreCase = true) -> "VPBank"
        sender.contains("MOMO", ignoreCase = true) -> "Momo"
        sender.contains("TEST", ignoreCase = true) -> "Test"
        else -> sender.take(10)
    }

    private fun getBankAbbrev(sender: String): String = when {
        sender.contains("MB", ignoreCase = true) && !sender.contains("MOMO", ignoreCase = true) -> "MB"
        sender.contains("VCB", ignoreCase = true) || sender.contains("VIETCOMBANK", ignoreCase = true) -> "VCB"
        sender.contains("TCB", ignoreCase = true) || sender.contains("TECHCOMBANK", ignoreCase = true) -> "TCB"
        sender.contains("VIB", ignoreCase = true) -> "VIB"
        sender.contains("BIDV", ignoreCase = true) -> "BID"
        sender.contains("ACB", ignoreCase = true) -> "ACB"
        sender.contains("VPB", ignoreCase = true) || sender.contains("VPBANK", ignoreCase = true) -> "VPB"
        sender.contains("MOMO", ignoreCase = true) -> "MO"
        else -> sender.take(3).uppercase()
    }

    private fun getBankColor(sender: String): String = when {
        sender.contains("MB", ignoreCase = true) && !sender.contains("MOMO", ignoreCase = true) -> "#6A1B9A"
        sender.contains("VCB", ignoreCase = true) || sender.contains("VIETCOMBANK", ignoreCase = true) -> "#1B5E20"
        sender.contains("TCB", ignoreCase = true) || sender.contains("TECHCOMBANK", ignoreCase = true) -> "#E65100"
        sender.contains("VIB", ignoreCase = true) -> "#1565C0"
        sender.contains("BIDV", ignoreCase = true) -> "#1A237E"
        sender.contains("ACB", ignoreCase = true) -> "#F57F17"
        sender.contains("VPB", ignoreCase = true) || sender.contains("VPBANK", ignoreCase = true) -> "#00695C"
        sender.contains("MOMO", ignoreCase = true) -> "#AD1457"
        else -> "#546E7A"
    }

    private fun formatAmount(amount: Long): String =
        "%,.0f đ".format(amount.toDouble()).replace(",", ".")

    private fun formatTime(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        return when {
            diff < 60_000L -> "vừa xong"
            diff < 3_600_000L -> "${diff / 60_000} phút trước"
            diff < 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timeMs))
        }
    }
}
