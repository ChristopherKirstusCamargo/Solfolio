package dev.zhar.abc.util

import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.DisplayCurrency
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun usdToDisplay(valueUsd: Double, settings: AppSettings): Double =
    if (settings.displayCurrency == DisplayCurrency.BRL) valueUsd * settings.brlPerUsd else valueUsd

fun formatMoney(valueUsd: Double, settings: AppSettings): String {
    val isBrl = settings.displayCurrency == DisplayCurrency.BRL
    val locale = if (isBrl) Locale("pt", "BR") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(usdToDisplay(valueUsd, settings))
}

fun formatMoneyDirect(value: Double, currency: DisplayCurrency): String {
    val locale = if (currency == DisplayCurrency.BRL) Locale("pt", "BR") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(value)
}

fun formatPercent(value: Double?): String = value?.let {
    val sign = if (it > 0.0) "+" else ""
    "$sign${String.format(Locale("pt", "BR"), "%.2f", it)}%"
} ?: "—"

fun formatQuantity(value: Double): String {
    val decimals = when {
        abs(value) >= 1000 -> 2
        abs(value) >= 1 -> 4
        else -> 8
    }
    return NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
    }.format(value)
}

fun hiddenOr(value: String, hidden: Boolean): String = if (hidden) "••••••" else value
