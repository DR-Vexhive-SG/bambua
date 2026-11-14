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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambua.app.R
import com.bambua.app.domain.Res
import com.bambua.app.ui.LocalNav
import com.bambua.app.ui.components.BambuaTopBar

/**
 * Saldito v0.1: placeholder de UI (la puerta). Cero lógica, cero red: el motor de IA vive en un
 * servicio propietario separado y NO se construye en este repo (PROMPT-FORK §5).
 */
@Composable
fun SalditoScreen() {
    val nav = LocalNav.current
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
                painterResource(R.drawable.ic_saldito_normal), null,
                Modifier.size(180.dp).padding(top = 8.dp),
            )
            Text(Res.s(R.string.saldito_titulo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            Text(
                Res.s(R.string.saldito_proximamente),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
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
