package com.mofoxx.kaby.binance

import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Instant

class BinanceClientTest {
    private val credentials = BinanceCredentials("K".repeat(64), "S".repeat(64))
    private val epoch = Instant.parse("2026-10-22T03:00:00Z").toEpochMilli()
    private val permissions = """{"enableReading":true,"enableWithdrawals":false,"enableInternalTransfer":false,"enableMargin":false,"enableFutures":false,"permitsUniversalTransfer":false,"enableVanillaOptions":false,"enableSpotAndMarginTrading":false,"enableFixApiTrade":false,"enablePortfolioMarginTrading":false}"""

    private fun order(number: String = "123", time: Long = epoch - 1000, status: String = "COMPLETED", asset: String = "USDT", fiat: String = "VES", total: String = "1000.25") =
        """{"orderNumber":"$number","tradeType":"SELL","createTime":$time,"orderStatus":"$status","asset":"$asset","fiat":"$fiat","unitPrice":"100.025","totalPrice":"$total","amount":"10"}"""

    private fun page(data: String = "", total: Int = 0) = BinanceResponse(200, """{"code":"000000","success":true,"total":$total,"data":[$data]}""")
    private fun normal(endpoint: BinanceEndpoint) = when(endpoint) {
        BinanceEndpoint.TIME -> BinanceResponse(200, """{"serverTime":$epoch}""")
        BinanceEndpoint.PERMISSIONS -> BinanceResponse(200, permissions)
        BinanceEndpoint.HISTORY -> page()
        BinanceEndpoint.FUNDING -> BinanceResponse(200, """[{"asset":"USDT","free":"12.34567890","locked":"99"}]""")
    }
    private fun client(transport: BinanceTransport) = BinanceClient(transport, now = { epoch + 30 * 60000 }, monotonic = { 1000 }, pause = {})

