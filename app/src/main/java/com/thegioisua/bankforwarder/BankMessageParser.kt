package com.thegioisua.bankforwarder

data class ParsedTransfer(
    val orderId: String,
    val amount: Long,
    val normalizedMessage: String
)

object BankMessageParser {
    private val genericPlusAmountRegex = Regex(
        """(?i)\+\s*(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\s*(?:vnd|đ|d)"""
    )

    private val genericHintedPlusAmountRegex = Regex(
        """(?i)(?:số tiền|so tien|gd|giao dịch|giao dich|biến động|bien dong|nhận|nhan|chuyển khoản|chuyen khoan|ck)[^\n\r]{0,30}?\+\s*(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\s*(?:vnd|đ|d)?"""
    )

    // MB SMS template: TK... GD: +10,000VND ... ND: TGS186-...
    private fun parseMbTemplate(text: String, prefix: String): ParsedTransfer? {
        val orderId = extractOrderId(text, prefix) ?: return null
        val amountMatch = Regex("""(?i)\bGD\s*:\s*\+\s*(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\s*VND\b""")
            .find(text) ?: return null
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null
        if (!isValidIncomingAmount(amount)) return null
        return ParsedTransfer(orderId = orderId, amount = amount, normalizedMessage = "$prefix$orderId $amount")
    }

    // BIDV notification template: "Số tiền GD: +10,000 VND" and only process positive transactions.
    private fun parseBidvTemplate(text: String, prefix: String): ParsedTransfer? {
        val orderId = extractOrderId(text, prefix) ?: return null
        val m = Regex("""(?i)Số\s*tiền\s*GD\s*:\s*([+-])\s*(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\s*(?:VND|Đ|D)?""")
            .find(text) ?: return null
        val sign = m.groupValues[1]
        if (sign != "+") return null
        val amount = parseAmount(m.groupValues[2]) ?: return null
        if (!isValidIncomingAmount(amount)) return null
        return ParsedTransfer(orderId = orderId, amount = amount, normalizedMessage = "$prefix$orderId $amount")
    }

    // Generic fallback for other banks: prefer hinted "+amount" patterns, then plain "+...VND".
    private fun parseGenericTemplate(text: String, prefix: String): ParsedTransfer? {
        val orderId = extractOrderId(text, prefix) ?: return null

        val hinted = genericHintedPlusAmountRegex.find(text)?.groupValues?.getOrNull(1)
        val hintedAmount = hinted?.let { parseAmount(it) }
        if (hintedAmount != null && isValidIncomingAmount(hintedAmount)) {
            return ParsedTransfer(orderId = orderId, amount = hintedAmount, normalizedMessage = "$prefix$orderId $hintedAmount")
        }

        val generic = genericPlusAmountRegex.find(text)?.groupValues?.getOrNull(1)
        val genericAmount = generic?.let { parseAmount(it) }
        if (genericAmount != null && isValidIncomingAmount(genericAmount)) {
            return ParsedTransfer(orderId = orderId, amount = genericAmount, normalizedMessage = "$prefix$orderId $genericAmount")
        }

        return null
    }

    private fun parseAmount(raw: String): Long? {
        return raw.replace(Regex("[^0-9]"), "").toLongOrNull()
    }

    private fun isValidIncomingAmount(amount: Long): Boolean {
        return amount in 1_000L..2_000_000_000L
    }

    private fun extractOrderId(text: String, prefix: String): String? {
        val orderRegex = Regex("""(?i)\b${Regex.escape(prefix)}\s*(\d{2,10})\b""")
        val orderId = orderRegex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return orderId.ifBlank { null }
    }

    fun parse(raw: String, prefix: String, sender: String = "", source: String = ""): ParsedTransfer? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        // Only process messages that actually contain configured prefix (e.g. TGSxxx)
        if (extractOrderId(text, prefix) == null) return null

        val senderLower = sender.lowercase()
        val sourceLower = source.lowercase()
        val isMb = senderLower.contains("mb") || senderLower.contains("mbbank") || (sourceLower == "sms" && text.contains("ND:", true) && text.contains("GD:", true))
        val isBidv = senderLower.contains("bidv") || senderLower.contains("smartbanking") || text.contains("Thông báo BIDV", true) || text.contains("Số tiền GD", true)

        if (isMb) {
            parseMbTemplate(text, prefix)?.let { return it }
        }
        if (isBidv) {
            parseBidvTemplate(text, prefix)?.let { return it }
        }

        return parseGenericTemplate(text, prefix)
    }
}
