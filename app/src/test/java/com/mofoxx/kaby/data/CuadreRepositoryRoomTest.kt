package com.mofoxx.kaby.data

import android.app.Application
import androidx.room.Room
import com.google.gson.JsonParser
import com.mofoxx.kaby.tasas.TasaClient
import com.mofoxx.kaby.tasas.TasaFetchResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.OffsetDateTime
import java.time.ZoneId
import java.io.File
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class CuadreRepositoryRoomTest {
    private lateinit var database: KabyDatabase
    private lateinit var repository: CuadreRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            KabyDatabase::class.java,
        )
            .addCallback(KabyDatabase.invariantsCallback)
            .allowMainThreadQueries()
            .build()
        repository = CuadreRepository(database, TasaClient { TasaFetchResult.NoNetwork })
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun testBitacora_editarDejaRastro() = runBlocking {
        repository.saveMovement(
            direction = "GASTO",
            currency = "Bs",
            amountText = "100",
            occurredAt = pastTime(),
            category = "Alimentos",
            note = null,
            thirdParty = null,
        )
        val created = database.kabyDao().allMovements().single()

        repository.saveMovement(
            id = created.id,
            direction = created.tipo,
            nature = created.naturaleza,
            currency = created.moneda,
            amountText = "120",
            occurredAt = created.fechaHora,
            category = "Comida",
            note = null,
            thirdParty = null,
        )

        val updated = database.kabyDao().movement(created.id)!!
        val entries = repository.auditForMovement(created.id).sortedBy { it.id }
        assertEquals(listOf("CREAR", "EDITAR"), entries.map { it.accion })
        val edit = entries.last()
        val before = JsonParser.parseString(edit.antes).asJsonObject
        val after = JsonParser.parseString(edit.despues).asJsonObject
        assertEquals("100", before.get("monto").asString)
        assertEquals("Alimentos", before.get("categoria").asString)
        assertEquals("120", after.get("monto").asString)
        assertEquals("Comida", after.get("categoria").asString)
        assertEquals(created.timestampCreacion, updated.timestampCreacion)
        assertEquals(updated.timestampUltimaEdicion, edit.timestamp)
        assertNotNull(OffsetDateTime.parse(edit.timestamp))
    }

    @Test
    fun testRegistroGastoEIngresoMercantilSeAplicanUnaVezYUsdtNoLoAltera() = runBlocking {
        val anchorAt = timeAgo(8)
        repository.saveInitialAnchor("1000", anchorAt)
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120", occurredAt = timeAgo(7),
            category = "Comida", note = null, thirdParty = null)
        val dao = database.kabyDao()
        val originalExpense = dao.allMovements().single()
        assertEquals("880.00", KabySnapshot(dao.accountNow(), listOf(originalExpense), emptyList(), emptyList()).summary.mercantilBs?.toPlainString())
        repository.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200", occurredAt = timeAgo(6),
            category = "Apoyo", note = null, thirdParty = "María · remesa")
        assertEquals("1080.00", KabySnapshot(dao.accountNow(), dao.allMovements(), emptyList(), emptyList()).summary.mercantilBs?.toPlainString())
        repository.saveMovement(direction = "GASTO", currency = "USDT", amountText = "3.50", occurredAt = timeAgo(5),
            category = "Compra online", note = null, thirdParty = null)

        val rows = dao.allMovements()
        val snapshot = KabySnapshot(dao.accountNow(), rows, emptyList(), emptyList())
        assertEquals(3, rows.size)
        assertEquals("1080.00", snapshot.summary.mercantilBs?.toPlainString())
        assertEquals("200.00", snapshot.summary.incomeBs.toPlainString())
        assertEquals("120.00", snapshot.summary.expenseBs.toPlainString())
        assertEquals("SIN VALUAR", rows.single { it.moneda == "USDT" }.estadoValuacion)
        assertEquals(originalExpense, dao.movement(originalExpense.id))
        rows.forEach { row -> assertEquals(listOf("CREAR"), repository.auditForMovement(row.id).map { it.accion }) }
    }

    @Test
    fun testCuadreComparaSoloMovimientosHastaLaHoraObservada() = runBlocking {
        val anchorAt = timeAgo(8)
        val expenseAt = timeAgo(6)
        val observedAt = timeAgo(4)
        val laterIncomeAt = timeAgo(2)
        repository.saveInitialAnchor("1000", anchorAt)
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120", occurredAt = expenseAt,
            category = "Comida", note = null, thirdParty = null)
        repository.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200", occurredAt = laterIncomeAt,
            category = "Apoyo", note = null, thirdParty = "María · remesa")

        repository.reconcile("880", observedAt)

        val account = database.kabyDao().accountNow()!!
        assertEquals("CALCULADO", account.estadoCuadre)
        assertNull(account.cifraDescuadre)
        assertEquals("1080.00", KabySnapshot(account, database.kabyDao().allMovements(), emptyList(), emptyList()).summary.mercantilBs?.toPlainString())
    }

    @Test
    fun testCuadreDescuadreNegativoAjusteConRastroYAplicacionUnica() = runBlocking {
        repository.saveInitialAnchor("1000", timeAgo(8))
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120", occurredAt = timeAgo(7),
            category = "Comida", note = null, thirdParty = null)
        repository.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200", occurredAt = timeAgo(6),
            category = "Apoyo", note = null, thirdParty = "María · remesa")
        val observedAt = timeAgo(5)
        repository.reconcile("1030", observedAt, "1080.00")
        val dao = database.kabyDao()
        assertEquals("-50.00", dao.accountNow()?.cifraDescuadre)
        assertEquals("DESCUADRE_ABIERTO", dao.accountNow()?.estadoCuadre)
        assertEquals(observedAt, dao.accountNow()?.ultimoCuadreAt)
        assertEquals(listOf("CREAR", "EDITAR"), repository.auditFor("estado_cuentas", 1).map { it.accion })

        repository.saveMovement(direction = "GASTO", nature = "AJUSTE_CUADRE", currency = "Bs", amountText = "50",
            occurredAt = timeAgo(6), category = "Ajuste de cuadre", note = "Comisión omitida", thirdParty = null)
        val adjustment = dao.allMovements().single { it.naturaleza == "AJUSTE_CUADRE" }
        val snapshot = KabySnapshot(dao.accountNow(), dao.allMovements(), emptyList(), emptyList())
        assertEquals("1030.00", snapshot.summary.mercantilBs?.toPlainString())
        assertNull(snapshot.summary.discrepancyBs)
        assertEquals("CALCULADO", snapshot.summary.reconciliationState)
        assertEquals("120.00", snapshot.summary.expenseBs.toPlainString())
        assertEquals("200.00", snapshot.summary.incomeBs.toPlainString())
        assertEquals(listOf("CREAR"), repository.auditForMovement(adjustment.id).map { it.accion })
        assertEquals(3, dao.allMovements().size)
    }

    @Test
    fun testEditarFechaQueCruzaElCorteReabreConImpactoDelCorte() = runBlocking {
        repository.saveInitialAnchor("1000", timeAgo(8))
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "100", occurredAt = timeAgo(6),
            category = "Comida", note = null, thirdParty = null)
        val original = database.kabyDao().allMovements().single()
        val closeAt = timeAgo(4)
        repository.reconcile("900", closeAt)
        repository.saveMovement(id = original.id, direction = original.tipo, nature = original.naturaleza,
            currency = original.moneda, amountText = original.monto, occurredAt = timeAgo(2),
            category = original.categoria, note = null, thirdParty = null)

        val account = database.kabyDao().accountNow()!!
        assertEquals("DESCUADRE_ABIERTO", account.estadoCuadre)
        assertEquals("-100.00", account.cifraDescuadre)
        assertEquals(listOf("CREAR", "EDITAR"), repository.auditForMovement(original.id).sortedBy { it.id }.map { it.accion })
    }

    @Test
    fun testEditarVentaQueCruzaElCorteNoMantieneCuadreFalso() = runBlocking {
        repository.saveInitialAnchor("1000", timeAgo(8))
        repository.saveManualSale(netBsText = "500", p2pRateText = "1000", usdtText = "0.5",
            occurredAt = timeAgo(6), provenance = "Venta anotada")
        val original = database.kabyDao().allSales().single()
        repository.reconcile("1500", timeAgo(4))
        repository.saveManualSale(id = original.id, netBsText = original.bsNetos,
            p2pRateText = original.tasaP2p, usdtText = original.usdtCantidad,
            occurredAt = timeAgo(2), provenance = original.proveniencia)

        val account = database.kabyDao().accountNow()!!
        assertEquals("DESCUADRE_ABIERTO", account.estadoCuadre)
        assertEquals("500.00", account.cifraDescuadre)
        assertEquals(listOf("CREAR", "EDITAR"), repository.auditFor("ventas_p2p_mano", original.id).sortedBy { it.id }.map { it.accion })
    }

    @Test
    fun testCuadreRechazaCorteAnteriorAlAnclaYCambioDesdeVistaPrevia() = runBlocking {
        repository.saveInitialAnchor("1000", timeAgo(4))
        assertNotNull(runCatching { repository.reconcile("1000", timeAgo(5)) }.exceptionOrNull())
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "10", occurredAt = timeAgo(3),
            category = "Comida", note = null, thirdParty = null)
        assertNotNull(runCatching { repository.reconcile("990", timeAgo(2), "1000.00") }.exceptionOrNull())
        assertNull(database.kabyDao().accountNow()?.ultimoCuadreAt)
        assertEquals(listOf("CREAR"), repository.auditFor("estado_cuentas", 1).map { it.accion })
    }

    @Test
    fun testCuadreNoRetrocedeLaFechaDeLaUltimaComparacion() = runBlocking {
        repository.saveInitialAnchor("1000", timeAgo(8))
        val latestCut = timeAgo(2)
        repository.reconcile("1000", latestCut)
        assertNotNull(runCatching { repository.reconcile("1000", timeAgo(4)) }.exceptionOrNull())
        assertEquals(latestCut, database.kabyDao().accountNow()?.ultimoCuadreAt)
        assertEquals(listOf("CREAR", "EDITAR"), repository.auditFor("estado_cuentas", 1).sortedBy { it.id }.map { it.accion })
    }

    @Test
    fun testAnclaSeGuardaUnaSolaVezSinContarComoIngreso() = runBlocking {
        val at = timeAgo(4)
        repository.saveInitialAnchor("1000", at)
        assertNotNull(runCatching { repository.saveInitialAnchor("2000", timeAgo(3)) }.exceptionOrNull())
        val dao = database.kabyDao()
        assertEquals("1000", dao.accountNow()?.anclaMonto)
        assertEquals(at, dao.accountNow()?.anclaFechaHora)
        assertEquals("0.00", KabySnapshot(dao.accountNow(), emptyList(), emptyList(), emptyList()).summary.incomeBs.toPlainString())
        assertEquals(listOf("CREAR"), repository.auditFor("estado_cuentas", 1).map { it.accion })
    }

    @Test
    fun testSaldoSeReconstruyeAlReabrirBaseDePruebaPersistente() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "kaby-increment02-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, KabyDatabase::class.java, name)
            .addCallback(KabyDatabase.invariantsCallback).allowMainThreadQueries().build()
        var fileDb = open()
        try {
            val firstRepo = CuadreRepository(fileDb, TasaClient { TasaFetchResult.NoNetwork })
            firstRepo.saveInitialAnchor("1000", timeAgo(8))
            firstRepo.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120", occurredAt = timeAgo(7),
                category = "Comida", note = null, thirdParty = null)
            firstRepo.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200", occurredAt = timeAgo(6),
                category = "Apoyo", note = null, thirdParty = "María · remesa")
            fileDb.close()
            fileDb = open()
            val dao = fileDb.kabyDao()
            assertEquals("1080.00", KabySnapshot(dao.accountNow(), dao.allMovements(), dao.allSales(), dao.ratesNow()).summary.mercantilBs?.toPlainString())
            assertEquals(2, dao.allMovements().size)
            assertEquals(listOf("CREAR"), CuadreRepository(fileDb, TasaClient { TasaFetchResult.NoNetwork })
                .auditFor("estado_cuentas", 1).map { it.accion })
        } finally {
            fileDb.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun testRegistroRechazaImportesInvalidosYProvenienciaAusenteSinEscribir() = runBlocking {
        val at = pastTime()
        listOf("", "0", "-1", "texto").forEach { amount ->
            assertNotNull(runCatching {
                repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = amount,
                    occurredAt = at, category = "Comida", note = null, thirdParty = null)
            }.exceptionOrNull())
        }
        assertNotNull(runCatching {
            repository.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200",
                occurredAt = at, category = "Apoyo", note = null, thirdParty = "")
        }.exceptionOrNull())
        assertNotNull(runCatching {
            repository.saveMovement(direction = "INGRESO", currency = "USDT", amountText = "1",
                occurredAt = at, category = "Apoyo", note = null, thirdParty = "María")
        }.exceptionOrNull())
        assertTrue(database.kabyDao().allMovements().isEmpty())
    }

    @Test
    fun testRegistroSinAnclaYAnteriorAlAnclaNoInventaNiCambiaSaldoInicial() = runBlocking {
        repository.saveMovement(direction = "GASTO", currency = "Bs", amountText = "120.25",
            occurredAt = timeAgo(8), category = "Comida", note = null, thirdParty = null)
        val dao = database.kabyDao()
        assertNull(KabySnapshot(null, dao.allMovements(), emptyList(), emptyList()).summary.mercantilBs)
        repository.saveInitialAnchor("1000", timeAgo(7))
        assertEquals("1000.00", KabySnapshot(dao.accountNow(), dao.allMovements(), emptyList(), emptyList()).summary.mercantilBs?.toPlainString())
        repository.saveMovement(direction = "INGRESO", currency = "Bs", amountText = "200.50",
            occurredAt = timeAgo(6), category = "Apoyo", note = null, thirdParty = "María · devolución")
        assertEquals("1200.50", KabySnapshot(dao.accountNow(), dao.allMovements(), emptyList(), emptyList()).summary.mercantilBs?.toPlainString())
    }

    @Test
    fun testBitacora_fallaAlGuardarRastroRevierteEdicion() = runBlocking {
        repository.saveMovement(
            direction = "GASTO",
            currency = "Bs",
            amountText = "100",
            occurredAt = pastTime(),
            category = "Alimentos",
            note = null,
            thirdParty = null,
        )
        val before = database.kabyDao().allMovements().single()
        database.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER rechazar_rastro_edicion
                BEFORE INSERT ON bitacora WHEN NEW.accion = 'EDITAR'
                BEGIN SELECT RAISE(ABORT, 'TEST_FORCED_AUDIT_FAILURE'); END""",
        )

        val failure = runCatching {
            repository.saveMovement(
                id = before.id,
                direction = before.tipo,
                nature = before.naturaleza,
                currency = before.moneda,
                amountText = "120",
                occurredAt = before.fechaHora,
                category = "Comida",
                note = null,
                thirdParty = null,
            )
        }.exceptionOrNull()

        assertNotNull("el trigger debía rechazar la inserción de auditoría", failure)
        assertEquals(before, database.kabyDao().movement(before.id))
        assertEquals(listOf("CREAR"), repository.auditForMovement(before.id).map { it.accion })
    }

    @Test
    fun testPrima_editarFila_recalculaConRastro() = runBlocking {
        val rateAt = timeAgo(4)
        val saleAt = timeAgo(3)
        repository.saveManualRate("880", rateAt)
        repository.saveManualSale(
            netBsText = "50000",
            p2pRateText = "1000",
            usdtText = "50",
            occurredAt = saleAt,
            provenance = null,
        )
        val created = database.kabyDao().allSales().single()
        assertEquals("6000.00", created.prima)

        repository.saveManualSale(
            id = created.id,
            netBsText = "50100",
            p2pRateText = created.tasaP2p,
            usdtText = created.usdtCantidad,
            occurredAt = created.fechaHora,
            provenance = created.proveniencia,
        )

        val updated = database.kabyDao().sale(created.id)!!
        val entries = repository.auditFor("ventas_p2p_mano", created.id).sortedBy { it.id }
        assertEquals("6100.00", updated.prima)
        assertEquals("880", updated.tasaBcvAlMomento)
        assertEquals(created.timestampCreacion, updated.timestampCreacion)
        assertEquals(listOf("CREAR", "EDITAR"), entries.map { it.accion })
        val before = JsonParser.parseString(entries.last().antes).asJsonObject
        val after = JsonParser.parseString(entries.last().despues).asJsonObject
        assertEquals("50000", before.get("bsNetos").asString)
        assertEquals("50100", after.get("bsNetos").asString)
    }

    @Test
    fun testPrima_sinBcvDelDia_sinValuar() = runBlocking {
        repository.saveManualSale(
            netBsText = "6450",
            p2pRateText = "860",
            usdtText = "7.5",
            occurredAt = pastTime(),
            provenance = null,
        )

        val sale = database.kabyDao().allSales().single()
        val summary = KabySnapshot(account = null, movements = emptyList(), sales = listOf(sale), rates = emptyList()).summary

        assertNull(sale.tasaBcvAlMomento)
        assertNull(sale.prima)
        assertEquals("SIN VALUAR", sale.estadoValuacion)
        assertEquals(1, summary.unvaluedRows)
    }

    @Test
    fun testTasas_prellenado_espejoNoAutomatico() = runBlocking {
        val proposal = com.mofoxx.kaby.tasas.TasaPropuesta(
            valor = BigDecimal("883.3963"),
            horaFuente = OffsetDateTime.now(ZoneId.of("America/Caracas")).minusHours(1),
            proveedor = "DOLARAPI",
        )
        val proposalRepository = CuadreRepository(
            database,
            TasaClient { TasaFetchResult.Success(proposal) },
        )

        assertEquals(TasaFetchResult.Success(proposal), proposalRepository.proposeRate())
        assertTrue(database.kabyDao().ratesNow().isEmpty())

        proposalRepository.confirmProposal(proposal)
        val saved = database.kabyDao().ratesNow().single()
        assertEquals("HUMANO", saved.confirmadaPor)
        assertEquals("ESPEJO_DOLARAPI", saved.fuente)
        assertEquals(listOf("CREAR"), proposalRepository.auditFor("tasas_bcv", saved.id).map { it.accion })
    }

    @Test
    fun testCuadre_editarAntesDeCuadre_reabre() = runBlocking {
        val anchorAt = timeAgo(8)
        val movementAt = timeAgo(7)
        val saleAt = timeAgo(6)
        val closeAt = timeAgo(5)
        repository.saveInitialAnchor("1000", anchorAt)
        repository.saveMovement(
            direction = "GASTO",
            currency = "Bs",
            amountText = "100",
            occurredAt = movementAt,
            category = "Alimentos",
            note = null,
            thirdParty = null,
        )
        val movement = database.kabyDao().allMovements().single()
        repository.saveManualSale(
            netBsText = "500",
            p2pRateText = "1000",
            usdtText = "0.5",
            occurredAt = saleAt,
            provenance = null,
        )
        val sale = database.kabyDao().allSales().single()
        repository.reconcile("1400", closeAt)
        assertEquals("CALCULADO", database.kabyDao().accountNow()?.estadoCuadre)

        repository.saveMovement(
            id = movement.id,
            direction = movement.tipo,
            nature = movement.naturaleza,
            currency = movement.moneda,
            amountText = "120",
            occurredAt = movement.fechaHora,
            category = movement.categoria,
            note = movement.motivo,
            thirdParty = movement.provenienciaTercero,
        )
        assertEquals("20.00", database.kabyDao().accountNow()?.cifraDescuadre)

        repository.saveManualSale(
            id = sale.id,
            netBsText = "550",
            p2pRateText = sale.tasaP2p,
            usdtText = sale.usdtCantidad,
            occurredAt = sale.fechaHora,
            provenance = sale.proveniencia,
        )
        val reopened = database.kabyDao().accountNow()
        assertEquals("DESCUADRE_ABIERTO", reopened?.estadoCuadre)
        assertEquals("-30.00", reopened?.cifraDescuadre)
    }

    @Test
    fun testRoom_invariantesDeDominioDeclaradasEnCallbacks() = runBlocking {
        val at = pastTime()
        val base = MovimientoEntity(
            tipo = "GASTO",
            naturaleza = "NORMAL",
            moneda = "Bs",
            monto = "100",
            fechaHora = at,
            categoria = "Alimentos",
            timestampCreacion = at,
            timestampUltimaEdicion = at,
        )
        val invalidRows = listOf(
            base.copy(monto = "-1"),
            base.copy(tipo = "INGRESO", provenienciaTercero = null),
            base.copy(moneda = "USDT", tasaBcvAlMomento = null, estadoValuacion = "VALUADA"),
        )
        invalidRows.forEach { invalid ->
            val failure = runCatching { database.kabyDao().insertMovement(invalid) }.exceptionOrNull()
            assertNotNull("el trigger debía rechazar $invalid", failure)
        }
        assertTrue(database.kabyDao().allMovements().isEmpty())

        val validId = database.kabyDao().insertMovement(base)
        assertNotNull(database.kabyDao().movement(validId))
        val daoSource = File("src/main/java/com/mofoxx/kaby/data/KabyDao.kt").readText()
        assertTrue(!daoSource.contains("@Delete"))
        assertTrue(!daoSource.contains("DELETE FROM movimientos") && !daoSource.contains("DELETE FROM ventas_p2p_mano"))
    }

    @Test
    fun testRoom_rotacionAcotadaSoloParaErrores() = runBlocking {
        repository.saveMovement(
            direction = "GASTO",
            currency = "Bs",
            amountText = "100",
            occurredAt = pastTime(),
            category = "Alimentos",
            note = null,
            thirdParty = null,
        )
        val movement = database.kabyDao().allMovements().single()

        repeat(CuadreRepository.ERROR_LOG_LIMIT + 1) { repository.logRateError("E-TASAS-SIN-FUENTE") }

        val errors = database.kabyDao().allErrors()
        assertEquals(CuadreRepository.ERROR_LOG_LIMIT, errors.size)
        assertEquals(2L, errors.first().id)
        assertEquals(CuadreRepository.ERROR_LOG_LIMIT.toLong() + 1, errors.last().id)
        assertEquals(movement, database.kabyDao().movement(movement.id))
    }

    @Test
    fun testLogs_sinSecretNiLlave() = runBlocking {
        val apiKeyCanary = "KABY_SYNTHETIC_API_KEY_9b1c"
        val secretCanary = "KABY_SYNTHETIC_SECRET_a82d"
        repository.logRateError("E-SIN-RED")
        repository.logDatabaseFailure()
        repository.logLocalBackupFailure()

        val rejected = runCatching {
            repository.logRateError("apiKey=$apiKeyCanary secret=$secretCanary")
        }.exceptionOrNull()

        assertTrue("el código fuera de la lista debe rechazarse", rejected is IllegalArgumentException)
        val entries = database.kabyDao().allErrors()
        assertEquals(3, entries.size)
        assertTrue(entries.none { entry ->
            listOf(entry.capa, entry.mensaje, entry.detalleTecnico.orEmpty(), entry.timestamp)
                .any { it.contains(apiKeyCanary) || it.contains(secretCanary) }
        })
        assertTrue(entries.all { entry ->
            entry.detalleTecnico == null || entry.detalleTecnico in setOf("E-SIN-RED", "E-TASAS-SIN-FUENTE")
        })
        assertFalse(entries.any { it.mensaje.contains("apiKey=") || it.mensaje.contains("secret=") })
    }

    private fun pastTime(): String = OffsetDateTime.now(ZoneId.of("America/Caracas")).minusHours(2).toString()
    private fun timeAgo(hours: Long): String = OffsetDateTime.now(ZoneId.of("America/Caracas")).minusHours(hours).toString()
}