    @Test fun signatureMatchesKnownHmacVector() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", BinanceClient.signature("The quick brown fox jumps over the lazy dog", "key"))
    }
    @Test fun windowsHaveNoGapsOrOverlap() {
        val windows = BinanceClient.windows(0, 90L * 86400000 - 1)
        assertEquals(3, windows.size)
        assertTrue(windows.all { it.last - it.first < 30L * 86400000 })
        assertEquals(0L, windows.last().first)
        windows.zipWithNext().forEach { (newer, older) -> assertEquals(newer.first - 1, older.last) }
    }
    @Test fun rejectsWritePermissionsAndUnknownCapabilities() {
        for (field in listOf("enableWithdrawals", "enableInternalTransfer", "enableMargin", "enableFutures", "permitsUniversalTransfer", "enableVanillaOptions", "enableSpotAndMarginTrading", "enableNewTrading")) {
            val json = JsonParser.parseString(permissions).asJsonObject.apply { addProperty(field, true) }
            assertThrows(BinanceFailure::class.java) { BinanceClient.validatePermissions(json) }
        }
        assertThrows(BinanceFailure::class.java) { BinanceClient.validatePermissions(JsonParser.parseString("""{"enableReading":true}""").asJsonObject) }
    }
    @Test fun signsWithServerTimeNotSkewedPhoneAndReadsOnlyFundingFree() = runBlocking {
        val calls = mutableListOf<BinanceEndpoint>()
        val result = client { endpoint, query, key ->
            calls.add(endpoint)
            if (endpoint != BinanceEndpoint.TIME) {
                assertTrue(query.contains("timestamp=$epoch"))
                assertEquals(credentials.apiKey, key)
                assertEquals(BinanceClient.signature(query.substringBefore("&signature="), credentials.secret), query.substringAfter("&signature="))
                assertFalse(query.contains(credentials.secret))
            }
            normal(endpoint)
        }.read(credentials)
        assertEquals("12.34567890", result.fundingFreeUsdt)
        assertTrue(result.orders.isEmpty())
        assertEquals(BinanceEndpoint.PERMISSIONS, calls[1])
        assertEquals(BinanceEndpoint.FUNDING, calls.last())
    }
    @Test fun paginatesAndFiltersCompletedUsdtVesOnly() = runBlocking {
        var calls = 0
        val result = client { endpoint, _, _ ->
            if (endpoint == BinanceEndpoint.HISTORY) when (++calls) {
                1 -> page(order() + "," + order("124", status = "CANCELLED"), 4)
                2 -> page(order("125", fiat = "USD") + "," + order("126", asset = "BTC"), 4)
                else -> page()
            } else normal(endpoint)
        }.read(credentials)
        assertEquals(listOf("123"), result.orders.map { it.number })
        assertEquals("1000.25", result.orders.single().total)
        assertTrue(calls > 6)
    }
    @Test fun duplicatePageOrChangingTotalAborts() {
        for (changeTotal in listOf(false, true)) {
            var pages = 0
            assertThrows(BinanceFailure::class.java) { runBlocking {
                client { endpoint, _, _ ->
                    if (endpoint == BinanceEndpoint.HISTORY) { pages++; page(order(), if (pages > 1 && changeTotal) 3 else 2) }
                    else normal(endpoint)
                }.read(credentials)
            } }
        }
    }
    @Test fun malformedMoneyAndOutOfWindowDataAbort() {
        for (row in listOf(order(total = "NaN"), order(total = "-1"), order(time = epoch + 1))) {
            assertThrows(BinanceFailure::class.java) { runBlocking {
                client { endpoint, _, _ -> if (endpoint == BinanceEndpoint.HISTORY) page(row, 1) else normal(endpoint) }.read(credentials)
            } }
        }
    }
    @Test fun rateLimitStopsWithoutRetryAndBlocksNextAttempt() = runBlocking {
        var count = 0
        val client = client { _, _, _ -> count++; BinanceResponse(429, "{}", "60") }
        repeat(2) { assertThrows(BinanceFailure::class.java) { runBlocking { client.read(credentials) } } }
        assertEquals(1, count)
    }
    @Test fun transientNetworkHasTwoBoundedRetries() {
        var count = 0
        val waits = mutableListOf<Long>()
        val client = BinanceClient(BinanceTransport { _, _, _ -> count++; throw IOException("sensitive request") }, pause = { waits.add(it) })
        val error = assertThrows(BinanceFailure::class.java) { runBlocking { client.read(credentials) } }
        assertEquals(3, count)
        assertEquals(listOf(1000L, 4000L), waits)
        assertFalse(error.toString().contains("sensitive"))
    }
    @Test fun clockErrorResyncsOnceAndAuthenticationErrorDoesNotRetry() = runBlocking {
        var time = 0
        var permission = 0
        client { endpoint, _, _ ->
            if (endpoint == BinanceEndpoint.TIME) time++
            if (endpoint == BinanceEndpoint.PERMISSIONS && permission++ == 0) BinanceResponse(400, """{"code":-1021}""") else normal(endpoint)
        }.read(credentials)
        assertEquals(2, time)
        var calls = 0
        val error = assertThrows(BinanceFailure::class.java) { runBlocking {
            client { endpoint, _, _ ->
                if (endpoint == BinanceEndpoint.PERMISSIONS) { calls++; BinanceResponse(401, """{"code":-2015,"msg":"PRIVATE"}""") } else normal(endpoint)
            }.read(credentials)
        } }
        assertEquals(1, calls)
        assertFalse(error.message!!.contains("PRIVATE"))
    }
    @Test fun secretsDoNotHavePrintableRepresentationAndRejectWhitespace() {
        assertFalse(credentials.toString().contains(credentials.secret))
        assertFalse(credentials.toString().contains(credentials.apiKey))
        assertThrows(IllegalArgumentException::class.java) { BinanceCredentials("x\n".repeat(32), "s".repeat(64)) }
    }
}
