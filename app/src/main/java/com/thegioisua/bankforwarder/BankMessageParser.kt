package com.thegioisua.bankforwarder

data class ParsedTransfer(
    val orderId: String,
    val amount: Long,
    val normalizedMessage: String
)

object BankMessageParser {
    private val amountRegex = Regex("""(?<!\d)(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})(?!\d)""")
    private val amountHintRegex = Regex(
        """(?i)(?:\+|nhan|nh\u1eadn|giao d\u1ecbch|gd|chuy\u1ec3n kho\u1ea3n|ck)\D{0,24}(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\D{0,8}(?:vnd|d|\u0111)?"""
    )

    private fun parseMbStyle(text: String, prefix: String): ParsedTransfer? {
        // Example:
        // TK 50xxx000 GD: +20,000VND ... ND: TGS169- Ma GD ACSP/93713148
        val orderRegex = Regex("""(?i)ND\s*:\s*.*?${Regex.escape(prefix)}\s*(\d{2,10})""")
        val amountRegexMb = Regex("""(?i)GD\s*:\s*\+?\s*(\d{1,3}(?:[\.,\s]\d{3})+|\d{4,12})\s*VND""")

        val orderMatch = orderRegex.find(text) ?: return null
        val amountMatch = amountRegexMb.find(text) ?: return null

        val orderId = orderMatch.groupValues.getOrNull(1)?.trim().orEmpty()
        val amount = amountMatch.groupValues.getOrNull(1)
            ?.replace(Regex("[^0-9]"), "")
            ?.toLongOrNull()
            ?: return null

        if (orderId.isBlank() || amount < 1000) return null

        return ParsedTransfer(
            orderId = orderId,
            amount = amount,
            normalizedMessage = "$prefix$orderId $amount"
        )
    }

    fun parse(raw: String, prefix: String): ParsedTransfer? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        // Prefer strict MB SMS pattern when present to avoid capturing SD/Ma GD numbers.
        if (text.contains("GD:", ignoreCase = true) && text.contains("ND:", ignoreCase = true)) {
            parseMbStyle(text, prefix)?.let { return it }
        }

        val orderRegex = Regex("""(?i)${Regex.escape(prefix)}\s*(\d{2,10})""")
        val orderMatch = orderRegex.find(text) ?: return null
        val orderId = orderMatch.groupValues.getOrNull(1)?.trim().orEmpty()
        if (orderId.isEmpty()) return null

        val hintedAmounts = amountHintRegex.findAll(text)
            .map { it.groupValues[1] }
            .map { it.replace(Regex("[^0-9]"), "") }
            .mapNotNull { it.toLongOrNull() }
            .filter { it >= 1000 }
            .toList()

        val fallbackAmounts = amountRegex.findAll(text)
            .map { it.groupValues[1] }
            .map { it.replace(Regex("[^0-9]"), "") }
            .mapNotNull { it.toLongOrNull() }
            .filter { it in 1000..500_000_000 }
            .toList()

        val amounts = if (hintedAmounts.isNotEmpty()) hintedAmounts else fallbackAmounts

        if (amounts.isEmpty()) return null
        val amount = amounts.maxOrNull() ?: return null

        return ParsedTransfer(
            orderId = orderId,
            amount = amount,
            normalizedMessage = "$prefix$orderId $amount"
        )
    }
}
