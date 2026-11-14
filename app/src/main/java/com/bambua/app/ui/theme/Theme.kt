// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package com.bambua.app.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import com.bambua.app.data.BackgroundTheme
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

/* ============================ Fondos seleccionables (v0.3, spec §1) ============================ */

/** Paleta de fondo por tema (tabla §1 de art/temas-fondos.md — valores al carácter). */
data class ThemePalette(
    val headerTop: Color, val headerBottom: Color,
    val headerTopDark: Color, val headerBottomDark: Color,
    val surface: Color, val surfaceDark: Color,
    val cardSurface: Color,
    val accent: Color,
)

val BackgroundTheme.palette: ThemePalette get() = when (this) {
    BackgroundTheme.OCEANO -> ThemePalette(
        Color(0xFF0A2540), Color(0xFF123A5C), Color(0xFF050B14), Color(0xFF0C1B2E),
        Color(0xFFF7F9FC), Color(0xFF0F1419), Color(0xFFEDF2F7), Color(0xFF1FA36B),
    )
    BackgroundTheme.SELVA -> ThemePalette(
        Color(0xFF0D2B1F), Color(0xFF14532D), Color(0xFF06140D), Color(0xFF0B2417),
        Color(0xFFF6FAF7), Color(0xFF0F1419), Color(0xFFEAF3EE), Color(0xFF1FA36B),
    )
    BackgroundTheme.ORQUIDEA -> ThemePalette(
        Color(0xFF2B2140), Color(0xFF3B2E5C), Color(0xFF140F20), Color(0xFF1D1730),
        Color(0xFFFAF8FD), Color(0xFF0F1419), Color(0xFFF1EEF8), Color(0xFF6A4C93),
    )
    BackgroundTheme.CARBON -> ThemePalette(
        Color(0xFF111418), Color(0xFF1F242B), Color(0xFF0A0C0E), Color(0xFF14181E),
        Color(0xFFF5F5F6), Color(0xFF0F1419), Color(0xFFEFF0F1), Color(0xFFF2A900),
    )
}

/** Esquema claro por tema (spec §1 y §2: acento como acción, superficies tintadas, divisores con alpha 8%). */
private fun lightSchemeFor(p: ThemePalette, accentSecondary: Color): ColorScheme = lightColorScheme(
    primary = p.accent, onPrimary = Color(0xFFFFFFFF),
    primaryContainer = when (p.accent) {
        Color(0xFF1FA36B) -> Color(0xFFBDF2D6)
        Color(0xFF6A4C93) -> Color(0xFFEADDFF)
        else -> Color(0xFFFFE3B3)
    }, onPrimaryContainer = when (p.accent) {
        Color(0xFF6A4C93) -> Color(0xFF221444)
        Color(0xFFF2A900) -> Color(0xFF3A2500)
        else -> Color(0xFF00391E)
    },
    secondary = accentSecondary, onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = when (accentSecondary) {
        Color(0xFF0D2B1F) -> Color(0xFFDCEEE4)
        Color(0xFF14532D) -> Color(0xFFDCEEE4)
        else -> Color(0xFFCFDFF2)
    }, onSecondaryContainer = Color(0xFF071E26),
    tertiary = Color(0xFF6A4C93), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF), onTertiaryContainer = Color(0xFF221444),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = p.surface, onBackground = Color(0xFF191C20),
    surface = p.surface, onSurface = Color(0xFF191C20),
    surfaceVariant = p.cardSurface, onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF74777F),
    // §2.4: divisores teñidos con el primario al 8%.
    outlineVariant = p.accent.copy(alpha = 0.08f),
    inverseSurface = Color(0xFF2D3035), inverseOnSurface = Color(0xFFEFF0F4), inversePrimary = Color(0xFFA6C8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = p.cardSurface.copy(alpha = 0.55f).compositeOver(Color(0xFFFFFFFF)),
    surfaceContainer = p.cardSurface,
    surfaceContainerHigh = p.cardSurface.copy(alpha = 0.85f).compositeOver(Color(0xFFFFFFFF)),
    surfaceContainerHighest = p.cardSurface.copy(alpha = 1.1f).compositeOver(Color(0xFFFFFFFF)),
)

private fun Color.compositeOver(other: Color): Color {
    val a = alpha
    return Color(
        red = red * a + other.red * (1 - a),
        green = green * a + other.green * (1 - a),
        blue = blue * a + other.blue * (1 - a),
        alpha = 1f,
    )
}

