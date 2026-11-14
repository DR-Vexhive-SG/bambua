// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package com.bambua.app

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.bambua.app.data.AppSettings
import com.bambua.app.data.Backup
import com.bambua.app.data.BackupTarget
import com.bambua.app.data.FolderMode
import com.bambua.app.data.FoundBackup
import com.bambua.app.data.LocaleSettings
import com.bambua.app.data.Palette
import com.bambua.app.data.SampleData
import com.bambua.app.data.ThemeMode
import com.bambua.app.data.db.Account
import com.bambua.app.data.db.AccountType
import com.bambua.app.data.db.Bill
import com.bambua.app.data.db.CategoryKind
import com.bambua.app.data.db.CreditCard
import com.bambua.app.data.db.Debt
import com.bambua.app.data.db.DebtKind
import com.bambua.app.data.db.Goal
import com.bambua.app.data.db.Investment
import com.bambua.app.data.db.InvestmentKind
import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import com.bambua.app.domain.Ledger
import com.bambua.app.domain.ParsedEntry
import com.bambua.app.domain.PhraseParser
import com.bambua.app.domain.key
import com.bambua.app.domain.ym
import com.bambua.app.ui.BambuaRoot
import com.bambua.app.ui.LocalNav
import com.bambua.app.ui.Navigator
import com.bambua.app.ui.components.LocalContainer
import com.bambua.app.ui.components.LocalSettings
import com.bambua.app.ui.screens.AutoBackupScreen
import com.bambua.app.ui.screens.CardDetailScreen
import com.bambua.app.ui.screens.ExistingBackupContent
import com.bambua.app.ui.screens.DebtDetailScreen
import com.bambua.app.ui.screens.DebtEditorScreen
import com.bambua.app.ui.screens.DebtsScreen
import com.bambua.app.ui.screens.GoalsArchiveScreen
import com.bambua.app.ui.screens.InsightsScreen
import com.bambua.app.ui.screens.WalletArchiveScreen
import com.bambua.app.ui.screens.OnboardingScreen
import com.bambua.app.ui.screens.PlanScreen
import com.bambua.app.ui.screens.SettingsScreen
import com.bambua.app.ui.screens.TransactionsScreen
import com.bambua.app.ui.screens.WalletScreen
import com.bambua.app.ui.theme.BambuaTheme
import com.bambua.app.ui.tx.TxDraft
import com.bambua.app.ui.tx.TxEditorScreen
import com.bambua.app.ui.tx.TxForm
import com.bambua.app.ui.tx.parseContext
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.random.Random

/** Application das imagens de tela: sem lembretes nem backup, mas com a língua e a moeda do app (como o BambuaApp). */
class TestApp : Application() {
    private lateinit var systemBase: Context
    override fun attachBaseContext(base: Context) {
        systemBase = base
        super.attachBaseContext(LocaleSettings.wrap(base))
    }
    override fun onCreate() {
        super.onCreate()
        LocaleSettings.install(systemBase)
    }
}

