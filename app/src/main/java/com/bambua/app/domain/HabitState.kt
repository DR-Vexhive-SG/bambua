// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: racha de registro diario (v0.4 B1 — contador simple,
// la gamificación completa es fase 4 del pre-registro y NO se construye aquí).
package com.bambua.app.domain

import com.bambua.app.data.db.Tx
import java.time.LocalDate

/**
 * Racha de registro diario: días consecutivos con ≥1 movimiento registrado.
 *
 * - Gracia (regla del operador): 1 día sin registro NO rompe la racha (se cuenta desde ayer).
 * - 2 días seguidos sin registro (hoy y ayer) la rompen.
 * - La persistencia es implícita: los días con registro viven en Room (transactions), el contador
 *   se deriva de ellos — no hay tabla nueva que se desincronice.
 */
object HabitStreak {
    fun streak(diasConRegistro: Set<LocalDate>, today: LocalDate): Int {
        if (diasConRegistro.isEmpty()) return 0
        var d = today
        if (d !in diasConRegistro) {
            // Gracia de 1 día: si hoy no registró, la racha sigue desde ayer.
            d = today.minusDays(1)
            if (d !in diasConRegistro) return 0
        }
        var racha = 0
        while (d in diasConRegistro) {
            racha++
            d = d.minusDays(1)
        }
        return racha
    }

    fun streakOf(txs: List<Tx>, today: LocalDate): Int = streak(txs.map { it.date }.toSet(), today)
}
