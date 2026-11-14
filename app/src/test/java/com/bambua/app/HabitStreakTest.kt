// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: tests de la racha de registro (v0.4 B1).
package com.bambua.app

import com.bambua.app.domain.HabitStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakTest {

    private val hoy = LocalDate.of(2026, 11, 14)

    @Test fun rachaCreceConDiasConsecutivos() {
        val dias = setOf(hoy, hoy.minusDays(1), hoy.minusDays(2), hoy.minusDays(3))
        assertEquals(4, HabitStreak.streak(dias, hoy))
    }

    @Test fun graciaUnDiaSinRegistroNoRompe() {
        // Hoy sin registrar, pero ayer y antes sí: la racha sigue (gracia de 1 día).
        val dias = setOf(hoy.minusDays(1), hoy.minusDays(2), hoy.minusDays(3))
        assertEquals(3, HabitStreak.streak(dias, hoy))
    }

    @Test fun dosDiasDeSilencioRompen() {
        // Hoy y ayer sin registrar: la racha se rompe (2 días de silencio).
        val dias = setOf(hoy.minusDays(2), hoy.minusDays(3), hoy.minusDays(4))
        assertEquals(0, HabitStreak.streak(dias, hoy))
    }

    @Test fun soloHoyCuentaUno() {
        assertEquals(1, HabitStreak.streak(setOf(hoy), hoy))
    }

    @Test fun sinRegistrosCero() {
        assertEquals(0, HabitStreak.streak(emptySet(), hoy))
    }

    @Test fun huecoIntermedioRompeLaContinuidad() {
        // Ayer sí, pero antier no: la racha cuenta solo lo consecutivo (2: hoy y ayer).
        val dias = setOf(hoy, hoy.minusDays(1), hoy.minusDays(3), hoy.minusDays(4))
        assertEquals(2, HabitStreak.streak(dias, hoy))
    }
}
