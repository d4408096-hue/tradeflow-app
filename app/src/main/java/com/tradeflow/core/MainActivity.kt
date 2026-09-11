package com.tradeflow.core

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.tradeflow.core.engine.Defaults
import com.tradeflow.core.ui.nav.TradeFlowApp
import com.tradeflow.core.ui.theme.TradeFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // First-run: seed default message templates (no-op afterwards)
        lifecycleScope.launch {
            runCatching { Defaults.ensureSeeded((application as TradeFlowApp).repo) }
        }
        setContent {
            TradeFlowTheme {
                TradeFlowApp(initialPhone = intent.getStringExtra("phone"))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate() // V1: simplest deep-link refresh for notification taps
    }
}
