// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
// Modificaciones © 2026 operador Bambua: fixture del export CSV formato Coach (PROMPT-FORK §4).
package com.bambua.app

import com.bambua.app.data.db.Account
import com.bambua.app.data.db.AccountType
import com.bambua.app.data.db.Category
import com.bambua.app.data.db.CategoryKind
import com.bambua.app.data.db.CreditCard
import com.bambua.app.data.db.Debt
import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import com.bambua.app.domain.CoachCsv
import com.bambua.app.domain.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CoachCsvTest {

    private val d = LocalDate.of(2026, 11, 14)

    private val snap = Snapshot(
        accounts = listOf(
            Account(id = 1, name = "Efectivo", type = AccountType.CASH, color = 1),
            Account(id = 2, name = "Banco", type = AccountType.CHECKING, color = 2),
        ),
        categories = listOf(
            Category(id = 10, name = "Transporte", kind = CategoryKind.EXPENSE, icon = "star", color = 3),
            Category(id = 11, name = "Salario", kind = CategoryKind.INCOME, icon = "star", color = 4),
        ),
        cards = listOf(CreditCard(id = 20, name = "Visa", limitAmount = 100000, closingDay = 3, dueDay = 10, color = 5)),
        txs = listOf(
            // Cabecera del §4: ingreso y egreso de referencia, byte a byte.
            Tx(id = 100, type = TxType.INCOME, amount = 6820000, date = d, description = "Uber", categoryId = null, accountId = 1),
            Tx(id = 101, type = TxType.EXPENSE, amount = 2300000, date = d, description = "Tanqueo", categoryId = 10, accountId = 1),
            // Transferencia: dos filas (Egreso origen + Ingreso destino), misma fecha.
            Tx(id = 102, type = TxType.TRANSFER, amount = 1500000, date = d, description = "Recarga", accountId = 1, toAccountId = 2),
            // Cuotas y tarjeta: cuota "2/10", tarjeta en la columna Tarjeta.
            Tx(id = 103, type = TxType.EXPENSE, amount = 5000000, date = d, description = "TV", accountId = 2, cardId = 20, installmentNumber = 2, installmentTotal = 10),
            // Nota y decimales exactos.
            Tx(id = 104, type = TxType.EXPENSE, amount = 12345, date = d, description = "Café", accountId = 2, note = "con parce"),
        ),
    )

    @Test
    fun cabeceraExacta() {
        assertEquals("Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda", CoachCsv.HEADER)
        assertEquals(CoachCsv.HEADER, CoachCsv.csv(Snapshot(), d).trimEnd('\n'))
    }

    @Test
    fun formatoByteAByte() {
        val esperado = """
            Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda
            2026-11-14;Ingreso;Uber;;Efectivo;;;;68200,00;;
            2026-11-14;Egreso;Tanqueo;Transporte;Efectivo;;;;23000,00;;
            2026-11-14;Egreso;Recarga;;Efectivo;Banco;;;15000,00;;
            2026-11-14;Ingreso;Recarga;;Banco;Efectivo;;;15000,00;;
            2026-11-14;Egreso;TV;;Banco;;Visa;2/10;50000,00;;
            2026-11-14;Egreso;Café;;Banco;;;;123,45;con parce;
        """.trimIndent() + "\n"
        assertEquals(esperado, CoachCsv.csv(snap, d))
    }

    @Test
    fun sinBomYConNewlineFinal() {
        val csv = CoachCsv.csv(snap, d)
        assertEquals('F', csv[0])
        assertEquals('\n', csv.last())
        assertEquals(-1, csv.indexOf('\r'))
        assertEquals(-1, csv.indexOf("null"))
    }

    @Test
    fun camposConPuntoYComaEscapados() {
        val s = Snapshot(txs = listOf(Tx(id = 1, type = TxType.EXPENSE, amount = 1000, date = d, description = "Almuerzo; luego; \"algo\"", accountId = 1)))
        val linea = CoachCsv.csv(s, d).lines()[1]
        val esperado = "2026-11-14;Egreso;\"Almuerzo; luego; \"\"algo\"\"\";;;;;;10,00;;"
        assertEquals(esperado, linea)
    }
}
