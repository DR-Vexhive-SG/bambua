// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: tests de frontera por cada umbral de la tabla §3 (v0.3).
package com.bambua.app

import com.bambua.app.domain.SalditoMetrics
import com.bambua.app.domain.SalditoState
import com.bambua.app.domain.SalditoStateEvaluator
import com.bambua.app.domain.SalditoThresholds
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cada frontera de la tabla §3 con un test de ambos lados. Umbrales default del operador:
 * runway 3.0/1.0/0.5, banda ±5%, 4 semanas positivas, 3 semanas negativas, 2 meses de deuda.
 */
class SalditoStateEvaluatorTest {

    private val t = SalditoThresholds()

    private fun m(
        runway: Double = 2.0,
        weeklyBalance: Long = 0,
        weeklyIncome: Long = 100000,
        weeks: List<Long> = listOf(0, 0, 0, 0),
        debtMonths: Int = 0,
    ) = SalditoMetrics(
        runwayMonths = runway,
        weeklyBalance = weeklyBalance.takeIf { it != 0L } ?: (weeks.firstOrNull() ?: 0L),
        weeklyIncome = weeklyIncome,
        lastWeeksBalances = weeks,
        negativeStreakWeeks = weeks.takeWhile { it < 0 }.size,
        debtGrowingMonths = debtMonths,
    )

    /* -------- PODEROSO: runway ≥ 3.0 y balance > 0 las últimas 4 semanas -------- */
    @Test fun poderosoFronteraRunway() {
        val positivas = listOf(50000L, 40000L, 30000L, 20000L)
        assertEquals(SalditoState.PODEROSO, SalditoStateEvaluator.evaluate(m(runway = 3.0, weeks = positivas), t))
        // 2.9 meses: por la tabla §3 es SALUDABLE (balance > 0, runway ≥ 1), no PODEROSO.
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m(runway = 2.99, weeks = positivas), t))
    }

    @Test fun poderosoFronteraSemanasPositivas() {
        assertEquals(SalditoState.PODEROSO, SalditoStateEvaluator.evaluate(m(runway = 4.0, weeks = listOf(1L, 2L, 3L, 4L)), t))
        // La semana actual en 0 rompe el "las últimas 4 semanas > 0" y el balance >0: queda NEUTRAL (equilibrio).
        assertEquals(SalditoState.NEUTRAL, SalditoStateEvaluator.evaluate(m(runway = 4.0, weeks = listOf(0L, 2L, 3L, 4L)), t))
        // Solo 3 semanas positivas (la 4ª en 0): no PODEROSO.
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m(runway = 4.0, weeks = listOf(1L, 2L, 3L, 0L)), t))
    }

    /* -------- SALUDABLE: balance semanal > 0 y runway ≥ 1.0 -------- */
    @Test fun saludableFronteraRunway() {
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m(runway = 1.0, weeklyBalance = 1000), t))
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 0.99, weeklyBalance = 1000), t))
    }

    /* -------- NEUTRAL: balance entre −5% y +5% del ingreso semanal -------- */
    @Test fun neutralFronteraBandaInferior() {
        // −4% del ingreso semanal (100000) = −4000: dentro de la banda, runway ≥ 1 → NEUTRAL.
        assertEquals(SalditoState.NEUTRAL, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeklyBalance = -4000), t))
        assertEquals(SalditoState.NEUTRAL, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeklyBalance = -5000), t))
        // −6%: fuera de la banda con runway ≥ 1 → PREOCUPADO.
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeklyBalance = -6000), t))
    }

    @Test fun neutralFronteraBandaSuperior() {
        // Balance positivo dentro de la banda con runway sano: SALUDABLE (balance > 0 manda).
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeklyBalance = 5000), t))
        // El runway corto domina la banda: no alcanzar el mes es PREOCUPADO aunque la semana sea +5%.
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 0.9, weeklyBalance = 5000), t))
        // +6% con runway < 1: fuera de banda → PREOCUPADO.
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 0.9, weeklyBalance = 6000), t))
    }

    /* -------- PREOCUPADO: balance < 0 (1–2 semanas) o runway < 1.0 -------- */
    @Test fun preocupadoStreakUnaYDosSemanas() {
        // Balance semanal fuera de banda (−6% del ingreso) con runway sano: PREOCUPADO.
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeks = listOf(-6000L)), t))
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 2.0, weeks = listOf(-6000L, -6000L)), t))
    }

    /* -------- CRISIS: balance < 0 ≥ 3 semanas seguidas, o runway < 0.5, o deuda creciendo 2 meses -------- */
    @Test fun crisisFronteraStreak() {
        assertEquals(SalditoState.CRISIS, SalditoStateEvaluator.evaluate(m(runway = 5.0, weeks = listOf(-6000L, -6000L, -6000L)), t))
        // 2 semanas negativas ANTES y la actual positiva (weeks[0] = actual): SALUDABLE (balance > 0, runway sano).
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m(runway = 5.0, weeks = listOf(1L, -6000L, -6000L)), t))
    }

    @Test fun crisisFronteraRunway() {
        assertEquals(SalditoState.CRISIS, SalditoStateEvaluator.evaluate(m(runway = 0.49, weeklyBalance = 1000), t))
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 0.5, weeklyBalance = 1000), t))
    }

    @Test fun crisisFronteraDeuda() {
        assertEquals(SalditoState.CRISIS, SalditoStateEvaluator.evaluate(m(runway = 5.0, debtMonths = 2), t))
        // Deuda creciendo 1 mes no es crisis: con balance fuera de banda queda PREOCUPADO.
        assertEquals(SalditoState.PREOCUPADO, SalditoStateEvaluator.evaluate(m(runway = 5.0, weeklyBalance = -6000, debtMonths = 1), t))
    }

    /* -------- CRISIS gana sobre PODEROSO (el estado más grave resuelve primero) -------- */
    @Test fun crisisDominaSobrePoderoso() {
        val positivas = listOf(50000L, 40000L, 30000L, 20000L)
        assertEquals(
            SalditoState.CRISIS,
            SalditoStateEvaluator.evaluate(m(runway = 5.0, weeks = positivas, debtMonths = 2), t),
        )
    }

    /* -------- Umbrales editables: el mismo caso cambia con el umbral -------- */
    @Test fun umbralesEditablesCambianElEstado() {
        val positivas = listOf(50000L, 40000L, 30000L, 20000L)
        val m = m(runway = 2.5, weeks = positivas)
        assertEquals(SalditoState.SALUDABLE, SalditoStateEvaluator.evaluate(m, t))
        // El operador sube el umbral PODEROSO a 2.0: el mismo usuario pasa a poderoso.
        assertEquals(
            SalditoState.PODEROSO,
            SalditoStateEvaluator.evaluate(m, t.copy(runwayStrongMonths = 2.0)),
        )
    }
}
