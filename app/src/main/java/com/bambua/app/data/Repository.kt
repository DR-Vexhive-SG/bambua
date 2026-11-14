// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package com.bambua.app.data

import com.bambua.app.R
import com.bambua.app.domain.SalditoThresholds
import com.bambua.app.domain.Res
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.bambua.app.data.db.Account
import com.bambua.app.data.db.Bill
import com.bambua.app.data.db.Budget
import com.bambua.app.data.db.Category
import com.bambua.app.data.db.CategoryKind
import com.bambua.app.data.db.CreditCard
import com.bambua.app.data.db.BambuaDatabase
import com.bambua.app.data.db.Debt
import com.bambua.app.data.db.DebtAdjustment
import com.bambua.app.data.db.Goal
import com.bambua.app.data.db.Investment
import com.bambua.app.data.db.InvestmentYield
import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import com.bambua.app.domain.CardMath
import com.bambua.app.domain.Money
import com.bambua.app.domain.Snapshot
import com.bambua.app.domain.debtCategory
import com.bambua.app.domain.key
import com.bambua.app.domain.ym
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.roundToInt

/* ============================== Ajustes ============================== */

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Fondos seleccionables (spec v0.3 §1): Océano default, Selva, Orquídea, Carbón. */
enum class BackgroundTheme { OCEANO, SELVA, ORQUIDEA, CARBON }


data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val lockEnabled: Boolean = false,
    val hideValues: Boolean = false,
    val defaultAccountId: Long? = null,
    val onboardingDone: Boolean = false,
    val userName: String = "",
    val remindersEnabled: Boolean = true,
    /** Consentimiento de datos (Ley 1581), pedido en el onboarding. */
    val consentAccepted: Boolean = false,
    /** Tono colombiano: los textos pueden usar jerga cercana (default: cercano-neutral). */
    val colombianTone: Boolean = false,
    /** Fondo seleccionable (v0.3): Océano default. */
    val backgroundTheme: BackgroundTheme = BackgroundTheme.OCEANO,
    /** Celebración activa hasta este timestamp (meta cumplida, 24h). */
    val celebrateUntil: Long = 0,
    /** Umbrales de Saldito (v0.3, editables). */
    val salditoThresholds: SalditoThresholds = SalditoThresholds(),
)

private val Context.dataStore by preferencesDataStore("settings")

/** Ajustes do modo de teste (modo desenvolvedor): separados dos de verdade. */
private val Context.testDataStore by preferencesDataStore("settings_test")

class SettingsRepository(context: Context, sandbox: Boolean = false) {
    private val store = if (sandbox) context.testDataStore else context.dataStore

    private object K {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val lock = booleanPreferencesKey("lock")
        val hide = booleanPreferencesKey("hide")
        val defaultAccount = longPreferencesKey("default_account")
        val onboarding = booleanPreferencesKey("onboarding")
        val name = stringPreferencesKey("name")
        val reminders = booleanPreferencesKey("reminders")
        val consent = booleanPreferencesKey("consent")
        val tone = booleanPreferencesKey("colombian_tone")
        val background = stringPreferencesKey("background")
        val celebrate = longPreferencesKey("celebrate_until")
        val thRunwayStrong = stringPreferencesKey("th_runway_strong")
        val thRunwayHealthy = stringPreferencesKey("th_runway_healthy")
        val thRunwayCrisis = stringPreferencesKey("th_runway_crisis")
        val thNeutralBand = stringPreferencesKey("th_neutral_band")
        val thStrongWeeks = stringPreferencesKey("th_strong_weeks")
        val thCrisisWeeks = stringPreferencesKey("th_crisis_weeks")
        val thDebtMonths = stringPreferencesKey("th_debt_months")
    }

