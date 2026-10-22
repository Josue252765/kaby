package com.mofoxx.kaby.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.room.Room
import com.mofoxx.kaby.data.CuadreRepository
import com.mofoxx.kaby.data.KabyDatabase
import com.mofoxx.kaby.data.VentaP2pEntity
import com.mofoxx.kaby.binance.BinanceOrder
import com.mofoxx.kaby.binance.BinanceRead
import com.mofoxx.kaby.tasas.TasaClient
import com.mofoxx.kaby.tasas.TasaFetchResult
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.runBlocking

/** Clicks through the real Compose semantics tree in Robolectric, without an Android emulator. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SettingsInteractionTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: KabyDatabase
    private lateinit var vm: KabyViewModel
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), KabyDatabase::class.java)
            .addCallback(KabyDatabase.invariantsCallback).allowMainThreadQueries().build()
        vm = KabyViewModel(CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork }))
    }
    @After fun close() { database.close() }

    @Test fun connectDialogRequiresConsentAndCanBeCancelled() {
        compose.setContent { KabyTheme { SettingsPage(vm, null, false, {}, {}, {}) } }
        compose.onNodeWithText("Traer datos de Binance").performScrollTo().performClick()
        compose.onNodeWithText("Consultar y olvidar llave").assertIsNotEnabled()
        compose.onNodeWithText("API key").performTextInput("invalid-fixture")
        compose.onNodeWithText("SECRET").performTextInput("invalid-fixture")
        compose.onNodeWithText("Consultar y olvidar llave").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithText("Conectar Binance").assertDoesNotExist()
        compose.onNodeWithText("Traer datos de Binance").assertExists()
    }

    @Test fun settingsActionsCallTheirDestinationsAndPersistBackupFile() {
        var rates = false
        var checklist = false
        var motion = true
        compose.setContent { KabyTheme { SettingsPage(vm, null, true, { motion = it }, { checklist = true }, { rates = true }) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Crear copia ahora"))
        compose.onNodeWithText("Crear copia ahora").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            File(RuntimeEnvironment.getApplication().filesDir, "local-backup/kaby-latest.json").isFile
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Administrar tasa BCV"))
        compose.onNodeWithText("Administrar tasa BCV").performScrollTo().performClick()
        compose.onNodeWithText("Estado de preparación · M1").performScrollTo().performClick()
        compose.onNode(isToggleable() and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Switch)).performClick()
        compose.runOnIdle { assertTrue(rates); assertTrue(checklist); assertFalse(motion) }
        assertNotNull(com.mofoxx.kaby.data.BackupSnapshotCodec.decode(File(RuntimeEnvironment.getApplication().filesDir, "local-backup/kaby-latest.json").readText()))
    }

    @Test fun mainNavigationReachesFunctionalSettingsAndReturnsHome() {
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Conexiones y datos").assertExists()
        compose.onNodeWithText("Inicio").performClick()
        compose.onNodeWithText("SALDO MERCANTIL").assertExists()
    }

    @Test fun homeShowsLocalReadFailureInsteadOfUnknownBalanceAsIfEmpty() {
        database.close()
        compose.setContent { KabyApp(vm) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No pude leer los datos guardados", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Bs 0,00").assertDoesNotExist()
    }

    @Test fun homeOffersOnlyStoredPendingBinanceSaleAndOpensItsReview() {
        val time = java.time.Instant.now().minusSeconds(3600).toEpochMilli()
        val repo = CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork })
        runBlocking {
            repo.storeBinanceRead(BinanceRead(listOf(BinanceOrder("fixture-order", time, "100", "1000", "10")),
                "4", time + 1000, time - 1000, time))
        }
        compose.setContent { KabyApp(vm) }
        compose.waitUntil(5_000) { vm.binanceReads.value.size == 1 }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("1 venta(s) Binance por conciliar"))
        compose.onNodeWithText("1 venta(s) Binance por conciliar").performClick()
        compose.onNodeWithText("Conexiones y datos").assertExists()
    }

    @Test fun navigationOpensP2PAndRatesAndThePrimaryActionsAreReachable() {
        compose.setContent { KabyApp(vm) }
        compose.onNode(hasText("Tasas") and hasClickAction()).performClick()
        compose.onNodeWithText("Referencia BCV").assertExists()
        compose.onNodeWithText("Registrar desde el tablero").assertExists()

        compose.onNode(hasText("P2P") and hasClickAction()).performClick()
        compose.onNodeWithText("Ventas P2P").assertExists()
        compose.onNodeWithText("Registrar venta P2P").performClick()
        compose.onNodeWithText("Venta P2P a mano").assertExists()
    }

    @Test fun emptyMovementAmountIsNotPresentedAsZero() {
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithContentDescription("Registrar movimiento").performClick()
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("0,00").assertDoesNotExist()
    }

    @Test fun incomeFilterShowsIncomeSummaryInsteadOfExpenseCopy() {
        runBlocking {
            val at = java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Caracas")).minusHours(1).toString()
            val repo = CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork })
            repo.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120", occurredAt = at,
                category = "Comida", note = null, thirdParty = null)
            repo.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200", occurredAt = at,
                category = "Apoyo", note = null, thirdParty = "María · remesa")
        }
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithText("Registro").performClick()
        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithText("Gastos · Bs 120,00").assertExists()
        compose.onNodeWithText("Ingresos").performClick()
        compose.onNodeWithText("Ingresos").assertIsSelected()
        compose.onNodeWithText("Entradas directas · Bs 200,00").assertExists()
        compose.onNodeWithText("gastos filtrados", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Todo").performClick()
        compose.onNodeWithText("Entradas directas · Bs 200,00", substring = true).assertExists()
    }

    @Test fun usdtExpenseWithoutRateDoesNotClaimACompleteBsExpenseTotal() {
        runBlocking {
            CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork }).saveMovement(
                direction = "GASTO", currency = "USDT", amountText = "3.50",
                occurredAt = java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Caracas")).minusHours(1).toString(),
                category = "Compra online", note = null, thirdParty = null)
        }
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithText("Registro").performClick()
        compose.onNodeWithText("Gastos").performClick()
        compose.onNodeWithText("Gastos valuados · Bs 0,00").assertExists()
        compose.onNodeWithText("sin tasa confirmada no tienen equivalencia completa", substring = true).assertExists()
    }

    @Test fun cancellingMovementFormDoesNotWriteAnything() {
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithContentDescription("Registrar movimiento").performClick()
        compose.onNodeWithText("E-SIN-RED", substring = true).assertDoesNotExist()
        compose.onNodeWithText("1").performScrollTo().performClick()
        compose.onAllNodesWithText("1").assertCountEquals(2)
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertTrue(runBlocking { database.kabyDao().allMovements().isEmpty() }) }
    }

    @Test fun savingExpenseThenDirectIncomeCreatesTwoRowsAndShowsThemInActivity() {
        compose.setContent { KabyApp(vm) }
        compose.onNodeWithContentDescription("Registrar movimiento").performClick()
        compose.onNodeWithText("E-SIN-RED", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Sale de Mercantil · Bs").assertExists()
        compose.onNodeWithText("1").performScrollTo().performClick()
        compose.onNodeWithText("2").performScrollTo().performClick()
        compose.onNodeWithText("0").performScrollTo().performClick()
        compose.onNodeWithText("120").assertExists()
        compose.onNodeWithText("Categoría").performScrollTo().performTextInput("Comida")
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Registrar movimiento").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, runBlocking { database.kabyDao().allMovements().size })
        compose.onNodeWithText("Cerrar").performClick()

        compose.onNodeWithContentDescription("Registrar movimiento").performClick()
        compose.onNodeWithText("Otra plata que entra").performClick()
        compose.onNodeWithText("Entra a Mercantil · Bs").assertExists()
        compose.onNodeWithText("2").performScrollTo().performClick()
        compose.onNodeWithText("0").performScrollTo().performClick()
        compose.onNodeWithText("0").performScrollTo().performClick()
        compose.onNodeWithText("Categoría").performScrollTo().performTextInput("Apoyo")
        compose.onNodeWithText("De quién y por qué").performScrollTo().performTextInput("María · remesa")
        compose.onNodeWithText("Guardar entrada").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Registrar movimiento").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, runBlocking { database.kabyDao().allMovements().size })
        compose.onNodeWithText("Registro").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Apoyo"))
        compose.onNodeWithText("Apoyo").assertExists()
        compose.runOnIdle { assertEquals(listOf("GASTO", "INGRESO"), runBlocking { database.kabyDao().allMovements() }.map { it.tipo }) }
    }

    @Test fun twoImmediateSaveRequestsCreateOnlyOneMovement() {
        val at = java.time.OffsetDateTime.now(java.time.ZoneId.of("America/Caracas")).minusHours(1)
            .format(KabyViewModel.DATE_TIME_FORMAT)
        compose.setContent {
            KabyTheme {
                val saving = vm.isSavingEntry.collectAsState().value
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Button(onClick = {
                        repeat(2) {
                            vm.saveMovement(direction = "GASTO", currency = "Bs", amount = "120", dateTime = at,
                                category = "Comida", note = "", thirdParty = "")
                        }
                    }) { androidx.compose.material3.Text("Dos guardados") }
                    androidx.compose.material3.Text(if (saving) "Guardando" else "Listo")
                }
            }
        }
        compose.onNodeWithText("Dos guardados").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Listo").fetchSemanticsNodes().isNotEmpty() }
        val rows = runBlocking { database.kabyDao().allMovements() }
        assertEquals(1, rows.size)
        assertEquals(listOf("CREAR"), runBlocking { CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork }).auditForMovement(rows.single().id) }.map { it.accion })
    }

    @Test fun recreatedViewModelReadsPersistedMercantilBalance() {
        val repo = CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork })
        val zone = java.time.ZoneId.of("America/Caracas")
        runBlocking {
            repo.saveInitialAnchor("1000", java.time.OffsetDateTime.now(zone).minusHours(3).toString())
            repo.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120",
                occurredAt = java.time.OffsetDateTime.now(zone).minusHours(2).toString(),
                category = "Comida", note = null, thirdParty = null)
            repo.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200",
                occurredAt = java.time.OffsetDateTime.now(zone).minusHours(1).toString(),
                category = "Apoyo", note = null, thirdParty = "María · remesa")
        }
        val recreated = KabyViewModel(CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork }))
        compose.setContent { KabyApp(recreated) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bs 1.080,00").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun homeReviewsSignedDifferenceBeforeSavingReconciliation() {
        val repo = CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork })
        val zone = java.time.ZoneId.of("America/Caracas")
        runBlocking {
            repo.saveInitialAnchor("1000", java.time.OffsetDateTime.now(zone).minusHours(3).toString())
            repo.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120",
                occurredAt = java.time.OffsetDateTime.now(zone).minusHours(2).toString(),
                category = "Comida", note = null, thirdParty = null)
            repo.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200",
                occurredAt = java.time.OffsetDateTime.now(zone).minusHours(1).toString(),
                category = "Apoyo", note = null, thirdParty = "María · remesa")
        }
        compose.setContent { KabyApp(vm) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bs 1.080,00").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Iniciar cuadre"))
        compose.onNodeWithText("Iniciar cuadre").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Saldo observado · Bs"))
        compose.onNodeWithText("Saldo observado · Bs").performTextInput("1030")
        compose.onNodeWithText("Comparar").performScrollTo().performClick()
        compose.onNodeWithText("Diferencia · Bs -50,00").assertExists()
        compose.onNodeWithText("Confirmar cuadre").performClick()
        compose.waitUntil(5_000) { runBlocking { database.kabyDao().accountNow()?.cifraDescuadre == "-50.00" } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Descuadre abierto"))
        compose.onNodeWithText("Diferencia observada · Bs -50,00").assertExists()
    }

    @Test @Config(qualifiers = "w320dp-h640dp")
    fun compactHomeKeepsAnchorAndNavigationReachableWithLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.4f)) {
                KabyApp(vm)
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Falta ancla").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("Bs 0,00").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Guardar saldo inicial"))
        compose.onNodeWithText("Guardar saldo inicial").assertHasClickAction()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("BCV", substring = true))
        assertTrue(compose.onAllNodesWithText("BCV", substring = true).fetchSemanticsNodes().isNotEmpty())
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Registro"))
        assertTrue(compose.onAllNodesWithText("Registro").fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodesWithText("Registro").onFirst().performClick()
        compose.onNodeWithText("Tu actividad").assertExists()
    }

    @Test fun p2pRowDistinguishesBinanceReadFromOwnerConfirmedNetAndExplainsPremium() {
        val timestamp = "2026-10-21T12:00:00-04:00"
        runBlocking {
            database.kabyDao().insertSale(VentaP2pEntity(
                fechaHora = timestamp,
                bsNetos = "100.00",
                tasaP2p = "100.00",
                usdtCantidad = "1.00",
                estadoValuacion = "SIN VALUAR",
                fuente = "API",
                timestampCreacion = timestamp,
                timestampUltimaEdicion = timestamp,
            ))
        }
        compose.setContent { KabyApp(vm) }
        compose.onNode(hasText("P2P") and hasClickAction()).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Orden leída de Binance", substring = true))
        compose.onNodeWithText("Orden leída de Binance · neto confirmado por ti", substring = true).assertExists()
        compose.onNodeWithText("Prima sin valuar", substring = true).performClick()
        compose.onNodeWithText("Bolívares netos recibidos menos", substring = true).assertExists()
    }
}
