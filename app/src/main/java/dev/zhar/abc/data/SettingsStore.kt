package dev.zhar.abc.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zhar.abc.domain.AppSettings
import dev.zhar.abc.domain.ColorPalette
import dev.zhar.abc.domain.DisplayCurrency
import dev.zhar.abc.domain.PriceRefreshSpeed
import dev.zhar.abc.domain.ThemePreference
import dev.zhar.abc.domain.LockTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.abcDataStore by preferencesDataStore(name = "abc_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val displayCurrency = stringPreferencesKey("display_currency")
        val hideBalances = booleanPreferencesKey("hide_balances")
        val biometricLock = booleanPreferencesKey("biometric_lock")
        val secureScreen = booleanPreferencesKey("secure_screen")
        val colorPalette = stringPreferencesKey("color_palette")
        val priceRefreshSpeed = stringPreferencesKey("price_refresh_speed")
        val lockTimeout = stringPreferencesKey("lock_timeout")
        val lastDestination = stringPreferencesKey("last_destination")
        val brlPerUsd = doublePreferencesKey("brl_per_usd")
        val fiatRates = stringPreferencesKey("fiat_rates")
        val fxUpdatedAt = longPreferencesKey("fx_updated_at")
    }

    val settings: Flow<AppSettings> = context.abcDataStore.data.map { preferences ->
        AppSettings(
            theme = preferences[Keys.theme]
                ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.AMOLED,
            displayCurrency = preferences[Keys.displayCurrency]
                ?.let { runCatching { DisplayCurrency.valueOf(it) }.getOrNull() }
                ?: DisplayCurrency.BRL,
            hideBalances = preferences[Keys.hideBalances] ?: false,
            biometricLock = preferences[Keys.biometricLock] ?: false,
            secureScreen = preferences[Keys.secureScreen] ?: false,
            colorPalette = preferences[Keys.colorPalette]
                ?.let { runCatching { ColorPalette.valueOf(it) }.getOrNull() }
                ?: ColorPalette.SOLANA,
            priceRefreshSpeed = preferences[Keys.priceRefreshSpeed]
                ?.let { runCatching { PriceRefreshSpeed.valueOf(it) }.getOrNull() }
                ?: PriceRefreshSpeed.MEDIUM,
            lockTimeout = preferences[Keys.lockTimeout]
                ?.let { runCatching { LockTimeout.valueOf(it) }.getOrNull() }
                ?: LockTimeout.ONE_MINUTE,
            lastDestination = preferences[Keys.lastDestination] ?: "HOME",
            brlPerUsd = preferences[Keys.brlPerUsd] ?: 5.50,
            fiatPerUsd = decodeFiatRates(preferences[Keys.fiatRates]),
            fxUpdatedAt = preferences[Keys.fxUpdatedAt] ?: 0L,
        )
    }

    suspend fun setTheme(value: ThemePreference) = context.abcDataStore.edit {
        it[Keys.theme] = value.name
    }

    suspend fun setDisplayCurrency(value: DisplayCurrency) = context.abcDataStore.edit {
        it[Keys.displayCurrency] = value.name
    }

    suspend fun setHideBalances(value: Boolean) = context.abcDataStore.edit {
        it[Keys.hideBalances] = value
    }

    suspend fun setBiometricLock(value: Boolean) = context.abcDataStore.edit {
        it[Keys.biometricLock] = value
    }

    suspend fun setSecureScreen(value: Boolean) = context.abcDataStore.edit { it[Keys.secureScreen] = value }
    suspend fun setLockTimeout(value: LockTimeout) = context.abcDataStore.edit { it[Keys.lockTimeout] = value.name }

    suspend fun setColorPalette(value: ColorPalette) = context.abcDataStore.edit {
        it[Keys.colorPalette] = value.name
    }

    suspend fun setPriceRefreshSpeed(value: PriceRefreshSpeed) = context.abcDataStore.edit {
        it[Keys.priceRefreshSpeed] = value.name
    }

    suspend fun setLastDestination(value: String) = context.abcDataStore.edit {
        it[Keys.lastDestination] = value
    }

    suspend fun updateFxRates(values: Map<DisplayCurrency, Double>, updatedAt: Long = System.currentTimeMillis()) =
        context.abcDataStore.edit {
            val safe = values.filterValues { rate -> rate.isFinite() && rate > 0.0 }
            it[Keys.brlPerUsd] = safe[DisplayCurrency.BRL] ?: 5.50
            it[Keys.fiatRates] = safe.entries.joinToString(";") { (currency, rate) -> "${currency.name}=$rate" }
            it[Keys.fxUpdatedAt] = updatedAt
        }

    suspend fun restoreUserPreferences(value: AppSettings) = context.abcDataStore.edit {
        it[Keys.theme] = value.theme.name
        it[Keys.displayCurrency] = value.displayCurrency.name
        it[Keys.hideBalances] = value.hideBalances
        it[Keys.colorPalette] = value.colorPalette.name
        it[Keys.priceRefreshSpeed] = value.priceRefreshSpeed.name
        it[Keys.secureScreen] = value.secureScreen
        it[Keys.lockTimeout] = value.lockTimeout.name
        it[Keys.fiatRates] = value.fiatPerUsd.entries.joinToString(";") { (currency, rate) -> "${currency.name}=$rate" }
    }

    private fun decodeFiatRates(raw: String?): Map<DisplayCurrency, Double> {
        val defaults = DisplayCurrency.entries.associateWith { it.fallbackPerUsd }.toMutableMap()
        raw.orEmpty().split(';').forEach { item ->
            val parts = item.split('=', limit = 2)
            val currency = parts.getOrNull(0)?.let { runCatching { DisplayCurrency.valueOf(it) }.getOrNull() }
            val rate = parts.getOrNull(1)?.toDoubleOrNull()
            if (currency != null && rate != null && rate.isFinite() && rate > 0.0) defaults[currency] = rate
        }
        defaults[DisplayCurrency.USD] = 1.0
        return defaults
    }
}
