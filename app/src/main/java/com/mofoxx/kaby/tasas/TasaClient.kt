package com.mofoxx.kaby.tasas

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

data class TasaPropuesta(
    val valor: BigDecimal,
    val horaFuente: OffsetDateTime,
    val proveedor: String,
)

sealed interface TasaFetchResult {
    data class Success(val proposal: TasaPropuesta) : TasaFetchResult
    data object NoNetwork : TasaFetchResult
    data object Unavailable : TasaFetchResult
}

fun interface TasaClient {
    suspend fun fetchOfficialMirror(): TasaFetchResult
}

private interface DolarApiService {
    @GET("v1/dolares")
    suspend fun rates(): JsonElement
}

class DolarApiTasaClient : TasaClient {
    private val service: DolarApiService by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        Retrofit.Builder()
            .baseUrl("https://ve.dolarapi.com/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DolarApiService::class.java)
    }

    override suspend fun fetchOfficialMirror(): TasaFetchResult = withContext(Dispatchers.IO) {
        var sawInternetFailure = false
        repeat(3) { attempt ->
            try {
                val result = parse(service.rates())
                if (result != null) return@withContext TasaFetchResult.Success(result)
                return@withContext TasaFetchResult.Unavailable
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: java.io.IOException) {
                sawInternetFailure = true
                if (attempt < 2) delay(400L * (attempt + 1))
            } catch (error: HttpException) {
                val isTransient = error.code() == 429 || error.code() >= 500
                if (isTransient && attempt < 2) delay(400L * (attempt + 1))
                else return@withContext TasaFetchResult.Unavailable
            } catch (_: Exception) {
                return@withContext TasaFetchResult.Unavailable
            }
        }
        if (sawInternetFailure) TasaFetchResult.NoNetwork else TasaFetchResult.Unavailable
    }

    internal fun parse(root: JsonElement): TasaPropuesta? {
        val rows = if (root.isJsonArray) root.asJsonArray.mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
        else listOfNotNull(root.takeIf(JsonElement::isJsonObject)?.asJsonObject)
        val official = rows.firstOrNull { it.string("fuente")?.equals("oficial", ignoreCase = true) == true }
            ?: return null
        val currency = official.string("moneda")
        if (currency != null && !currency.equals("USD", ignoreCase = true)) return null
        val average = official.string("promedio")?.let {
            runCatching { BigDecimal(it) }.getOrNull()
        } ?: return null
        if (average <= BigDecimal.ZERO) return null
        val timestamp = official.string("fechaActualizacion") ?: return null
        val sourceTime = runCatching { OffsetDateTime.parse(timestamp) }.getOrNull() ?: return null
        return TasaPropuesta(average, sourceTime, "DOLARAPI")
    }

    private fun JsonObject.string(key: String): String? {
        val element = get(key)?.takeUnless { it.isJsonNull } ?: return null
        return runCatching { element.asString }.getOrNull()
    }
}
