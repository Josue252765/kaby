package com.mofoxx.kaby.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import com.mofoxx.kaby.data.BitacoraEntity
import com.mofoxx.kaby.data.ActaCasillaEntity
import com.mofoxx.kaby.data.CuadreRepository
import com.mofoxx.kaby.data.TasaBcvEntity
import com.mofoxx.kaby.data.KabySnapshot
import com.mofoxx.kaby.data.MovimientoEntity
import com.mofoxx.kaby.data.VentaP2pEntity
import com.mofoxx.kaby.tasas.TasaFetchResult
import com.mofoxx.kaby.tasas.TasaPropuesta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KabyUiState(
    val snapshot: KabySnapshot? = null,
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val message: String? = null,
    val proposal: TasaPropuesta? = null,
    val auditEntries: List<BitacoraEntity> = emptyList(),
    val m1ChecklistEntries: List<ActaCasillaEntity> = emptyList(),
)

private data class SnapshotRead(val snapshot: KabySnapshot? = null, val failed: Boolean = false)

class KabyViewModel(private val repository: CuadreRepository,
    private val binanceClient: com.mofoxx.kaby.binance.BinanceClient = com.mofoxx.kaby.binance.BinanceClient(),
) : ViewModel() {
    val binanceReads = repository.binanceReads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _binanceStatus = MutableStateFlow(BinanceUiStatus())
    val binanceStatus: StateFlow<BinanceUiStatus> = _binanceStatus
    private var binanceJob: Job? = null

    fun readBinance(key: String, secret: String) {
        if (binanceJob?.isActive == true) return
        val credentials = try { com.mofoxx.kaby.binance.BinanceCredentials(key, secret) }
        catch (e: IllegalArgumentException) {
            _binanceStatus.value = BinanceUiStatus(detail = e.message.orEmpty())
            return
        }
        _binanceStatus.value = BinanceUiStatus(busy = true, detail = "Conectando con Binance…")
        binanceJob = viewModelScope.launch {
            try {
                val read = binanceClient.read(credentials) { _binanceStatus.value = BinanceUiStatus(true, it) }
                repository.storeBinanceRead(read)
                _binanceStatus.value = BinanceUiStatus(detail = "Lectura guardada: ${read.orders.size} ventas completadas USDT/VES. Revisa las pendientes antes de sumarlas a Mercantil.")
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                _binanceStatus.value = BinanceUiStatus(detail = "E-BINANCE-TIEMPO · La consulta excedió tres minutos. Conservamos la lectura anterior.")
            } catch (cancelled: CancellationException) {
                _binanceStatus.value = BinanceUiStatus(detail = "Consulta cancelada. Revisa la fecha de la última lectura guardada.")
                throw cancelled
            } catch (e: com.mofoxx.kaby.binance.BinanceFailure) {
                _binanceStatus.value = BinanceUiStatus(detail = e.safeMessage)
            } catch (_: Exception) {
                // Never serialize exception bodies, requests, credentials or upstream messages.
                _binanceStatus.value = BinanceUiStatus(detail = "E-BINANCE-LECTURA · No pude validar o guardar la consulta completa. Se conserva la lectura anterior.")
            }
        }
    }

    fun cancelBinance() { binanceJob?.cancel() }

    suspend fun createPrivateBackup(context: android.content.Context): Boolean =
        com.mofoxx.kaby.backup.PrivateBackup.write(context, repository)

    fun reconcileBinance(number: String, net: String, manualId: Long?, done: (Boolean) -> Unit) =
        action(guardEntrySave = true, onComplete = done) {
            repository.reconcileBinanceOrder(number, net, manualId)
            "Venta conciliada una sola vez. Revisa el cuadre de Mercantil."
        }
    private val message = MutableStateFlow<String?>(null)
    private val proposal = MutableStateFlow<TasaPropuesta?>(null)
    private val auditEntries = MutableStateFlow<List<BitacoraEntity>>(emptyList())
    private val _isSavingEntry = MutableStateFlow(false)
    private val _isFetchingRate = MutableStateFlow(false)
    private var rateFetchJob: Job? = null
    val isSavingEntry: StateFlow<Boolean> = _isSavingEntry
    val isFetchingRate: StateFlow<Boolean> = _isFetchingRate

    private val snapshotRead = repository.snapshots
        .map { SnapshotRead(snapshot = it) }
        .onStart { emit(SnapshotRead()) }
        .catch { emit(SnapshotRead(failed = true)) }
    val state: StateFlow<KabyUiState> = combine(snapshotRead, message, proposal, auditEntries,
        repository.m1ChecklistEntries.catch { emit(emptyList()) }) { read, text, rateProposal, audit, checklist ->
        KabyUiState(snapshot = read.snapshot, loading = read.snapshot == null && !read.failed,
            loadError = read.failed, message = text, proposal = rateProposal,
            auditEntries = audit, m1ChecklistEntries = checklist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KabyUiState())

    fun saveAnchor(amount: String, localDateTime: String) = action {
        repository.saveInitialAnchor(amount, toCaracasIso(localDateTime))
        "Ancla guardada con su fecha y hora."
    }

    fun reconcile(realBalance: String, localDateTime: String, expectedCalculated: String) = action {
        repository.reconcile(realBalance, toCaracasIso(localDateTime), expectedCalculated)
        "Cuadre actualizado. Revisa el estado y la diferencia que aparece en pantalla."
    }

    fun saveMovement(
        id: Long? = null,
        direction: String,
        nature: String = "NORMAL",
        currency: String,
        amount: String,
        dateTime: String,
        category: String,
        note: String,
        thirdParty: String,
        onComplete: (Boolean) -> Unit = {},
    ) = action(guardEntrySave = true, onComplete = onComplete) {
        repository.saveMovement(
            id = id,
            direction = direction,
            nature = nature,
            currency = currency,
            amountText = amount,
            occurredAt = toCaracasIso(dateTime),
            category = category,
            note = note,
            thirdParty = thirdParty,
        )
        if (id == null) "Movimiento guardado." else "Movimiento editado; el cambio quedó en el rastro."
    }

    fun saveSale(
        id: Long? = null,
        netBs: String,
        p2pRate: String,
        usdt: String,
        dateTime: String,
        provenance: String,
        onComplete: (Boolean) -> Unit = {},
    ) = action(guardEntrySave = true, onComplete = onComplete) {
        repository.saveManualSale(
            id = id,
            netBsText = netBs,
            p2pRateText = p2pRate,
            usdtText = usdt,
            occurredAt = toCaracasIso(dateTime),
            provenance = provenance,
        )
        if (id == null) "Venta guardada." else "Venta editada; el cambio quedó en el rastro."
    }

    fun saveManualRate(value: String, dateTime: String) = action {
        repository.saveManualRate(value, toCaracasIso(dateTime))
        "Tasa guardada y confirmada por ti."
    }

    fun fetchRate() {
        if (rateFetchJob?.isActive == true) return
        rateFetchJob = viewModelScope.launch {
            _isFetchingRate.value = true
            message.value = null
            proposal.value = null
            try {
                when (val result = repository.proposeRate()) {
                    is TasaFetchResult.Success -> {
                        proposal.value = result.proposal
                    }
                    TasaFetchResult.NoNetwork, TasaFetchResult.Unavailable -> {
                        message.value = rateFetchErrorMessage(result, state.value.snapshot?.latestConfirmedRate).orEmpty()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                runCatching { repository.logDatabaseFailure() }
                message.value = rateFetchErrorMessage(TasaFetchResult.Unavailable, state.value.snapshot?.latestConfirmedRate).orEmpty()
            } finally {
                _isFetchingRate.value = false
            }
        }
    }

    fun confirmProposal() {
        val current = proposal.value ?: return
        action {
            repository.confirmProposal(current)
            proposal.value = null
            "Tasa del espejo confirmada por ti. Conservamos la hora que publicó la fuente."
        }
    }

    fun loadAudit(table: String, id: Long) {
        viewModelScope.launch { auditEntries.value = repository.auditFor(table, id) }
    }

    fun recordM1ChecklistResult(caseId: String, verdict: String, cause: String, evidence: String) = action {
        repository.recordM1ChecklistResult(caseId, verdict, cause, evidence)
        "Resultado de la casilla guardado con fecha y hora."
    }

    fun sealM1ByOwner() = action {
        repository.sealM1ByOwner()
        "Tu confirmación local de M1 quedó registrada."
    }

    fun clearMessage() { message.value = null }

    private fun action(
        guardEntrySave: Boolean = false,
        onComplete: (Boolean) -> Unit = {},
        block: suspend () -> String,
    ) {
        if (guardEntrySave && _isSavingEntry.value) return
        if (guardEntrySave) _isSavingEntry.value = true
        viewModelScope.launch {
            val succeeded = try {
                message.value = block()
                true
            } catch (error: IllegalArgumentException) {
                message.value = error.message ?: "No pude guardar ese registro. Revisa los datos e inténtalo otra vez."
                false
            } catch (error: java.time.DateTimeException) {
                message.value = "La fecha debe usar el formato AAAA-MM-DD HH:MM en hora de Caracas."
                false
            } catch (_: Exception) {
                runCatching { repository.logDatabaseFailure() }
                message.value = "No pude confirmar el guardado en este teléfono. Revisa la lista antes de volver a intentarlo; tus datos escritos siguen en los campos."
                false
            } finally {
                if (guardEntrySave) _isSavingEntry.value = false
            }
            onComplete(succeeded)
        }
    }

    private fun toCaracasIso(value: String): String = java.time.LocalDateTime
        .parse(value.trim(), DATE_TIME_FORMAT)
        .atZone(java.time.ZoneId.of("America/Caracas"))
        .toOffsetDateTime()
        .toString()

    companion object {
        val DATE_TIME_FORMAT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}

data class BinanceUiStatus(val busy: Boolean = false, val detail: String = "Sin consulta en esta sesión.")

internal fun rateFetchErrorMessage(result: TasaFetchResult, latestConfirmedRate: TasaBcvEntity?): String? {
    val fallback = latestConfirmedRate?.let {
        "La última tasa confirmada conserva su hora publicada ${it.horaFuente}; sigue visible en Tasas."
    } ?: "No hay una tasa confirmada previa."
    return when (result) {
        TasaFetchResult.NoNetwork -> "E-SIN-RED · No pude consultar el espejo. Tus registros siguen disponibles. $fallback Revisa la conexión o ingresa la tasa a mano."
        TasaFetchResult.Unavailable -> "E-TASAS-SIN-FUENTE · El espejo no entregó una tasa oficial válida. $fallback Puedes ingresar la tasa de tu tablero a mano."
        is TasaFetchResult.Success -> null
    }
}

class KabyViewModelFactory(private val repository: CuadreRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(KabyViewModel::class.java))
        return KabyViewModel(repository) as T
    }
}
