// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: placeholder de Saldito (la puerta, PROMPT-FORK §3 Paso 4).
package com.bambua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambua.app.R
import com.bambua.app.domain.Res
import com.bambua.app.domain.SalditoState
import com.bambua.app.domain.SalditoStateEvaluator
import com.bambua.app.domain.SalditoMetricsCalculator
import com.bambua.app.ui.LocalNav
import com.bambua.app.ui.components.BambuaTopBar
import com.bambua.app.ui.components.rememberLedger

/**
 * Saldito v0.1: placeholder de UI (la puerta). Cero lógica, cero red: el motor de IA vive en un
 * servicio propietario separado y NO se construye en este repo (PROMPT-FORK §5).
 */
@Composable
fun SalditoScreen() {
    val nav = LocalNav.current
    val c = com.bambua.app.ui.components.LocalContainer.current
    Scaffold(
        topBar = { BambuaTopBar(Res.s(R.string.saldito_titulo), onBack = nav::back) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // El personaje Saldito v0.2 (asset del Coach) con su estado normal.
            Image(
                painterResource(R.drawable.saldito_furry_neutral), null,
                Modifier.size(180.dp).padding(top = 8.dp),
            )
            Text(Res.s(R.string.saldito_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text(
                Res.s(R.string.saldito_proximamente),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            // v0.3: el estado semanal evaluado (el placeholder siempre dibuja el personaje normal).
            val settings = com.bambua.app.ui.components.LocalSettings.current
            val l = rememberLedger()
            val state = remember(l, settings.celebrateUntil) {
                when {
                    settings.celebrateUntil > System.currentTimeMillis() -> SalditoState.CELEBRA
                    l != null -> SalditoStateEvaluator.evaluate(
                        SalditoMetricsCalculator.of(l.s, l.today), settings.salditoThresholds,
                    )
                    else -> SalditoState.NEUTRAL
                }
            }
            if (state != SalditoState.CELEBRA) {
                Text(
                    Res.s(R.string.saldito_estado, Res.s(estadoName(state))),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // Regla de dignidad (pre-registro): en CRISIS, la línea acompaña — jamás humillación.
            if (state == SalditoState.CRISIS) {
                Text(
                    Res.s(R.string.saldito_dignidad),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                Res.s(R.string.saldito_concepto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Nombre localizado del estado para la UI. */
fun estadoName(s: com.bambua.app.domain.SalditoState): Int = when (s) {
    com.bambua.app.domain.SalditoState.PODEROSO -> R.string.estado_poderoso
    com.bambua.app.domain.SalditoState.SALUDABLE -> R.string.estado_saludable
    SalditoState.NEUTRAL -> R.string.estado_neutral
    com.bambua.app.domain.SalditoState.PREOCUPADO -> R.string.estado_preocupado
    com.bambua.app.domain.SalditoState.CRISIS -> R.string.estado_crisis
    SalditoState.CELEBRA -> R.string.estado_celebra
}