    val flow: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[K.dynamic] ?: false,
            lockEnabled = p[K.lock] ?: false,
            hideValues = p[K.hide] ?: false,
            defaultAccountId = p[K.defaultAccount],
            onboardingDone = p[K.onboarding] ?: false,
            userName = p[K.name] ?: "",
            remindersEnabled = p[K.reminders] ?: true,
            consentAccepted = p[K.consent] ?: false,
            colombianTone = p[K.tone] ?: false,
            backgroundTheme = p[K.background]?.let { runCatching { BackgroundTheme.valueOf(it) }.getOrNull() } ?: BackgroundTheme.OCEANO,
            celebrateUntil = p[K.celebrate] ?: 0L,
            salditoThresholds = SalditoThresholds(
                runwayStrongMonths = p[K.thRunwayStrong]?.toDoubleOrNull() ?: 3.0,
                runwayHealthyMonths = p[K.thRunwayHealthy]?.toDoubleOrNull() ?: 1.0,
                runwayCrisisMonths = p[K.thRunwayCrisis]?.toDoubleOrNull() ?: 0.5,
                neutralBandShare = p[K.thNeutralBand]?.toDoubleOrNull() ?: 0.05,
                strongPositiveWeeks = p[K.thStrongWeeks]?.toIntOrNull() ?: 4,
                crisisNegativeWeeks = p[K.thCrisisWeeks]?.toIntOrNull() ?: 3,
                crisisDebtMonths = p[K.thDebtMonths]?.toIntOrNull() ?: 2,
            ),
        )
    }

    suspend fun current(): AppSettings = flow.first()
    suspend fun setTheme(m: ThemeMode) = store.edit { it[K.theme] = m.name }
    suspend fun setDynamic(v: Boolean) = store.edit { it[K.dynamic] = v }
    suspend fun setLock(v: Boolean) = store.edit { it[K.lock] = v }
    suspend fun setHideValues(v: Boolean) = store.edit { it[K.hide] = v }
    suspend fun setOnboardingDone(v: Boolean) = store.edit { it[K.onboarding] = v }
    suspend fun setConsent(v: Boolean) = store.edit { it[K.consent] = v }
    suspend fun setColombianTone(v: Boolean) = store.edit { it[K.tone] = v }
    suspend fun setBackground(t: BackgroundTheme) = store.edit { it[K.background] = t.name }
    suspend fun setCelebrateUntil(ts: Long) = store.edit { it[K.celebrate] = ts }
    suspend fun setSalditoThresholds(s: SalditoThresholds) = store.edit {
        it[K.thRunwayStrong] = s.runwayStrongMonths.toString()
        it[K.thRunwayHealthy] = s.runwayHealthyMonths.toString()
        it[K.thRunwayCrisis] = s.runwayCrisisMonths.toString()
        it[K.thNeutralBand] = s.neutralBandShare.toString()
        it[K.thStrongWeeks] = s.strongPositiveWeeks.toString()
        it[K.thCrisisWeeks] = s.crisisNegativeWeeks.toString()
        it[K.thDebtMonths] = s.crisisDebtMonths.toString()
    }
    suspend fun setName(v: String) = store.edit { it[K.name] = v.trim() }
    suspend fun setReminders(v: Boolean) = store.edit { it[K.reminders] = v }
    /** Volta todos os ajustes ao padrão. */
    suspend fun clear() = store.edit { it.clear() }

    suspend fun setDefaultAccount(id: Long?) = store.edit {
        if (id == null) it.remove(K.defaultAccount) else it[K.defaultAccount] = id
    }
}

/* ============================ Repositório ============================ */

private data class Part1(
    val a: List<Account>, val c: List<CreditCard>, val cat: List<Category>, val t: List<Tx>, val i: List<Investment>,
)

private data class Part2(
    val y: List<InvestmentYield>, val g: List<Goal>, val b: List<Bill>, val bu: List<Budget>, val d: List<Debt>,
)

/** O que foi gravado numa ação, para o botão "Desfazer". */
data class Undo(val txs: List<Tx> = emptyList(), val adjustments: List<DebtAdjustment> = emptyList())

