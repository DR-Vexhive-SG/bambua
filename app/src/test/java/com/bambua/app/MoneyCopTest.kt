// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: regla COP de Colombia (v0.2) — el peso no lleva centavos.
package com.bambua.app

import com.bambua.app.domain.AppLanguage
import com.bambua.app.domain.AppLocale
import com.bambua.app.domain.Money
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Formateador consciente de moneda: COP → 0 decimales + "$" pegado; las demás monedas intactas. */
class MoneyCopTest {

    @After
    fun restore() = AppLocale.reset()

    @Test
    fun copSinDecimales() {
        AppLocale.set(AppLanguage.ES, Locale.forLanguageTag("es-CO"), "COP")
        // 1000 pesos (100000 centavos) -> "$1.000": símbolo pegado, punto de miles, sin centavos.
        assertEquals("$1.000", Money.format(100000))
        // Redondeo del display: el valor guardado sigue en centavos.
        assertEquals("$1.001", Money.format(100050))
        assertEquals("$1.000", Money.format(99999))
        // Sin símbolo: solo el número.
        assertEquals("1.000", Money.format(100000, symbol = false))
        // Con signo explícito (Entró/Salió).
        // El signo va pegado al símbolo con espacio no separable (TXT-07): "+ $1.000".
        assertEquals("+\u00A0$1.000", Money.signed(100000))
        assertEquals("−\u00A0$500", Money.signed(-50000))
    }

    @Test
    fun copValoresRedondosYGrandes() {
        AppLocale.set(AppLanguage.ES, Locale.forLanguageTag("es-CO"), "COP")
        assertEquals("$0", Money.format(0))
        assertEquals("$50.000", Money.format(5000000))
        assertEquals("$2.750", Money.format(275000))
    }

    @Test
    fun usdConservaCentavos() {
        // Locale inglés + USD: sigue viendo centavos (v0.1 intacto).
        AppLocale.set(AppLanguage.EN, Locale.US, "USD")
        assertEquals("$1,000.00", Money.format(100000))
        assertEquals("$52,750.00", Money.format(5275000))
    }

    @Test
    fun usdEnLocaleEspanolConservaDecimales() {
        // Locale español + USD: dos decimales (la regla sin centavos es SOLO del COP).
        AppLocale.set(AppLanguage.ES, Locale.forLanguageTag("es-CO"), "USD")
        val v = Money.format(100000)
        assertEquals(true, v.contains("1.000") && v.contains(",00"))
    }

    @Test
    fun copCompactoSinCentavos() {
        AppLocale.set(AppLanguage.ES, Locale.forLanguageTag("es-CO"), "COP")
        // Resumen para gráficos: redondeo a miles con un decimal ("2,8 mil"), sin centavos jamás.
        assertEquals("$2,8 mil".replace(" ", "\u00A0"), Money.compact(275000))
        val c = Money.compact(1234567)
        assertEquals(true, c.startsWith("$") && c.contains("mil"))
    }
}
