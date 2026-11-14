// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package com.bambua.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bambua.app.data.ThemeMode

/* Paleta própria Bambua: primário azul profundo #0A2540, secundário verde bambú #1FA36B,
 * terciário orquídea #6A4C93, alerta âmbar #F2A900. Usada quando as cores do papel de parede
 * (Material You) estão desligadas ou o aparelho é anterior ao Android 12. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0A2540), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFDFF2), onPrimaryContainer = Color(0xFF0A2540),
    secondary = Color(0xFF1FA36B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDF2D6), onSecondaryContainer = Color(0xFF00391E),
    tertiary = Color(0xFF6A4C93), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF), onTertiaryContainer = Color(0xFF221444),
    // v0.2 (T5.4): el error vuelve al rojo M3 accesible (6.54 AA); el ámbar #F2A900 queda
    // solo para advertencias e iconos de alerta (BambuaColors.warning / warningContainer).
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FC), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF7F9FC), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE4EC), onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C7CF),
    inverseSurface = Color(0xFF2D3035), inverseOnSurface = Color(0xFFEFF0F4), inversePrimary = Color(0xFFA6C8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF1F3F7),
    surfaceContainer = Color(0xFFEBEDF2), surfaceContainerHigh = Color(0xFFE5E8EC),
    surfaceContainerHighest = Color(0xFFDFE2E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6C8F5), onPrimary = Color(0xFF0A2540),
    primaryContainer = Color(0xFF27476E), onPrimaryContainer = Color(0xFFCFDFF2),
    secondary = Color(0xFF7FD9A9), onSecondary = Color(0xFF00391F),
    secondaryContainer = Color(0xFF00522B), onSecondaryContainer = Color(0xFFBDF2D6),
    tertiary = Color(0xFFD3BBFF), onTertiary = Color(0xFF381E5C),
    tertiaryContainer = Color(0xFF503574), onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111417), onBackground = Color(0xFFE1E2E6),
    surface = Color(0xFF111417), onSurface = Color(0xFFE1E2E6),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC4C7CF),
    outline = Color(0xFF8E9199), outlineVariant = Color(0xFF43474E),
    inverseSurface = Color(0xFFE1E2E6), inverseOnSurface = Color(0xFF2D3035), inversePrimary = Color(0xFF0A2540),
    surfaceContainerLowest = Color(0xFF0C0E11), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

/** Cores de significado fixo (entrada = verde, saída = vermelho, atenção = âmbar), sempre com bom contraste. */
@Immutable
data class BambuaColors(
    val income: Color,
    /** Verde para TEXTO pequeño (AA 5.33 sobre blanco); [income] queda para iconos/rellenos/barras (T5.2). */
    val incomeText: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val isDark: Boolean,
)

private val LightExt = BambuaColors(
    income = Color(0xFF1FA36B), incomeText = Color(0xFF157A52),
    incomeContainer = Color(0xFFBDF2D6), onIncomeContainer = Color(0xFF00391E),
    expense = Color(0xFFB3261E), expenseContainer = Color(0xFFFFDAD6), onExpenseContainer = Color(0xFF410002),
    warning = Color(0xFF8A5100), warningContainer = Color(0xFFFFE3B3), onWarningContainer = Color(0xFF3A2500),
    isDark = false,
)

private val DarkExt = BambuaColors(
    income = Color(0xFF1FA36B), incomeText = Color(0xFF8FDDB2),
    incomeContainer = Color(0xFF00522B), onIncomeContainer = Color(0xFFBDF2D6),
    expense = Color(0xFFFFB4AB), expenseContainer = Color(0xFF93000A), onExpenseContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFFFC94D), warningContainer = Color(0xFF6E4A00), onWarningContainer = Color(0xFFFFDEAB),
    isDark = true,
)

val LocalBambuaColors = staticCompositionLocalOf { LightExt }

object BambuaTheme {
    val colors: BambuaColors
        @Composable get() = LocalBambuaColors.current
}

private val BambuaTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        displayMedium = t.displayMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        bodyLarge = t.bodyLarge.copy(fontFeatureSettings = "tnum"),
        bodyMedium = t.bodyMedium.copy(fontFeatureSettings = "tnum"),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val BambuaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun BambuaTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val dark = isDarkTheme(mode)
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalBambuaColors provides if (dark) DarkExt else LightExt) {
        MaterialTheme(colorScheme = scheme, typography = BambuaTypography, shapes = BambuaShapes, content = content)
    }
}