class FinanceRepository(private val db: BambuaDatabase, scope: CoroutineScope, private val onGoalAchieved: (suspend () -> Unit)? = null) {
    private val dao = db.dao()

    /** Fotografia sempre atualizada de todos os dados (qualquer gravação dispara uma nova). */
    val snapshot: StateFlow<Snapshot?> = combine(
        combine(dao.accounts(), dao.cards(), dao.categories(), dao.txs(), dao.investments(), ::Part1),
        combine(dao.yields(), dao.goals(), dao.bills(), dao.budgets(), dao.debts(), ::Part2),
        dao.debtAdjustments(),
    ) { p1, p2, adj -> Snapshot(p1.a, p1.c, p1.cat, p1.t, p1.i, p2.y, p2.g, p2.b, p2.bu, p2.d, adj) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Fotografia de todos os dados, lida de uma vez só (numa transação, para não pegar nada pela metade). */
    suspend fun loadOnce(): Snapshot = db.withTransaction {
        Snapshot(
            dao.accountsOnce(), dao.cardsOnce(), dao.categoriesOnce(), dao.txsOnce(), dao.investmentsOnce(),
            dao.yieldsOnce(), dao.goalsOnce(), dao.billsOnce(), dao.budgetsOnce(), dao.debtsOnce(), dao.debtAdjustmentsOnce(),
        )
    }

    /** Categorias padrão na primeira vez. Numa transação: nunca cria as categorias em dobro, mesmo junto com outra gravação. */
    suspend fun ensureSeeded() = db.withTransaction {
        if (dao.categoryCount() == 0) dao.putCategories(DefaultData.categories())
    }

    /* ---------------------------- lançamentos ---------------------------- */

    /** CELEBRA (v0.3): al cruzar el target de una meta, dispara la celebración de 24h. */
    suspend fun saveTx(tx: Tx): Long {
        val isGoalIn = tx.type == TxType.GOAL_IN && tx.goalId != null
        val savedBefore = if (isGoalIn) savedOf(tx.goalId!!) else 0L
        val id = dao.putTx(tx)
        if (isGoalIn) {
            val saved = savedOf(tx.goalId!!)
            val target = loadOnce().goals.find { it.id == tx.goalId }?.target
            if (target != null && savedBefore < target && saved >= target) onGoalAchieved?.invoke()
        }
        return id
    }

    private suspend fun savedOf(goalId: Long): Long =
        loadOnce().txs.filter { it.goalId == goalId && it.type == TxType.GOAL_IN }.sumOf { it.amount }

    /** Compra no cartão, opcionalmente parcelada: cria uma movimentação por parcela, cada uma na sua fatura. */
    suspend fun saveCardPurchase(base: Tx, card: CreditCard, installments: Int): Tx {
        val first = CardMath.invoiceFor(card, base.date)
        if (installments <= 1) {
            val t = base.copy(cardId = card.id, accountId = null, invoiceYm = first.key())
            return t.copy(id = dao.putTx(t))
        }
        val group = UUID.randomUUID().toString()
        val parts = CardMath.split(base.amount, installments)
        var firstTx: Tx? = null
        db.withTransaction {
            parts.forEachIndexed { i, v ->
                val t = base.copy(
                    id = 0, amount = v, date = base.date.plusMonths(i.toLong()), cardId = card.id, accountId = null,
                    invoiceYm = first.plusMonths(i.toLong()).key(), installmentGroup = group,
                    installmentNumber = i + 1, installmentTotal = installments,
                )
                val id = dao.putTx(t)
                if (i == 0) firstTx = t.copy(id = id)
            }
        }
        return firstTx!!
    }

    suspend fun deleteTx(tx: Tx, wholeGroup: Boolean = false) {
        if (wholeGroup && tx.installmentGroup != null) dao.deleteTxGroup(tx.installmentGroup) else dao.deleteTx(tx.id)
    }

    /** Várias gravações que entram juntas ou não entram (se algo falhar no meio, nada muda). */
    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)