/** Gera imagens das telas com dados de exemplo, para revisar o visual sem um celular. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class ScreensTest {
    private lateinit var c: AppContainer
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        c = AppContainer(ApplicationProvider.getApplicationContext())
        runBlocking {
            seed(c, today)
            c.autoBackup.clear()
        }
        val deadline = System.currentTimeMillis() + 10_000
        while ((c.repo.snapshot.value?.txs?.size ?: 0) < 50 && System.currentTimeMillis() < deadline) Thread.sleep(50)
    }

    private val settings = AppSettings(onboardingDone = true, defaultAccountId = 1)

    @Composable
    private fun Wrap(dark: Boolean = false, dynamic: Boolean = false, s: AppSettings = settings, content: @Composable () -> Unit) {
        BambuaTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = dynamic) {
            val nav = rememberNavController()
            val navigator = remember { Navigator(nav) }
            CompositionLocalProvider(LocalContainer provides c, LocalSettings provides s, LocalNav provides navigator) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    private fun shot(name: String, dark: Boolean = false, dynamic: Boolean = false, content: @Composable () -> Unit) =
        captureRoboImage("screenshots/$name.png") { Wrap(dark, dynamic) { content() } }

    @Test fun home() = shot("01_inicio") { BambuaRoot(null) }

    @Test fun homeDark() = shot("02_inicio_escuro", dark = true) { BambuaRoot(null) }

    @Test fun homeDynamic() = shot("03_inicio_material_you", dynamic = true) { BambuaRoot(null) }

    @Config(qualifiers = "+w393dp-h1600dp-xxhdpi")
    @Test fun homeLong() = shot("04_inicio_completo") { BambuaRoot(null) }

    @Test fun transactions() = shot("05_movimentos") { TransactionsScreen(null) }

    @Test fun budgets() = shot("06_orcamentos") { PlanScreen(0) }

    @Test fun bills() = shot("07_contas_fixas") { PlanScreen(1) }

    @Test fun goals() = shot("08_metas") { PlanScreen(2) }

    @Config(qualifiers = "+w393dp-h1500dp-xxhdpi")
    @Test fun wallet() = shot("09_carteira") { WalletScreen() }

    @Config(qualifiers = "+w393dp-h3000dp-xxhdpi")
    @Test fun insights() = shot("10_analises") { InsightsScreen() }

    @Config(qualifiers = "+w393dp-h1400dp-xxhdpi")
    @Test fun editor() = shot("11_novo_lancamento") { TxEditorScreen(null, TxType.EXPENSE, null, null) {} }

    @Config(qualifiers = "+w393dp-h1300dp-xxhdpi")
    @Test fun quickSheet() = shot("12_registro_rapido") {
        val l = Ledger(c.repo.snapshot.value!!, today)
        val p: ParsedEntry = PhraseParser(l.parseContext(settings)).parse("tv 1200 em 10x no cartão")
        val d = remember { TxDraft.fromParsed(p) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) { TxForm(d, l, compact = true) }
    }

    @Test fun card() = shot("13_cartao") { CardDetailScreen(1) }

    @Config(qualifiers = "+w393dp-h1900dp-xxhdpi")
    @Test fun settingsScreen() = shot("14_ajustes") { SettingsScreen() }

    @Test fun onboarding() = shot("15_boas_vindas") { OnboardingScreen() }

    @Config(qualifiers = "+w393dp-h2400dp-xxhdpi")
    @Test fun insightsDark() = shot("16_analises_escuro", dark = true) { InsightsScreen() }

    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun debts() = shot("17_dividas") { DebtsScreen() }

    @Config(qualifiers = "+w393dp-h1900dp-xxhdpi")
    @Test fun debtDetail() = shot("18_divida_detalhe") { DebtDetailScreen(1) }

    @Config(qualifiers = "+w393dp-h2600dp-xxhdpi")
    @Test fun debtEditor() = shot("19_divida_cadastro") { DebtEditorScreen(null) }

    @Config(qualifiers = "+w393dp-h1300dp-xxhdpi")
    @Test fun goalsArchive() = shot("22_metas_arquivadas") { GoalsArchiveScreen() }

    @Test fun walletArchive() = shot("23_carteira_arquivados") { WalletArchiveScreen() }

    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun debtsDark() = shot("24_dividas_escuro", dark = true) { DebtsScreen() }

    @Config(qualifiers = "+w393dp-h1600dp-xxhdpi")
    @Test fun billsWithDebts() = shot("25_contas_fixas_com_parcelas") { PlanScreen(1) }

    @Config(qualifiers = "+w393dp-h1400dp-xxhdpi")
    @Test fun debtDetailPaidOff() = shot("26_divida_quitada") { DebtDetailScreen(4) }

    @Config(qualifiers = "+w393dp-h1500dp-xxhdpi")
    @Test fun autoBackupEmpty() = shot("29_backup_automatico") { AutoBackupScreen() }

    @Config(qualifiers = "+w393dp-h1900dp-xxhdpi")
    @Test fun autoBackupConfigured() {
        val hour = 3_600_000L
        val now = System.currentTimeMillis()
        runBlocking {
            val b = c.autoBackup
            b.setTarget(BackupTarget.FOLDER, "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FBambua", "Documents/Bambua · no celular")
            b.recordWritten(BackupTarget.FOLDER, now - 26 * hour, "a", 120)
            b.recordUnchanged(BackupTarget.FOLDER, now - 2 * hour)
            b.setTarget(BackupTarget.CLOUD, "content://com.google.android.apps.docs.storage/document/x", "bambua-backup.json · Google Drive")
            b.recordWritten(BackupTarget.CLOUD, now - 50 * hour, "a", 120)
            b.recordFailure(BackupTarget.CLOUD, now - 2 * hour, "Não consegui gravar (sem espaço ou a nuvem não respondeu). Vou tentar de novo mais tarde.")
            b.setEnabled(true)
        }
        shot("30_backup_automatico_configurado") { AutoBackupScreen() }
    }

    @Config(qualifiers = "+w393dp-h1700dp-xxhdpi")
    @Test fun autoBackupDark() {
        runBlocking {
            c.autoBackup.setTarget(BackupTarget.FOLDER, "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FBambua", "Documents/Bambua · no celular")
            c.autoBackup.setFolderMode(FolderMode.REPLACE)
            c.autoBackup.recordWritten(BackupTarget.FOLDER, System.currentTimeMillis() - 3_600_000L, "a", 120)
            c.autoBackup.setEnabled(true)
        }
        shot("31_backup_automatico_escuro", dark = true) { AutoBackupScreen() }
    }

    @Config(qualifiers = "+w393dp-h900dp-xxhdpi")
    @Test fun existingBackupRestore() = shot("32_backup_encontrado_restaurar") {
        val snap = c.repo.snapshot.value!!
        val found = FoundBackup(Backup.Restored(snap, Backup.VERSION, System.currentTimeMillis() - 30 * 3_600_000L), currentTxs = 0, currentAccounts = 1)
        Box(Modifier.padding(24.dp)) { ExistingBackupContent(found, BackupTarget.CLOUD, overwrites = true, replaceMode = false, {}, {}, {}) }
    }

    @Config(qualifiers = "+w393dp-h900dp-xxhdpi")
    @Test fun existingBackupKeep() = shot("33_backup_encontrado_continuar", dark = true) {
        val snap = c.repo.snapshot.value!!
        val old = snap.copy(txs = snap.txs.take(40), accounts = snap.accounts.take(2))
        val found = FoundBackup(Backup.Restored(old, Backup.VERSION, System.currentTimeMillis() - 400 * 3_600_000L), currentTxs = snap.txs.size, currentAccounts = snap.accounts.size)
        Box(Modifier.padding(24.dp)) { ExistingBackupContent(found, BackupTarget.FOLDER, overwrites = false, replaceMode = false, {}, {}, {}) }
    }
}

/** Dados de exemplo realistas: 4 meses de vida financeira (os mesmos do modo de teste do app). */
suspend fun seed(c: AppContainer, today: LocalDate) = SampleData.fill(c.repo, today)
