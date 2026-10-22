package com.mofoxx.kaby.data

import android.app.Application
import androidx.room.Room
import com.mofoxx.kaby.binance.BinanceOrder
import com.mofoxx.kaby.binance.BinanceRead
import com.mofoxx.kaby.tasas.TasaClient
import com.mofoxx.kaby.tasas.TasaFetchResult
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class BinanceRepositoryTest {
    private lateinit var db: KabyDatabase
    private lateinit var repo: CuadreRepository
    private val time = Instant.parse("2026-01-05T12:00:00Z").toEpochMilli()
    private val order = BinanceOrder("12345", time, "100", "1000", "10")
    private fun read(orders: List<BinanceOrder> = listOf(order)) = BinanceRead(orders, "50", time + 1000, time - 1000, time)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), KabyDatabase::class.java)
            .addCallback(KabyDatabase.invariantsCallback).allowMainThreadQueries().build()
        repo = CuadreRepository(db, TasaClient { TasaFetchResult.NoNetwork })
    }
    @After fun close() { db.close() }

    @Test fun readingTwiceDeduplicatesWithoutChangingLedger() = runBlocking {
        repo.saveInitialAnchor("20", "2026-01-01T00:00:00Z")
        repo.storeBinanceRead(read())
        repo.storeBinanceRead(read())
        assertEquals(1, db.kabyDao().allBinanceReads().size)
        assertTrue(db.kabyDao().allSales().isEmpty())
        assertEquals("20", db.kabyDao().accountNow()!!.anclaMonto)
        assertEquals("50", db.kabyDao().accountNow()!!.saldoUsdt)
    }
    @Test fun failedBatchRollsBackEveryRowAndPreservesFreshness() = runBlocking {
        repo.storeBinanceRead(read())
        val before = db.kabyDao().accountNow()
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            repo.storeBinanceRead(read(listOf(order.copy(number = "999"), order.copy(total = "9999"))).copy(readAt = time + 2000))
        } }
        assertEquals(listOf("12345"), db.kabyDao().allBinanceReads().map { it.orderNumber })
        assertEquals(before, db.kabyDao().accountNow())
    }
    @Test fun confirmsNetAndRetiresOnlySelectedManualBridgeOnce() = runBlocking {
        repo.saveInitialAnchor("100", "2026-01-01T00:00:00Z")
        repo.saveManualRate("80", "2026-01-01T00:00:00Z")
        repeat(2) { repo.saveManualSale(netBsText = "995", p2pRateText = "100", usdtText = "10", occurredAt = "2026-01-05T12:00:00Z", provenance = "fixture") }
        val manual = db.kabyDao().allSales().first()
        repo.storeBinanceRead(read())
        repo.reconcileBinanceOrder(order.number, "995", manual.id)
        val sales = db.kabyDao().allSales()
        assertEquals(1, sales.count { it.estado == "JUBILADA" })
        assertEquals(2, sales.count { it.estado == "VIVA" })
        val api = sales.single { it.fuente == "API" }
        assertEquals("995", api.bsNetos)
        assertEquals("195.00", api.prima)
        assertEquals(order.number, api.orderNumber)
        assertEquals("1000", db.kabyDao().allBinanceReads().single().totalPrice)
        assertEquals(1990, sales.filter { it.estado == "VIVA" }.sumOf { it.bsNetos.toInt() })
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.reconcileBinanceOrder(order.number, "995", null) } }
        assertEquals(3, db.kabyDao().allSales().size)
        assertEquals(2, db.kabyDao().auditFor("ventas_p2p_mano", manual.id).size)
    }
    @Test fun mismatchedBridgeDoesNotRetireOrInsert() = runBlocking {
        repo.saveManualSale(netBsText = "900", p2pRateText = "100", usdtText = "9", occurredAt = "2026-01-05T12:00:00Z", provenance = "fixture")
        repo.storeBinanceRead(read())
        val manual = db.kabyDao().allSales().single()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.reconcileBinanceOrder(order.number, "1000", manual.id) } }
        assertEquals(listOf(manual), db.kabyDao().allSales())
    }
    @Test fun settingAnchorAfterBinancePreservesBalanceAndReadTime() = runBlocking {
        repo.storeBinanceRead(read())
        val before = db.kabyDao().accountNow()!!
        repo.saveInitialAnchor("100", "2026-01-01T00:00:00Z")
        assertEquals(before.saldoUsdt, db.kabyDao().accountNow()!!.saldoUsdt)
        assertEquals(before.leidoEn, db.kabyDao().accountNow()!!.leidoEn)
    }
    @Test fun apiSaleCannotBeEditedThroughManualFormAndBackupIncludesReads() = runBlocking {
        repo.storeBinanceRead(read())
        repo.reconcileBinanceOrder(order.number, "1000", null)
        val sale = db.kabyDao().allSales().single()
        assertThrows(IllegalArgumentException::class.java) { runBlocking {
            repo.saveManualSale(sale.id, "2000", "100", "10", "2026-01-05T12:00:00Z", "fixture")
        } }
        assertEquals(sale, db.kabyDao().allSales().single())
        val backup = repo.localBackupJson()
        assertNotNull(BackupSnapshotCodec.decode(backup))
        assertTrue(backup.contains(order.number))
        assertFalse(backup.contains("secret", ignoreCase = true))
        assertFalse(backup.contains("apiKey", ignoreCase = true))
    }
}