    /** Grava as movimentações de um extrato importado, todas de uma vez. Devolve os ids (para desfazer). */
    suspend fun importTxs(txs: List<Tx>): List<Long> = db.withTransaction { txs.map { dao.putTx(it) } }

    /** Desfaz uma importação. */
    suspend fun undoImport(ids: List<Long>) = db.withTransaction { ids.chunked(500).forEach { dao.deleteTxs(it) } }

    /** Recoloca uma movimentação apagada (botão "Desfazer"). */
    suspend fun restore(txs: List<Tx>) = dao.putTxs(txs)

    suspend fun txById(id: Long): Tx? = dao.txById(id)

    /** Aviso de orçamento depois de registrar um gasto (80% e 100%). */
    suspend fun budgetAlert(categoryId: Long?, date: LocalDate, added: Long): String? {
        if (categoryId == null || added <= 0) return null
        val budget = dao.budgetsOnce().firstOrNull { it.categoryId == categoryId } ?: return null
        if (budget.limitAmount <= 0) return null
        val cat = dao.categoriesOnce().firstOrNull { it.id == categoryId } ?: return null
        val ym = date.ym()
        val spent = dao.categorySpent(categoryId, ym.atDay(1), ym.atEndOfMonth())
        val before = spent - added
        val ratio = spent.toDouble() / budget.limitAmount
        val beforeRatio = before.toDouble() / budget.limitAmount
        return when {
            beforeRatio < 1.0 && ratio >= 1.0 ->
                Res.s(R.string.repo_orcamento_estourado, cat.name, Money.format(spent), Money.format(budget.limitAmount))
            beforeRatio < 0.8 && ratio >= 0.8 ->
                Res.s(R.string.repo_atencao_voce_ja_usou, (ratio * 100).roundToInt(), cat.name)
            else -> null
        }
    }

    /* ------------------------------- contas ------------------------------ */

    suspend fun saveAccount(a: Account): Long = dao.putAccount(a)

    /** Só apaga se a conta não tiver movimentações (senão o histórico ficaria quebrado). */
    suspend fun deleteAccount(a: Account): Boolean {
        if (dao.countTxForAccount(a.id) > 0) return false
        dao.deleteAccount(a.id)
        return true
    }

    /* ------------------------------ cartões ------------------------------ */

    suspend fun saveCard(c: CreditCard): Long = dao.putCard(c)

    suspend fun deleteCard(c: CreditCard): Boolean {
        if (dao.countTxForCard(c.id) > 0) return false
        dao.deleteCard(c.id)
        return true
    }

    suspend fun payInvoice(card: CreditCard, ym: YearMonth, amount: Long, accountId: Long, date: LocalDate) {
        dao.putTx(
            Tx(
                type = TxType.CARD_PAYMENT, amount = amount, date = date, description = Res.s(R.string.repo_fatura, card.name),
                accountId = accountId, cardId = card.id, invoiceYm = ym.key(),
            ),
        )
    }

    /* ----------------------------- categorias ---------------------------- */

    suspend fun saveCategory(c: Category): Long = dao.putCategory(c)

    /** Se a categoria já foi usada, ela é arquivada (some das listas, mas o histórico continua certo). */
    suspend fun deleteCategory(c: Category): Boolean {
        dao.deleteBudget(c.id)
        return if (dao.countTxForCategory(c.id) > 0) {
            dao.putCategory(c.copy(archived = true)); false
        } else {
            dao.deleteCategory(c.id); true
        }
    }

    /* --------------------------- investimentos --------------------------- */

    suspend fun saveInvestment(i: Investment): Long = dao.putInvestment(i)

    suspend fun deleteInvestment(i: Investment) = db.withTransaction {
        dao.deleteTxsOfInvestment(i.id); dao.deleteYieldsOf(i.id); dao.deleteInvestment(i.id)
    }

