package com.tradeflow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tradeflow.core.R

/**
 * Brand color comes from the active flavor's res (src/<flavor>/res/values/brand.xml).
 * Same code, different brand per customer — automatic.
 */
@Composable
fun TradeFlowTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val brand = Color(ContextCompat.getColor(context, R.color.brandPrimary))
    val scheme = lightColorScheme(
        primary = brand,
        secondary = brand,
        tertiary = brand
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}