/** Esquema oscuro espejado por tema (spec §1: franjas #050B14-based, superficies #0F1419-family). */
private fun darkSchemeFor(p: ThemePalette, accentSecondary: Color): ColorScheme = darkColorScheme(
    primary = when (p.accent) {
        Color(0xFF1FA36B) -> Color(0xFF7FD9A9)
        Color(0xFF6A4C93) -> Color(0xFFD3BBFF)
        else -> Color(0xFFFFC94D)
    }, onPrimary = Color(0xFF0A2540),
    primaryContainer = when (p.accent) {
        Color(0xFF1FA36B) -> Color(0xFF00522B)
        Color(0xFF6A4C93) -> Color(0xFF503574)
        else -> Color(0xFF6E4A00)
    }, onPrimaryContainer = Color(0xFFBDF2D6),
    secondary = if (accentSecondary == Color(0xFF0A2540)) Color(0xFFA6C8F5) else Color(0xFFBDF2D6),
    onSecondary = Color(0xFF0A2540),
    secondaryContainer = Color(0xFF27476E), onSecondaryContainer = Color(0xFFCFDFF2),
    tertiary = Color(0xFFD3BBFF), onTertiary = Color(0xFF381E5C),
    tertiaryContainer = Color(0xFF503574), onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = p.surfaceDark, onBackground = Color(0xFFE1E2E6),
    surface = p.surfaceDark, onSurface = Color(0xFFE1E2E6),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC4C7CF),
    outline = Color(0xFF8E9199), outlineVariant = Color(0xFF43474E),
    inverseSurface = Color(0xFFE1E2E6), inverseOnSurface = Color(0xFF2D3035), inversePrimary = Color(0xFF0A2540),
    surfaceContainerLowest = Color(0xFF0C0E11), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

/** §2.6: transición de 300ms al cambiar de fondo — los colores se funden, no saltan. */
@Composable
private fun animatedScheme(target: ColorScheme): ColorScheme {
    @Composable fun a(c: Color) = animateColorAsState(c, tween(300), label = "theme").value
    return target.copy(
        primary = a(target.primary), onPrimary = a(target.onPrimary),
        primaryContainer = a(target.primaryContainer), onPrimaryContainer = a(target.onPrimaryContainer),
        secondary = a(target.secondary), onSecondary = a(target.onSecondary),
        secondaryContainer = a(target.secondaryContainer), onSecondaryContainer = a(target.onSecondaryContainer),
        tertiary = a(target.tertiary), onTertiary = a(target.onTertiary),
        tertiaryContainer = a(target.tertiaryContainer), onTertiaryContainer = a(target.onTertiaryContainer),
        background = a(target.background), onBackground = a(target.onBackground),
        surface = a(target.surface), onSurface = a(target.onSurface),
        surfaceVariant = a(target.surfaceVariant), onSurfaceVariant = a(target.onSurfaceVariant),
        outline = a(target.outline), outlineVariant = a(target.outlineVariant),
        error = a(target.error), onError = a(target.onError),
        errorContainer = a(target.errorContainer), onErrorContainer = a(target.onErrorContainer),
        inverseSurface = a(target.inverseSurface), inverseOnSurface = a(target.inverseOnSurface),
        inversePrimary = a(target.inversePrimary),
    )
}

@Composable
fun BambuaTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = false, background: BackgroundTheme = BackgroundTheme.OCEANO, content: @Composable () -> Unit) {
    val dark = isDarkTheme(mode)
    val ctx = LocalContext.current
    val palette = background.palette
    val accentSecondary = when (background) {
        BackgroundTheme.OCEANO, BackgroundTheme.ORQUIDEA, BackgroundTheme.CARBON -> Color(0xFF0A2540)
        BackgroundTheme.SELVA -> Color(0xFF0D2B1F)
    }
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkSchemeFor(palette, accentSecondary)
        else -> lightSchemeFor(palette, accentSecondary)
    }
    val finalScheme = if (dynamicColor && Build.VERSION.SDK_INT >= 31) scheme else animatedScheme(scheme)
    CompositionLocalProvider(LocalBambuaColors provides if (dark) DarkExt else LightExt) {
        MaterialTheme(colorScheme = finalScheme, typography = BambuaTypography, shapes = BambuaShapes, content = content)
    }
}
