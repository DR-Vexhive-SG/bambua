// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: export CSV formato Coach (interfaz oficial con el
// sistema Coach; el formato no puede variar un carácter — PROMPT-FORK §4 / AGENTS.md).
package com.bambua.app.domain

import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import java.time.LocalDate

/**
 * Export CSV en el formato EXACTO del Coach (serie-corriente.csv):
 *
 *   Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda
 *
 * Separador ";", UTF-8 sin BOM, fechas YYYY-MM-DD, importes con coma decimal y dos decimales
 * (siempre positivos: el signo lo define el Tipo). Las transferencias internas entre cuentas se
 * exportan como DOS filas con la misma fecha: Egreso en la cuenta origen e Ingreso en la destino.
 * Campos sin dato: vacíos (sin "null" ni "N/A"). Sin traducir: la cabecera y los tipos son fijos.
 */
object CoachCsv {
    const val HEADER = "Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda"

    fun csv(s: Snapshot, today: LocalDate = LocalDate.now()): String {
        val accounts = s.accounts.associateBy { it.id }
        val categories = s.categories.associateBy { it.id }
        val cards = s.cards.associateBy { it.id }
        val debts = s.debts.associateBy { it.id }
        val investments = s.investments.associateBy { it.id }
        val goals = s.goals.associateBy { it.id }

        fun esc(v: String): String =
            if (v.contains(';') || v.contains('"') || v.contains('\n') || v.contains('\r')) {
                "\"" + v.replace("\"", "\"\"") + "\""
            } else v

        // Centavos -> "68200,00". El importe siempre es positivo; el signo lo define el Tipo.
        fun money(c: Long): String {
            val a = kotlin.math.abs(c)
            return "${a / 100},${(a % 100).toString().padStart(2, '0')}"
        }

        fun fila(
            fecha: LocalDate, tipo: String, desc: String, cat: String?, cuenta: String?,
            destino: String?, tarjeta: String?, cuota: String, importe: Long, nota: String, deuda: String?,
        ): String = listOf(
            fecha.toString(), tipo, esc(desc), esc(cat ?: ""), esc(cuenta ?: ""), esc(destino ?: ""),
            esc(tarjeta ?: ""), cuota, money(importe), esc(nota), esc(deuda ?: ""),
        ).joinToString(";")

        val sb = StringBuilder(HEADER)
        for (t in s.txs.sortedWith(compareBy({ it.date }, { it.id }))) {
            val cuenta = t.accountId?.let { accounts[it]?.name }
            val destinoCuenta = t.toAccountId?.let { accounts[it]?.name }
            val cat = t.categoryId?.let { categories[it]?.name }
            val tarjeta = t.cardId?.let { cards[it]?.name }
            val deuda = t.debtId?.let { debts[it]?.name }
            val cuota = if (t.installmentTotal > 1) "${t.installmentNumber}/${t.installmentTotal}" else ""
            val (tipo, destino) = when (t.type) {
                TxType.INCOME, TxType.INVEST_OUT, TxType.GOAL_OUT, TxType.DEBT_IN -> "Ingreso" to when (t.type) {
                    TxType.INVEST_OUT -> t.investmentId?.let { investments[it]?.name }
                    TxType.GOAL_OUT -> t.goalId?.let { goals[it]?.name }
                    else -> null
                }
                TxType.TRANSFER -> {
                    // Dos filas, misma fecha: Egreso en la cuenta origen, Ingreso en la destino.
                    sb.append('\n').append(fila(t.date, "Egreso", t.description, cat, cuenta, destinoCuenta, null, cuota, t.amount, t.note, null))
                    sb.append('\n').append(fila(t.date, "Ingreso", t.description, cat, destinoCuenta, cuenta, null, cuota, t.amount, t.note, null))
                    continue
                }
                // EXPENSE, CARD_PAYMENT, INVEST_IN, GOAL_OUT... todo lo demás es salida.
                else -> "Egreso" to when (t.type) {
                    TxType.INVEST_IN -> t.investmentId?.let { investments[it]?.name }
                    TxType.GOAL_IN -> t.goalId?.let { goals[it]?.name }
                    else -> null
                }
            }
            sb.append('\n').append(fila(t.date, tipo, t.description, cat, cuenta, destino, tarjeta, cuota, t.amount, t.note, deuda))
        }
        sb.append('\n')
        return sb.toString()
    }
}