    /** Atualiza o valor atual: a diferença vira um registro de rendimento (ou perda). */
    suspend fun updateInvestmentValue(i: Investment, currentValue: Long, newValue: Long, date: LocalDate) {
        val diff = newValue - currentValue
        if (diff != 0L) dao.putYield(InvestmentYield(investmentId = i.id, date = date, amount = diff))
    }

    suspend fun deleteYield(y: InvestmentYield) = dao.deleteYield(y.id)

    /* -------------------------------- metas ------------------------------ */

    suspend fun saveGoal(g: Goal): Long = dao.putGoal(g)

    suspend fun deleteGoal(g: Goal) = db.withTransaction { dao.deleteTxsOfGoal(g.id); dao.deleteGoal(g.id) }

    /* ----------------------------- contas fixas -------------------------- */

    suspend fun saveBill(b: Bill): Long = dao.putBill(b)

    suspend fun deleteBill(b: Bill) = db.withTransaction { dao.unlinkBill(b.id); dao.deleteBill(b.id) }

    suspend fun markBillPaid(
        bill: Bill, ym: YearMonth, amount: Long, date: LocalDate, accountId: Long?, card: CreditCard?,
    ): Long {
        val tx = Tx(
            type = if (bill.kind == CategoryKind.EXPENSE) TxType.EXPENSE else TxType.INCOME,
            amount = amount, date = date, description = bill.name, categoryId = bill.categoryId,
            accountId = if (card == null) accountId else null, cardId = card?.id,
            invoiceYm = card?.let { CardMath.invoiceFor(it, date).key() },
            billId = bill.id, billYm = ym.key(),
        )
        return dao.putTx(tx)
    }

    suspend fun unmarkBillPaid(bill: Bill, ym: YearMonth) = dao.deleteBillPayment(bill.id, ym.key())

    /* ----------------------------- orçamentos ---------------------------- */

    suspend fun saveBudget(categoryId: Long, limit: Long) = dao.putBudget(Budget(categoryId, limit))
    suspend fun deleteBudget(categoryId: Long) = dao.deleteBudget(categoryId)

    /* ------------------------------- dívidas ------------------------------ */

    suspend fun saveDebt(d: Debt): Long = dao.putDebt(d)

    /** Cadastra a dívida. Se o dinheiro do empréstimo entrou numa conta agora, registra essa entrada. */
    suspend fun createDebt(d: Debt, received: Long, accountId: Long?, date: LocalDate): Long = db.withTransaction {
        val id = dao.putDebt(d)
        if (received > 0 && accountId != null) {
            dao.putTx(
                Tx(
                    type = TxType.DEBT_IN, amount = received, date = date, description = Res.s(R.string.phrase_emprestimo, d.name),
                    accountId = accountId, debtId = id,
                ),
            )
        }
        id
    }

    /** Apaga a dívida e os ajustes dela. Os pagamentos continuam no extrato (o dinheiro saiu de verdade). */
    suspend fun deleteDebt(d: Debt) = db.withTransaction {
        dao.unlinkDebt(d.id); dao.deleteAdjustmentsOf(d.id); dao.deleteDebt(d.id)
    }

    suspend fun debtCategoryId(): Long? = dao.categoriesOnce().debtCategory()?.id

    suspend fun ensureDebtCategory() {
        if (dao.categoriesOnce().debtCategory() != null) return
        val existing = dao.categoriesOnce()
        val cat = DefaultData.debtCategory()
        dao.putCategories(existing.filter { it.kind == CategoryKind.EXPENSE && it.sortOrder >= cat.sortOrder }.map { it.copy(sortOrder = it.sortOrder + 1) })
        dao.putCategory(cat)
    }

