package dev.zhar.abc.util

import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.DisplayCurrency
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun usdToDisplay(valueUsd: Double, settings: AppSettings): Double =
    valueUsd * (settings.fiatPerUsd[settings.displayCurrency] ?: settings.displayCurrency.fallbackPerUsd)

fun formatMoney(valueUsd: Double, settings: AppSettings): String {
    return NumberFormat.getCurrencyInstance(localeFor(settings.displayCurrency)).apply {
        currency = java.util.Currency.getInstance(settings.displayCurrency.currencyCode)
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(usdToDisplay(valueUsd, settings))
}

fun formatMoneyDirect(value: Double, currency: DisplayCurrency): String {
    return NumberFormat.getCurrencyInstance(localeFor(currency)).apply {
        this.currency = java.util.Currency.getInstance(currency.currencyCode)
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

fun ratePerUsd(settings: AppSettings, currency: DisplayCurrency = settings.displayCurrency): Double =
    settings.fiatPerUsd[currency] ?: currency.fallbackPerUsd

fun localeFor(currency: DisplayCurrency): Locale = when (currency) {
    DisplayCurrency.BRL -> Locale("pt", "BR"); DisplayCurrency.USD -> Locale.US; DisplayCurrency.EUR -> Locale.GERMANY
    DisplayCurrency.GBP -> Locale.UK; DisplayCurrency.JPY -> Locale.JAPAN; DisplayCurrency.CAD -> Locale.CANADA
    DisplayCurrency.AUD -> Locale("en", "AU"); DisplayCurrency.CHF -> Locale("de", "CH"); DisplayCurrency.CNY -> Locale.CHINA
    DisplayCurrency.HKD -> Locale("zh", "HK"); DisplayCurrency.SGD -> Locale("en", "SG"); DisplayCurrency.NZD -> Locale("en", "NZ")
    DisplayCurrency.MXN -> Locale("es", "MX"); DisplayCurrency.ARS -> Locale("es", "AR"); DisplayCurrency.CLP -> Locale("es", "CL")
    DisplayCurrency.COP -> Locale("es", "CO"); DisplayCurrency.PEN -> Locale("es", "PE"); DisplayCurrency.UYU -> Locale("es", "UY")
    DisplayCurrency.INR -> Locale("en", "IN"); DisplayCurrency.KRW -> Locale.KOREA; DisplayCurrency.TRY -> Locale("tr", "TR")
    DisplayCurrency.ZAR -> Locale("en", "ZA"); DisplayCurrency.SEK -> Locale("sv", "SE"); DisplayCurrency.NOK -> Locale("nb", "NO")
    DisplayCurrency.DKK -> Locale("da", "DK"); DisplayCurrency.PLN -> Locale("pl", "PL")
}
