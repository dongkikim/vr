package com.example.vrapp.ui

import java.text.NumberFormat
import java.util.Locale

/**
 * 통화 포맷 유틸리티
 */
fun formatCurrency(amount: Double, currency: String): String {
    val locale = when (currency) {
        "USD" -> Locale.US
        "JPY" -> Locale.JAPAN
        else -> Locale.KOREA
    }
    val format = NumberFormat.getCurrencyInstance(locale)
    if (currency == "KRW" || currency == "JPY") {
        format.maximumFractionDigits = 0
    } else {
        format.maximumFractionDigits = 2
    }
    return format.format(amount)
}

/**
 * 숫자를 지정된 소수점 자리수까지 포맷
 */
fun formatNumber(value: Double, decimals: Int): String {
    return String.format("%.${decimals}f", value)
}

/**
 * 원화(KRW) 금액 포맷 (심볼 없이 숫자와 콤마만)
 */
fun formatKRW(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(value.toLong())
}

/**
 * 코인 종목 여부 판별
 */
fun isCoin(ticker: String): Boolean {
    return ticker.endsWith(".bithumb")
}

/**
 * 수량 포맷 유틸리티
 */
fun formatQuantity(quantity: Double, ticker: String): String {
    return if (ticker.endsWith(".bithumb")) {
        String.format("%.6f", quantity).trimEnd('0').trimEnd('.')
    } else {
        String.format("%.0f", quantity)
    }
}
