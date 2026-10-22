package com.mofoxx.kaby.binance

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Deliberately not a data class: credentials must never appear in toString/diagnostics. */
class BinanceCredentials(val apiKey: String, val secret: String) {
    init {
        require(apiKey.matches(Regex("[A-Za-z0-9]{32,128}")) && secret.matches(Regex("[A-Za-z0-9]{32,128}"))) {
            "Usa una llave HMAC generada por Binance, completa y sin espacios."
        }
    }
    override fun toString() = "BinanceCredentials([REDACTED])"
}

class BinanceFailure(val safeMessage: String) : Exception(safeMessage)
data class BinanceOrder(val number: String, val time: Long, val price: String, val total: String, val amount: String)
data class BinanceRead(val orders: List<BinanceOrder>, val fundingFreeUsdt: String, val readAt: Long, val from: Long, val to: Long)
data class BinanceResponse(val status: Int, val body: String, val retryAfter: String? = null)

enum class BinanceEndpoint(val method: String, val path: String) {
    TIME("GET", "/api/v3/time"),
    PERMISSIONS("GET", "/sapi/v1/account/apiRestrictions"),
    HISTORY("GET", "/sapi/v1/c2c/orderMatch/listUserOrderHistory"),
    FUNDING("POST", "/sapi/v1/asset/get-funding-asset"), // Query only, per Binance's USER_DATA contract.
}

fun interface BinanceTransport {
    suspend fun request(endpoint: BinanceEndpoint, query: String, apiKey: String?): BinanceResponse
}

class OkHttpBinanceTransport : BinanceTransport {
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).build()

    override suspend fun request(endpoint: BinanceEndpoint, query: String, apiKey: String?): BinanceResponse =
        suspendCancellableCoroutine { continuation ->
            val builder = Request.Builder().url("https://api.binance.com${endpoint.path}" +
                if (endpoint.method == "GET" && query.isNotEmpty()) "?$query" else "")
            if (apiKey != null) builder.header("X-MBX-APIKEY", apiKey)
            if (endpoint.method == "POST") builder.post(query.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            val call = client.newCall(builder.build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("Binance network unavailable"))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            // A malformed upstream must not allocate an unbounded response in this phone.
                            val bytes = it.body.byteStream().use { input ->
                                val output = java.io.ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (output.size() <= 2_000_000) {
                                    val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray()
                            }
                            if (bytes.size > 2_000_000) throw BinanceFailure("E-BINANCE-DATOS · Respuesta demasiado grande; no se guardó esta lectura.")
                            if (continuation.isActive) continuation.resume(BinanceResponse(it.code, bytes.toString(Charsets.UTF_8), it.header("Retry-After")))
                        }
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(BinanceFailure("E-BINANCE-DATOS · No se pudo leer la respuesta. Conservamos los datos anteriores."))
                    }
                }
            })
        }
}

