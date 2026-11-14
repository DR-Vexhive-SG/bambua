// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Hanry Franco
package com.bambua.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.hasScrollAction
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.bambua.app.data.AppSettings
import com.bambua.app.data.Palette
import com.bambua.app.data.ThemeMode
import com.bambua.app.data.db.Account
import com.bambua.app.data.db.AccountType
import com.bambua.app.data.db.CreditCard
import com.bambua.app.data.db.Tx
import com.bambua.app.data.db.TxType
import com.bambua.app.importer.ImportPlanner
import com.bambua.app.importer.PdfText
import com.bambua.app.importer.RowFlag
import com.bambua.app.importer.SampleFiles
import com.bambua.app.ui.LocalNav
import com.bambua.app.ui.Navigator
import com.bambua.app.ui.components.LocalContainer
import com.bambua.app.ui.components.LocalSettings
import com.bambua.app.ui.screens.ImportModel
import com.bambua.app.ui.screens.ImportScreen
import com.bambua.app.ui.theme.BambuaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.time.LocalDate

/** A importação de extratos usada como uma pessoa usaria: escolher o arquivo, conferir, ajustar e importar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class, qualifiers = "+w393dp-h852dp-xxhdpi")
class ImportFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var c: AppContainer
    private lateinit var vm: ImportModel
    private lateinit var dir: File
    private val today = LocalDate.of(2026, 10, 3)
    private var nubank = 0L
    private var roxo = 0L
    private val settings = AppSettings(onboardingDone = true, dynamicColor = false)

    /** Faz o papel do seletor de arquivos do Android. */
    private var answer: Uri? = null
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            val uri = if (contract is ActivityResultContracts.OpenDocument) answer else null
            dispatchResult(requestCode, if (uri != null) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().setData(uri))
        }
    }

    @Before fun setUp() {
        // Os extratos de exemplo só entram no modo de teste: estes testes rodam nele (banco de dados de teste).
        c = AppContainer(ApplicationProvider.getApplicationContext(), sandbox = true)
        runBlocking {
            c.repo.ensureSeeded()
            nubank = c.repo.saveAccount(Account(name = "Nubank", type = AccountType.CHECKING, initialBalance = 123456, color = Palette.pick(2)))
            c.repo.saveAccount(Account(name = "Carteira", type = AccountType.CASH, color = Palette.pick(9), sortOrder = 1))
            roxo = c.repo.saveCard(CreditCard(name = "Roxo", limitAmount = 500000, closingDay = 3, dueDay = 10, color = Palette.pick(2), payAccountId = nubank))
            // Lançada à mão um dia depois da compra do extrato: deve aparecer como "parece repetida".
            c.repo.saveTx(Tx(type = TxType.EXPENSE, amount = 1850, date = today.minusDays(24), description = "Padaria", accountId = nubank))
            c.settings.setDefaultAccount(nubank)
        }
        dir = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "import-${System.nanoTime()}")
        TestDocsProvider.install(dir)
        vm = ImportModel(c, today = { today }, pdfPages = { _, pw ->
            if (pw != "1234") throw PdfText.PasswordNeeded()
            listOf("Extrato da conta\n01/10/2026 PADARIA CENTRAL -12,50\n02/10/2026 PIX RECEBIDO ANA 50,00")
        })
        waitFor(10_000) { c.repo.snapshot.value?.accounts?.size == 2 }
    }

    private fun file(s: SampleFiles.Sample): Uri {
        File(dir, s.name).writeBytes(s.bytes)
        return DocumentsContract.buildDocumentUri(TestDocsProvider.AUTHORITY, "${TestDocsProvider.ROOT}/${s.name}")
    }

    private fun show() {
        rule.setContent {
            BambuaTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                val nav = rememberNavController()
                val navigator = remember { Navigator(nav) }
                val owner = remember { object : ActivityResultRegistryOwner { override val activityResultRegistry = registry } }
                CompositionLocalProvider(
                    LocalContainer provides c, LocalSettings provides settings, LocalNav provides navigator,
                    LocalActivityResultRegistryOwner provides owner,
                ) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { ImportScreen(model = vm) }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun pick(s: SampleFiles.Sample) {
        answer = file(s)
        rule.onNodeWithText("Escolher arquivo").performClick()
        waitFor(10_000) { vm.step == ImportModel.Step.Review }
        rule.waitForIdle()
    }

    /** Espera algo acontecer, deixando a tela (e o que o ViewModel manda para ela) andar enquanto isso. */
    private fun waitFor(ms: Long, condition: () -> Boolean) = rule.waitUntil(ms) {
        ShadowLooper.idleMainLooper()
        condition()
    }

    private fun imported() = runBlocking { c.repo.loadOnce().txs.filter { it.importKey != null } }

    private fun exists(text: String) = rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test fun ofxDaContaConfereImportaEReconheceNaSegundaVez() {
        show()
        captureScreenRoboImage("screenshots/37_importar_inicio.png")
        pick(SampleFiles.ofx(today))

        val rows = vm.rows
        assertEquals(13, rows.size)
        assertEquals(nubank, vm.target?.accountId)
        val padaria = rows.single { it.line.description.startsWith("Padaria") }
        assertEquals(RowFlag.LOOKS_DUPLICATE, padaria.flag)
        assertEquals(RowFlag.INVOICE_PAYMENT, rows.single { it.line.description.startsWith("Pagamento") }.flag)
        assertEquals(11, vm.selectedCount)
        assertTrue(exists("Ficaram desmarcadas: 1 parece repetida, 1 pagamento de fatura"))
        captureScreenRoboImage("screenshots/38_importar_conferencia.png")

        rule.onNodeWithText("Importar 11 movimentações").performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Done }
        val txs = imported()
        assertEquals(11, txs.size)
        assertTrue(txs.all { it.accountId == nubank && it.cardId == null })
        assertEquals(420000L, txs.single { it.description.startsWith("Salário") }.amount)
        assertTrue(exists("11 movimentações importadas"))
        assertTrue(exists("Saldo no fim do extrato"))
        captureScreenRoboImage("screenshots/43_importar_pronto.png")

        // O mesmo arquivo de novo: nada entra em dobro.
        rule.onNodeWithText("Importar outro arquivo").performClick()
        pick(SampleFiles.ofx(today))
        assertEquals(11, vm.rows.count { it.flag == RowFlag.IMPORTED_BEFORE })
        assertEquals(0, vm.selectedCount)
        assertTrue(exists("Marque o que quer importar"))
    }

    @Test fun desfazerTiraTudoQueEntrou() {
        show()
        pick(SampleFiles.csvAccount(today))
        assertEquals(13, vm.rows.size)
        rule.onNodeWithText("Importar", substring = true).performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Done }
        assertTrue(imported().isNotEmpty())
        rule.onNodeWithText("Desfazer importação").performScrollTo().performClick()
        waitFor(10_000) { (vm.step as? ImportModel.Step.Done)?.undone == true }
        assertTrue(imported().isEmpty())
        assertTrue(exists("Importação desfeita"))
    }

    @Test fun faturaDoCartaoViraOsSinaisESeparaPagamentoDeEstorno() {
        show()
        pick(SampleFiles.csvCard(today))
        val t = vm.target!!
        assertEquals(roxo, t.cardId)
        assertTrue(vm.autoInverted)
        val rows = vm.rows
        assertEquals(8, rows.size)
        val pay = rows.single { it.line.description == "Pagamento recebido" }
        assertEquals(RowFlag.CARD_CREDIT, pay.flag)
        assertFalse(pay.selected)
        val refund = rows.single { it.line.description.startsWith("Estorno") }
        assertEquals(TxType.INCOME, refund.type)
        assertTrue(refund.selected)
        assertEquals(TxType.EXPENSE, rows.single { it.line.description == "Spotify" }.type)
        assertTrue(exists("o Bambua já virou os sinais"))
        captureScreenRoboImage("screenshots/39_importar_fatura.png")

        rule.onNodeWithText("Importar 7 movimentações").performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Done }
        val txs = imported()
        assertEquals(7, txs.size)
        assertTrue(txs.all { it.cardId == roxo && it.accountId == null && it.invoiceYm == t.invoiceYm })
        assertEquals(TxType.INCOME, txs.single { it.description.startsWith("Estorno") }.type)
    }

    @Test fun pagamentoDeFaturaNaContaViraPagamentoDoCartao() {
        show()
        pick(SampleFiles.ofx(today))
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Pagamento Fatura Cartão"))
        rule.onNodeWithText("Pagamento Fatura Cartão").performClick()
        waitFor(5_000) { exists("É o pagamento de qual cartão?") }
        rule.onNodeWithText("Roxo").performClick()
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/40_importar_linha.png")
        rule.onNodeWithText("Pronto").performScrollTo().performClick()
        waitFor(5_000) { vm.rows.single { it.flag == RowFlag.INVOICE_PAYMENT }.selected }
        assertEquals(12, vm.selectedCount)

        rule.onNodeWithText("Importar 12 movimentações").performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Done }
        val payment = imported().single { it.type == TxType.CARD_PAYMENT }
        assertEquals(roxo, payment.cardId)
        assertEquals(nubank, payment.accountId)
        assertEquals(125000L, payment.amount)
        val card = runBlocking { c.repo.loadOnce().cards.single() }
        assertEquals(ImportPlanner.paidInvoice(card, today.minusDays(15)), payment.invoiceYm)
        assertEquals(null, payment.categoryId)
    }

    @Test fun marcarTodasAvisaPagamentoDeFaturaSemCartao() {
        show()
        pick(SampleFiles.ofx(today))
        assertEquals(0, vm.selectedUnpairedPayments)
        rule.onNodeWithContentDescription("Mais opções").performClick()
        rule.onNodeWithText("Marcar todas").performClick()
        waitFor(5_000) { vm.selectedCount == 13 }
        // Sem o cartão escolhido, o pagamento entraria como gasto comum: o Bambua avisa.
        assertEquals(1, vm.selectedUnpairedPayments)
        assertTrue(exists("1 pagamento de fatura marcado vai entrar como gasto comum"))

        // Escolhendo o cartão, ele vira pagamento do cartão e o aviso some.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Pagamento Fatura Cartão"))
        rule.onNodeWithText("Pagamento Fatura Cartão").performClick()
        waitFor(5_000) { exists("É o pagamento de qual cartão?") }
        rule.onNodeWithText("Roxo").performClick()
        rule.onNodeWithText("Pronto").performScrollTo().performClick()
        waitFor(5_000) { vm.selectedUnpairedPayments == 0 }
        assertEquals(13, vm.selectedCount)
    }

    @Test fun marcarTodasNaFaturaAvisaPagamentoSemConta() {
        show()
        pick(SampleFiles.csvCard(today))
        rule.onNodeWithContentDescription("Mais opções").performClick()
        rule.onNodeWithText("Marcar todas").performClick()
        waitFor(5_000) { vm.selectedUnpairedPayments == 1 }
        assertTrue(exists("1 pagamento da fatura marcado vai entrar como crédito no cartão, sem sair de nenhuma conta"))
    }

    @Test fun colunasQueOBambuaNaoEntendeuAPessoaEscolhe() {
        // Nomes de coluna que o Bambua não conhece e um cabeçalho do banco antes da tabela: ele não tem certeza e pergunta.
        val csv = "Banco Exemplo\nExtrato de conta\nPeríodo: 01/09/2026 a 30/09/2026\n\nDia;Histórico;Entrou;Saiu\n" +
            "01/10/2026;PADARIA;;12,50\n02/10/2026;PIX DA ANA;50,00;\n03/10/2026;MERCADO;;80,00\n"
        show()
        answer = file(SampleFiles.Sample("estranho.csv", "text/csv", "CSV", csv.toByteArray()))
        rule.onNodeWithText("Escolher arquivo").performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Columns }
        rule.waitForIdle()

        fun chip(text: String, group: Int) = rule.onAllNodes(hasText(text, substring = true) and hasClickAction())[group]
        chip("Dia", 0).performClick()
        chip("Histórico", 1).performClick()
        rule.onNodeWithText("Entradas e saídas").performClick()
        rule.waitForIdle()
        chip("Entrou", 2).performScrollTo().performClick()
        chip("Saiu", 3).performScrollTo().performClick()
        waitFor(5_000) { exists("Mercado") }
        captureScreenRoboImage("screenshots/41_importar_colunas.png")

        rule.onNodeWithText("Continuar").performClick()
        waitFor(10_000) { vm.step == ImportModel.Step.Review }
        assertEquals(listOf(-1250L, 5000L, -8000L), vm.rows.map { it.signed })
    }

    @Test fun pdfComSenhaPedeASenhaAteAcertar() {
        show()
        answer = file(SampleFiles.Sample("extrato.pdf", "application/pdf", "PDF", "%PDF-1.7 de teste".toByteArray()))
        rule.onNodeWithText("Escolher arquivo").performClick()
        waitFor(10_000) { vm.step is ImportModel.Step.Password }
        rule.onNode(hasSetTextAction()).performTextInput("0000")
        rule.onNodeWithText("Abrir o PDF").performClick()
        waitFor(10_000) { (vm.step as? ImportModel.Step.Password)?.retry == true }
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/42_importar_senha.png")
        rule.onNode(hasSetTextAction()).performTextReplacement("1234")
        rule.onNodeWithText("Abrir o PDF").performClick()
        waitFor(10_000) { vm.step == ImportModel.Step.Review }
        assertEquals(listOf(-1250L, 5000L), vm.rows.map { it.signed })
    }

    @Test fun arquivoQueNaoEExtratoExplicaOQueFazer() {
        c.dev.enabled = true // o relatório da leitura só fica guardado com o modo desenvolvedor ligado
        show()
        answer = file(SampleFiles.Sample("pagina.html", "text/html", "HTML", "<html><body><p>Acesse o internet banking</p></body></html>".toByteArray()))
        rule.onNodeWithText("Escolher arquivo").performClick()
        waitFor(10_000) { vm.error != null }
        assertTrue(exists("página da internet"))
        assertEquals(ImportModel.Step.Pick, vm.step)
        // O relatório (modo desenvolvedor) guarda o motivo.
        assertNotNull(c.lastImport.value?.message)
        c.dev.enabled = false
    }

    @Test fun jsonDeTesteEExemplosSoNoModoDeTeste() {
        show()
        // Com os dados de verdade: o JSON de teste e os extratos de exemplo são recusados, mesmo com o modo desenvolvedor.
        val real = ImportModel(AppContainer(ApplicationProvider.getApplicationContext()), today = { today })
        c.dev.enabled = true
        val json = SampleFiles.json(today)
        real.openBytes(json.name, json.bytes)
        waitFor(10_000) { real.error != null }
        assertTrue(real.error!!.contains("não parece um extrato"))
        val ofx = SampleFiles.ofx(today)
        real.openBytes(ofx.name, ofx.bytes)
        waitFor(10_000) { real.error?.contains("exemplo") == true }
        assertEquals(ImportModel.Step.Pick, real.step)

        // No modo de teste, os dois entram.
        vm.openBytes(json.name, json.bytes)
        waitFor(10_000) { vm.step == ImportModel.Step.Review }
        assertEquals(13, vm.rows.size)
        c.dev.enabled = false
    }
}
