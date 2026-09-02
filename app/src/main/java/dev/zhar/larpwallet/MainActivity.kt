package dev.zhar.larpwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zhar.larpwallet.ui.LarpWalletApp
import dev.zhar.larpwallet.ui.WalletViewModel
import dev.zhar.larpwallet.ui.theme.AppBackground
import dev.zhar.larpwallet.ui.theme.LarpWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LarpWalletTheme {
                SideEffect {
                    window.navigationBarColor = AppBackground.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    }
                }
                val walletViewModel: WalletViewModel = viewModel()
                LarpWalletApp(viewModel = walletViewModel)
            }
        }
    }
}
