// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: estados de Saldito ligados a la salud financiera
// (PROMPT-TEMAS v0.3, tabla §3 de art/temas-fondos.md — árbitro de diseño).
package com.bambua.app.domain

import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Estados de salud financiera. CELEBRA es un EVENTO (meta cumplida, 24h): el evaluador nunca lo
 * devuelve; la UI lo muestra mientras la celebración está activa.
 */
enum class SalditoState { PODEROSO, SALUDABLE, NEUTRAL, PREOCUPADO, CRISIS, CELEBRA }

/**
 * Umbrales de la tabla §3 — todos editables en Ajustes › avanzados y persistidos en DataStore.
 * Los defaults son los valores [D] del operador.
 */
data class SalditoThresholds(
    /** PODEROSO: runway en meses a partir del cual el estado es poderoso. */
    val runwayStrongMonths: Double = 3.0,
    /** SALUDABLE: runway mínimo para el estado saludable (con balance > 0). */
    val runwayHealthyMonths: Double = 1.0,
    /** CRISIS: runway por debajo del cual es crisis inmediata. */
    val runwayCrisisMonths: Double = 0.5,
    /** NEUTRAL: banda ±[x] del ingreso semanal considerada equilibrio (0.05 = 5%). */
    val neutralBandShare: Double = 0.05,
    /** PODEROSO: semanas con balance > 0 requeridas (la actual incluida). */
    val strongPositiveWeeks: Int = 4,
    /** CRISIS: semanas seguidas con balance < 0. */
    val crisisNegativeWeeks: Int = 3,
    /** CRISIS: meses seguidos con deuda creciendo (ajustes positivos). */
    val crisisDebtMonths: Int = 2,
)

/** Métricas semanales que alimentan el evaluador (calculadas del Snapshot, sin Android). */
data class SalditoMetrics(
    /** Runway: meses que el saldo cubre al ritmo de gasto mensual actual. */
    val runwayMonths: Double,
    /** Balance de la semana en curso (ingresos − saldos de los últimos 7 días), en centavos. */
    val weeklyBalance: Long,
    /** Ingreso de la semana en curso (base de la banda neutral), en centavos. */
    val weeklyIncome: Long,
    /** Balance de cada una de las últimas semanas, de la más reciente hacia atrás. */
    val lastWeeksBalances: List<Long>,
    /** Semanas seguidas (empezando en la actual) con balance < 0. */
    val negativeStreakWeeks: Int,
    /** Meses seguidos con deuda creciendo (ajustes positivos de deuda). */
    val debtGrowingMonths: Int,
)

object SalditoStateEvaluator {
    /**
     * Evaluación según la tabla §3. Orden de resolución (de más grave a mejor):
     * CRISIS → PODEROSO → SALUDABLE → NEUTRAL → PREOCUPADO. La tabla manda como árbitro;
     * un valor que cumple varias filas toma la más específica de este orden.
     */
    fun evaluate(m: SalditoMetrics, t: SalditoThresholds): SalditoState {
        if (m.runwayMonths < t.runwayCrisisMonths) return SalditoState.CRISIS
        if (m.negativeStreakWeeks >= t.crisisNegativeWeeks) return SalditoState.CRISIS
        if (m.debtGrowingMonths >= t.crisisDebtMonths) return SalditoState.CRISIS

        if (m.runwayMonths >= t.runwayStrongMonths &&
            m.lastWeeksBalances.take(t.strongPositiveWeeks).size >= t.strongPositiveWeeks &&
            m.lastWeeksBalances.take(t.strongPositiveWeeks).all { it > 0 }
        ) return SalditoState.PODEROSO

        if (m.weeklyBalance > 0 && m.runwayMonths >= t.runwayHealthyMonths) return SalditoState.SALUDABLE

        // El runway corto domina la banda neutral: no alcanzar el mes es señal grave (tabla §3).
        if (m.runwayMonths < t.runwayHealthyMonths) return SalditoState.PREOCUPADO

        if (m.weeklyIncome > 0 && kotlin.math.abs(m.weeklyBalance) <= (t.neutralBandShare * m.weeklyIncome).toLong()) {
            return SalditoState.NEUTRAL
        }

        // Residual: balance < 0 fuera de la banda con runway sano.
        return SalditoState.PREOCUPADO
    }
}

/** Cálculo de las métricas desde el Snapshot (la semana empieza el lunes, costumbre colombiana). */
object SalditoMetricsCalculator {
    private fun weekStart(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)

    private fun balanceOfWindow(txs: List<Tx>, from: LocalDate, until: LocalDate): Long =
        txs.filter { !it.date.isBefore(from) && it.date.isBefore(until) && it.type in EXPENSE_LIKE + INCOME_LIKE }
            .sumOf { if (it.type in INCOME_LIKE) it.amount else -it.amount }

    private val INCOME_LIKE = setOf(TxType.INCOME, TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN)
    private val EXPENSE_LIKE = setOf(TxType.EXPENSE, TxType.CARD_PAYMENT, TxType.INVEST_IN, TxType.GOAL_IN)

    fun of(s: Snapshot, today: LocalDate, monthlyExpenseCents: Long? = null): SalditoMetrics {
        val moneyTxs = s.txs
        val thisWeekStart = weekStart(today)

        val weeklyIncome = moneyTxs.filter { !it.date.isBefore(thisWeekStart) && it.type in INCOME_LIKE }.sumOf { it.amount }
        val weeklyBalance = balanceOfWindow(moneyTxs, thisWeekStart, today.plusDays(1))

        // 4 semanas: la actual y las 3 anteriores (lunes a domingo).
        val lastWeeks = (0..3).map { i ->
            val start = thisWeekStart.minusWeeks(i.toLong())
            val end = start.plusDays(7)
            balanceOfWindow(moneyTxs, start, end)
        }
        var streak = 0
        for (b in lastWeeks) {
            if (b < 0) streak++ else break
        }

        // Runway: saldo total de cuentas contra el gasto mensual (promedio de los últimos 30 días si no viene dado).
        val totalBalance = s.accounts.filter { !it.archived && it.includeInTotal }.sumOf { it.initialBalance }
        val expenseWindow = moneyTxs.filter {
            !it.date.isBefore(today.minusDays(30)) && it.type in EXPENSE_LIKE
        }.sumOf { it.amount }
        val monthly = monthlyExpenseCents ?: expenseWindow
        val runway = when {
            monthly <= 0 -> if (totalBalance > 0) 999.0 else 0.0
            else -> totalBalance / monthly.toDouble()
        }

        // Deuda creciendo: meses con ajustes de deuda netos positivos (intereses/encargos añadidos).
        val debtGrowing = (0 until 24).count { i ->
            val ym = today.minusMonths(i.toLong()).let { it.year * 100 + it.monthValue }
            s.debtAdjustments.filter { it.date.year * 100 + it.date.monthValue == ym && it.amount > 0 }
                .sumOf { it.amount } > 0
        }

        return SalditoMetrics(
            runwayMonths = runway,
            weeklyBalance = weeklyBalance,
            weeklyIncome = weeklyIncome,
            lastWeeksBalances = lastWeeks,
            negativeStreakWeeks = streak,
            debtGrowingMonths = debtGrowing,
        )
    }
}