    /**
     * Pagamento de dívida: vira um gasto normal (categoria "Dívidas e empréstimos") ligado à dívida.
     * [extra] é juros ou multa pagos junto: sai da conta, mas não diminui a dívida.
     * [discount] é desconto ao quitar: diminui a dívida sem sair dinheiro.
     */
    suspend fun payDebt(
        debt: Debt, amount: Long, accountId: Long, date: LocalDate, installment: Boolean,
        extra: Long = 0, discount: Long = 0,
    ): Undo {
        val cat = debtCategoryId()
        val txs = ArrayList<Tx>()
        val adjs = ArrayList<DebtAdjustment>()
        db.withTransaction {
            if (amount > 0) {
                val t = Tx(
                    type = TxType.EXPENSE, amount = amount, date = date,
                    description = (if (installment) Res.s(R.string.repo_parcela) else Res.s(R.string.repo_pagamento)) + debt.name,
                    categoryId = cat, accountId = accountId, debtId = debt.id,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (extra > 0) {
                val t = Tx(
                    type = TxType.EXPENSE, amount = extra, date = date, description = Res.s(R.string.repo_juros_multa, debt.name),
                    categoryId = cat, accountId = accountId,
                )
                txs += t.copy(id = dao.putTx(t))
            }
            if (discount > 0) {
                val a = DebtAdjustment(debtId = debt.id, date = date, amount = -discount, note = Res.s(R.string.repo_desconto_quitacao))
                adjs += a.copy(id = dao.putDebtAdjustment(a))
            }
        }
        return Undo(txs, adjs)
    }

    /** Pegou mais dinheiro emprestado: entra na conta e aumenta a dívida. */
    suspend fun borrowMore(debt: Debt, amount: Long, accountId: Long, date: LocalDate): Undo {
        val t = Tx(
            type = TxType.DEBT_IN, amount = amount, date = date, description = Res.s(R.string.phrase_emprestimo, debt.name),
            accountId = accountId, debtId = debt.id,
        )
        return Undo(txs = listOf(t.copy(id = dao.putTx(t))))
    }

    /** Corrige quanto falta pagar: a diferença vira juros/encargos (+) ou desconto (−). */
    suspend fun adjustDebt(debt: Debt, diff: Long, date: LocalDate, note: String): Undo {
        if (diff == 0L) return Undo()
        val a = DebtAdjustment(debtId = debt.id, date = date, amount = diff, note = note)
        return Undo(adjustments = listOf(a.copy(id = dao.putDebtAdjustment(a))))
    }

    suspend fun deleteDebtAdjustment(a: DebtAdjustment) = dao.deleteDebtAdjustment(a.id)

    suspend fun undo(u: Undo) = db.withTransaction {
        u.txs.forEach { dao.deleteTx(it.id) }
        u.adjustments.forEach { dao.deleteDebtAdjustment(it.id) }
    }

    /* ------------------------------- backup ------------------------------ */

    /** [fromOldVersion]: backup feito antes da versão 1.1 (sem dívidas): ganha a categoria de dívidas. */
    suspend fun replaceAll(s: Snapshot, fromOldVersion: Boolean = false) = db.withTransaction {
        clearTables()
        dao.putAccounts(s.accounts); dao.putCards(s.cards); dao.putCategories(s.categories); dao.putTxs(s.txs)
        dao.putInvestments(s.investments); dao.putYields(s.yields); dao.putGoals(s.goals); dao.putBills(s.bills)
        dao.putBudgets(s.budgets); dao.putDebts(s.debts); dao.putDebtAdjustments(s.debtAdjustments)
        if (fromOldVersion) ensureDebtCategory()
    }

    suspend fun eraseAll() = db.withTransaction {
        clearTables()
        dao.putCategories(DefaultData.categories())
    }

    private suspend fun clearTables() {
        dao.clearTxs(); dao.clearAccounts(); dao.clearCards(); dao.clearCategories(); dao.clearInvestments()
        dao.clearYields(); dao.clearGoals(); dao.clearBills(); dao.clearBudgets(); dao.clearDebts()
        dao.clearDebtAdjustments()
    }
}
