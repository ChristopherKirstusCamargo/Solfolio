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
        val interactionFeedback = booleanPreferencesKey("interaction_feedback")
        val proEntitled = booleanPreferencesKey("pro_entitled")
        val colorPalette = stringPreferencesKey("color_palette")
        val priceRefreshSpeed = stringPreferencesKey("price_refresh_speed")
        val lastDestination = stringPreferencesKey("last_destination")
        val brlPerUsd = doublePreferencesKey("brl_per_usd")
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
            interactionFeedback = preferences[Keys.interactionFeedback] ?: true,
            proEntitled = preferences[Keys.proEntitled] ?: false,
            colorPalette = preferences[Keys.colorPalette]
                ?.let { runCatching { ColorPalette.valueOf(it) }.getOrNull() }
                ?: ColorPalette.SOLANA,
            priceRefreshSpeed = preferences[Keys.priceRefreshSpeed]
                ?.let { runCatching { PriceRefreshSpeed.valueOf(it) }.getOrNull() }
                ?: PriceRefreshSpeed.MEDIUM,
            lastDestination = preferences[Keys.lastDestination] ?: "HOME",
            brlPerUsd = preferences[Keys.brlPerUsd] ?: 5.50,
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
    suspend fun setInteractionFeedback(value: Boolean) = context.abcDataStore.edit { it[Keys.interactionFeedback] = value }
    suspend fun setProEntitled(value: Boolean) = context.abcDataStore.edit { it[Keys.proEntitled] = value }

    suspend fun setColorPalette(value: ColorPalette) = context.abcDataStore.edit {
        it[Keys.colorPalette] = value.name
    }

    suspend fun setPriceRefreshSpeed(value: PriceRefreshSpeed) = context.abcDataStore.edit {
        it[Keys.priceRefreshSpeed] = value.name
    }

    suspend fun setLastDestination(value: String) = context.abcDataStore.edit {
        it[Keys.lastDestination] = value
    }

    suspend fun updateFxRate(value: Double, updatedAt: Long = System.currentTimeMillis()) =
        context.abcDataStore.edit {
            it[Keys.brlPerUsd] = value
            it[Keys.fxUpdatedAt] = updatedAt
        }

    suspend fun restoreUserPreferences(value: AppSettings) = context.abcDataStore.edit {
        it[Keys.theme] = value.theme.name
        it[Keys.displayCurrency] = value.displayCurrency.name
        it[Keys.hideBalances] = value.hideBalances
        it[Keys.colorPalette] = value.colorPalette.name
        it[Keys.priceRefreshSpeed] = value.priceRefreshSpeed.name
        it[Keys.secureScreen] = value.secureScreen
        it[Keys.interactionFeedback] = value.interactionFeedback
    }
}