class BinanceClient(
    private val transport: BinanceTransport = OkHttpBinanceTransport(),
    private val now: () -> Long = System::currentTimeMillis,
    private val monotonic: () -> Long = { System.nanoTime() / 1_000_000 },
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    private var blockedUntil = 0L

    suspend fun read(credentials: BinanceCredentials, progress: (String) -> Unit = {}): BinanceRead = withTimeout(180_000) {
        if (monotonic() < blockedUntil) throw BinanceFailure("E-BINANCE-LIMITE · Binance pidió esperar. Inténtalo más tarde.")
        var serverBase = 0L
        var tickBase = 0L
        suspend fun syncClock() {
            val start = monotonic()
            serverBase = json(raw(BinanceEndpoint.TIME, "", null)).asJsonObject.get("serverTime").asLong
            tickBase = monotonic()
            serverBase += (tickBase - start) / 2
        }
        suspend fun signed(endpoint: BinanceEndpoint, params: String): JsonElement {
            repeat(2) { attempt ->
                val query = (if (params.isEmpty()) "" else "$params&") + "recvWindow=5000&timestamp=${serverBase + monotonic() - tickBase}"
                val response = raw(endpoint, "$query&signature=${signature(query, credentials.secret)}", credentials.apiKey)
                val code = runCatching { JsonParser.parseString(response.body).asJsonObject.get("code")?.asString }.getOrNull()
                if (code == "-1021" && attempt == 0) syncClock() else return json(response)
            }
            throw BinanceFailure("E-BINANCE-RELOJ · No se pudo ajustar la hora con Binance.")
        }
        progress("Comprobando hora y permisos…")
        syncClock()
        validatePermissions(signed(BinanceEndpoint.PERMISSIONS, "").asJsonObject)
        val end = serverBase + monotonic() - tickBase
        // Margin avoids a moving six-month cutoff during a multi-page read.
        val start = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).minusMonths(6).plusMinutes(5).toInstant().toEpochMilli()
        val orders = linkedMapOf<String, BinanceOrder>()
        val windows = windows(start, end)
        for ((index, window) in windows.withIndex()) {
            var page = 1
            var received = 0
            var expected: Int? = null
            val seen = mutableSetOf<String>()
            while (true) {
                progress("Historial ${index + 1}/${windows.size} · página $page")
                if (page > 100) throw BinanceFailure("E-BINANCE-LIMITE · Historial muy grande; no se guardó una lectura incompleta.")
                val result = signed(BinanceEndpoint.HISTORY, "tradeType=SELL&startTimestamp=${window.first}&endTimestamp=${window.last}&page=$page&rows=100").asJsonObject
                if (result.get("success")?.asBoolean != true || result.get("code")?.asString != "000000") malformed()
                val data = result.getAsJsonArray("data") ?: malformed()
                val total = result.get("total")?.asInt ?: malformed()
                if (total < 0 || total > 10000 || (expected != null && expected != total) || data.size() > 100) malformed()
                expected = total
                for (element in data) {
                    val row = element.asJsonObject
                    val number = row.get("orderNumber")?.asString ?: malformed()
                    if (!seen.add(number)) malformed()
                    if (row.get("tradeType")?.asString != "SELL") malformed()
                    val time = row.get("createTime")?.asLong ?: malformed()
                    if (time !in window) malformed()
                    if (row.get("orderStatus")?.asString == "COMPLETED" && row.get("asset")?.asString == "USDT" && row.get("fiat")?.asString == "VES") {
                        if (!number.matches(Regex("[0-9]{1,40}"))) malformed()
                        val order = BinanceOrder(number, time, decimal(row, "unitPrice", true), decimal(row, "totalPrice", true), decimal(row, "amount", true))
                        if (orders.put(number, order)?.let { it != order } == true) malformed()
                    }
                }
                received += data.size()
                if (received == total) break
                if (received > total || data.size() == 0) malformed()
                page++
            }
        }
        progress("Leyendo USDT disponible en Funding…")
        val funding = signed(BinanceEndpoint.FUNDING, "asset=USDT&needBtcValuation=false").asJsonArray
        if (funding.size() > 1 || funding.any { it.asJsonObject.get("asset")?.asString != "USDT" }) malformed()
        val free = funding.firstOrNull()?.asJsonObject?.let { decimal(it, "free", false) } ?: "0"
        BinanceRead(orders.values.toList(), free, serverBase + monotonic() - tickBase, start, end)
    }

    private suspend fun raw(endpoint: BinanceEndpoint, query: String, key: String?): BinanceResponse {
        repeat(3) { attempt ->
            try {
                val response = transport.request(endpoint, query, key)
                if (response.status == 429 || response.status == 418) {
                    val seconds = (response.retryAfter?.toLongOrNull() ?: runCatching {
                        val date = java.time.ZonedDateTime.parse(response.retryAfter, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                        (date.toInstant().toEpochMilli() - now() + 999) / 1000
                    }.getOrNull() ?: 120L).coerceAtLeast(1)
                    blockedUntil = runCatching { Math.addExact(monotonic(), Math.multiplyExact(seconds, 1000L)) }.getOrDefault(Long.MAX_VALUE)
                    throw BinanceFailure("E-BINANCE-LIMITE · Binance pidió esperar $seconds segundos. No se modificaron tus registros.")
                }
                if (response.status >= 500 && attempt < 2) pause(if (attempt == 0) 1000 else 4000) else return response
            } catch (_: IOException) {
                if (attempt == 2) throw BinanceFailure("E-BINANCE-RED · No pude llegar a Binance. Conservamos la última lectura con su hora.")
                pause(if (attempt == 0) 1000 else 4000)
            }
        }
        error("unreachable")
    }

    private fun json(response: BinanceResponse): JsonElement {
        val parsed = runCatching { JsonParser.parseString(response.body) }.getOrNull() ?: malformed()
        val code = if (parsed.isJsonObject) parsed.asJsonObject.get("code")?.asString else null
        if (code == "-1021") throw BinanceFailure("E-BINANCE-RELOJ · La hora no concuerda con Binance después de ajustarla.")
        if (code == "-1022") throw BinanceFailure("E-BINANCE-FIRMA · Revisa que el SECRET esté completo y corresponda a esa llave.")
        if (response.status == 401 || code in listOf("-2014", "-2015")) throw BinanceFailure("E-BINANCE-LLAVE · Binance rechazó la llave, sus permisos o la IP. Revisa API Management.")
        if (response.status !in 200..299 || (code != null && code != "000000")) throw BinanceFailure("E-BINANCE-SERVICIO · Binance no autorizó o no completó esta consulta. Conservamos la lectura anterior.")
        return parsed
    }

    companion object {
        fun signature(query: String, secret: String): String = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            doFinal(query.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
        fun windows(from: Long, to: Long): List<LongRange> {
            require(from >= 0 && to >= from)
            val result = mutableListOf<LongRange>()
            var end = to
            while (end >= from) {
                val start = maxOf(from, end - 30L * 86400000 + 1)
                result.add(start..end)
                end = start - 1
            }
            return result
        }
        fun validatePermissions(value: JsonObject) {
            val required = listOf("enableWithdrawals", "enableInternalTransfer", "enableMargin", "enableFutures", "permitsUniversalTransfer", "enableVanillaOptions", "enableSpotAndMarginTrading", "enableFixApiTrade", "enablePortfolioMarginTrading")
            if (value.get("enableReading")?.asBoolean != true || required.any { value.get(it)?.asBoolean != false } ||
                value.entrySet().any { (name, flag) -> (name.startsWith("enable") || name.startsWith("permits")) && name !in listOf("enableReading", "enableFixReadOnly") && (!flag.isJsonPrimitive || !flag.asJsonPrimitive.isBoolean || flag.asBoolean) }) {
                throw BinanceFailure("E-BINANCE-PERMISOS · Usa una llave exclusivamente de lectura, sin operaciones, retiros ni transferencias.")
            }
        }
        private fun decimal(row: JsonObject, name: String, positive: Boolean): String {
            val text = row.get(name)?.asString ?: malformed()
            if (!text.matches(Regex("[0-9]{1,20}(\\.[0-9]{1,18})?"))) malformed()
            val value = BigDecimal(text)
            if (positive && value.signum() <= 0) malformed()
            return value.toPlainString()
        }
        private fun malformed(): Nothing = throw BinanceFailure("E-BINANCE-DATOS · La respuesta está incompleta o cambió durante la lectura. No se guardó parcialmente.")
    }
}
