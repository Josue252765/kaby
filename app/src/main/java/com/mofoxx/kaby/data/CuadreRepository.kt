package com.mofoxx.kaby.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.mofoxx.kaby.domain.CuadreCalculator
import com.mofoxx.kaby.domain.CuadreInput
import com.mofoxx.kaby.domain.MovementForCalculation
import com.mofoxx.kaby.domain.SaleForCalculation
import com.mofoxx.kaby.domain.ReconciliationMath
import com.mofoxx.kaby.domain.ConfirmedRateCandidate
import com.mofoxx.kaby.domain.RateLookup
import com.mofoxx.kaby.domain.RateSnapshotPolicy
import com.mofoxx.kaby.domain.StoredRateSnapshot
import com.mofoxx.kaby.domain.MovementPolicy
import com.mofoxx.kaby.domain.P2pPremium
import com.mofoxx.kaby.domain.M1AcceptanceChecklist
import com.mofoxx.kaby.tasas.TasaClient
import com.mofoxx.kaby.tasas.TasaFetchResult
import com.mofoxx.kaby.tasas.TasaPropuesta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneId

data class KabySnapshot(
    val account: EstadoCuentaEntity?,
    val movements: List<MovimientoEntity>,
    val sales: List<VentaP2pEntity>,
    val rates: List<TasaBcvEntity>,
) {
    val summary = CuadreCalculator.calculate(
        CuadreInput(
            anchorAmount = account?.anclaMonto,
            anchorAt = account?.anclaFechaHora,
            movements = movements.map {
                MovementForCalculation(it.tipo, it.naturaleza, it.moneda, it.monto, it.fechaHora, it.tasaBcvAlMomento)
            },
            sales = sales.map {
                SaleForCalculation(it.bsNetos, it.usdtCantidad, it.fechaHora, it.tasaBcvAlMomento, it.estado)
            },
            lastReconciliationState = account?.estadoCuadre ?: "SIN_ANCLA",
            discrepancy = account?.cifraDescuadre,
        ),
    )
    val latestConfirmedRate = rates
        .filter { it.confirmadaPor == "HUMANO" }
        .maxWithOrNull(compareBy<TasaBcvEntity> { CuadreCalculator.instantOrNull(it.horaFuente) ?: java.time.Instant.MIN }.thenBy { it.id })
}

class CuadreRepository(
    private val database: KabyDatabase,
    private val tasaClient: TasaClient,
    private val gson: Gson = Gson(),
) {
    private val dao = database.kabyDao()
    val snapshots: Flow<KabySnapshot> = combine(
        dao.observeAccount(), dao.observeMovements(), dao.observeSales(), dao.observeRates(),
    ) { account, movements, sales, rates -> KabySnapshot(account, movements, sales, rates) }
    val m1ChecklistEntries: Flow<List<ActaCasillaEntity>> = dao.observeM1ChecklistEntries()
    val binanceReads = dao.observeBinanceReads()

    suspend fun storeBinanceRead(read: com.mofoxx.kaby.binance.BinanceRead) {
        require(read.from <= read.to && read.to <= read.readAt)
        require(BigDecimal(read.fundingFreeUsdt).signum() >= 0)
        require(read.orders.map { it.number }.distinct().size == read.orders.size)
        val at = java.time.Instant.ofEpochMilli(read.readAt).atOffset(java.time.ZoneOffset.UTC).toString()
        database.withTransaction {
            for (order in read.orders) {
                require(order.time in read.from..read.to)
                require(listOf(order.price, order.total, order.amount).all { BigDecimal(it).signum() > 0 })
                val row = LecturaBinanceEntity(order.number, "SELL",
                    java.time.Instant.ofEpochMilli(order.time).atOffset(java.time.ZoneOffset.UTC).toString(),
                    order.price, order.total, order.amount, at, "${read.from}..${read.to}")
                val prior = dao.binanceRead(order.number)
                // A completed order changing monetary fields must be reviewed, never overwritten silently.
                require(prior == null || (prior.fechaHoraOrden == row.fechaHoraOrden &&
                    BigDecimal(prior.unitPrice).compareTo(BigDecimal(row.unitPrice)) == 0 &&
                    BigDecimal(prior.totalPrice).compareTo(BigDecimal(row.totalPrice)) == 0 &&
                    BigDecimal(prior.amount).compareTo(BigDecimal(row.amount)) == 0)) { "Una orden completada cambió en Binance; conserva la lectura anterior y revisa su número." }
                dao.saveBinanceRead(row)
            }
            val account = dao.accountNow() ?: EstadoCuentaEntity()
            dao.saveAccount(account.copy(saldoUsdt = read.fundingFreeUsdt, leidoEn = at))
        }
    }

    /** Human reconciliation preserves the bank net amount and explicitly links a manual bridge. */
    suspend fun reconcileBinanceOrder(number: String, netText: String, manualSaleId: Long?) {
        val net = positive(netText, "Confirma los bolívares netos recibidos en Mercantil.")
        database.withTransaction {
            require(dao.saleByOrder(number) == null) { "Esa orden ya está conciliada; no se volvió a sumar." }
            val read = requireNotNull(dao.binanceRead(number)) { "No encontré esa lectura de Binance." }
            val manual = manualSaleId?.let { requireNotNull(dao.sale(it)) }
            require(manual == null || (manual.fuente == "MANO" && manual.estado == "VIVA")) { "El puente seleccionado ya no está activo." }
            require(manual == null || (BigDecimal(manual.usdtCantidad).compareTo(BigDecimal(read.amount)) == 0 && BigDecimal(manual.bsNetos).compareTo(BigDecimal(net)) == 0)) {
                "El puente debe coincidir en USDT y Bs netos. Corrige el registro manual antes de vincularlo."
            }
            val rate = rateAt(read.fechaHoraOrden)
            val now = nowIso()
            val sale = VentaP2pEntity(fechaHora = read.fechaHoraOrden, bsNetos = net, tasaP2p = read.unitPrice,
                usdtCantidad = read.amount, tasaBcvAlMomento = rate?.valor, horaTasaBcv = rate?.horaFuente,
                prima = rate?.let { P2pPremium.calculate(net, read.amount, it.valor) },
                estadoValuacion = if (rate == null) "SIN VALUAR" else "VALUADA",
                proveniencia = "Binance · neto Mercantil confirmado por el propietario", orderNumber = number, fuente = "API",
                timestampCreacion = now, timestampUltimaEdicion = now)
            if (manual != null) {
                val retired = manual.copy(estado = "JUBILADA", timestampUltimaEdicion = now)
                dao.updateSale(retired)
                dao.insertAudit(BitacoraEntity(tabla = "ventas_p2p_mano", filaId = manual.id, accion = "EDITAR", antes = gson.toJson(manual), despues = gson.toJson(retired), timestamp = now))
            }
            val id = dao.insertSale(sale)
            dao.insertAudit(BitacoraEntity(tabla = "ventas_p2p_mano", filaId = id, accion = "CREAR", despues = gson.toJson(sale.copy(id = id)), timestamp = now))
            if (isAtOrBeforeLastClose(read.fechaHoraOrden) || (manual != null && isAtOrBeforeLastClose(manual.fechaHora))) {
                val anchor = dao.accountNow()?.anclaFechaHora
                val delta = saleImpact(sale, anchor).subtract(manual?.let { saleImpact(it, anchor) } ?: BigDecimal.ZERO)
                if (delta.signum() != 0) reopenWithDiscrepancy(delta)
            }
        }
    }

    suspend fun recordM1ChecklistResult(caseId: String, verdict: String, cause: String, evidence: String) {
        require(M1AcceptanceChecklist.cases.any { it.id == caseId }) { "La casilla de M1 no existe." }
        M1AcceptanceChecklist.validationError(verdict, cause, evidence)?.let { throw IllegalArgumentException(it) }
        dao.insertChecklistEntry(
            ActaCasillaEntity(
                hito = M1AcceptanceChecklist.MILESTONE,
                casillaId = caseId,
                veredicto = verdict,
                causaBloqueo = cause.trim().ifBlank { null },
                evidencia = evidence.trim().ifBlank { null },
                timestamp = nowIso(),
            ),
        )
    }

    suspend fun sealM1ByOwner() {
        database.withTransaction {
            val entries = dao.allChecklistEntries().filter { it.hito == M1AcceptanceChecklist.MILESTONE }
            require(M1AcceptanceChecklist.isReadyToSeal(entries)) {
                "No se puede confirmar M1: todas las casillas deben tener PASS y evidencia."
            }
            require(!M1AcceptanceChecklist.isSealed(entries)) { "M1 ya está confirmado en este teléfono." }
            dao.insertChecklistEntry(
                ActaCasillaEntity(
                    hito = M1AcceptanceChecklist.MILESTONE,
                    casillaId = M1AcceptanceChecklist.OWNER_CONFIRMATION_ID,
                    veredicto = "PASS",
                    evidencia = "Confirmación del propietario en la pantalla de M1.",
                    timestamp = nowIso(),
                ),
            )
        }
    }

    suspend fun saveInitialAnchor(amountText: String, at: String) {
        val amount = positiveOrZero(amountText, "El ancla no puede ser negativa.")
        validateOccurredAt(at)
        database.withTransaction {
            val current = dao.accountNow()
            require(current?.anclaMonto == null) { "El ancla ya está puesta." }
            val now = nowIso()
            val anchor = (current ?: EstadoCuentaEntity()).copy(anclaMonto = amount, anclaFechaHora = at, estadoCuadre = "CALCULADO")
            dao.saveAccount(anchor)
            dao.insertAudit(BitacoraEntity(tabla = "estado_cuentas", filaId = 1, accion = "CREAR", despues = gson.toJson(anchor), timestamp = now))
        }
    }

    suspend fun reconcile(realBalanceText: String, now: String = nowIso(), expectedCalculatedText: String? = null) {
        val realBalance = positiveOrZero(realBalanceText, "El saldo real no puede ser negativo.")
        validateOccurredAt(now)
        val observedInstant = requireNotNull(CuadreCalculator.instantOrNull(now))
        database.withTransaction {
            val account = dao.accountNow() ?: EstadoCuentaEntity()
            if (account.anclaMonto == null) {
                val anchored = account.copy(anclaMonto = realBalance, anclaFechaHora = now, estadoCuadre = "CALCULADO")
                dao.saveAccount(anchored)
                dao.insertAudit(BitacoraEntity(tabla = "estado_cuentas", filaId = 1, accion = "CREAR", despues = gson.toJson(anchored), timestamp = now))
            } else {
                val anchorInstant = requireNotNull(CuadreCalculator.instantOrNull(account.anclaFechaHora))
                require(!observedInstant.isBefore(anchorInstant)) { "El cuadre no puede ser anterior al ancla." }
                account.ultimoCuadreAt?.let { lastCut ->
                    val lastInstant = requireNotNull(CuadreCalculator.instantOrNull(lastCut))
                    require(!observedInstant.isBefore(lastInstant)) {
                        "El corte no puede ser anterior a la última comparación guardada."
                    }
                }
                val movements = dao.allMovements().filter {
                    CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(observedInstant) == false
                }
                val sales = dao.allSales().filter {
                    CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(observedInstant) == false
                }
                val summary = calculate(account, movements, sales)
                val calculated = summary.mercantilBs ?: BigDecimal.ZERO
                if (expectedCalculatedText != null) {
                    require(calculated.compareTo(BigDecimal(expectedCalculatedText)) == 0) {
                        "Los registros cambiaron durante la revisión. Compara de nuevo antes de confirmar."
                    }
                }
                val discrepancy = ReconciliationMath.againstObserved(BigDecimal(realBalance), calculated)
                val updated = account.copy(
                        ultimoCuadreAt = now,
                        estadoCuadre = if (discrepancy.signum() == 0) "CALCULADO" else "DESCUADRE_ABIERTO",
                        cifraDescuadre = if (discrepancy.signum() == 0) null else discrepancy.toPlainString(),
                    )
                dao.saveAccount(updated)
                dao.insertAudit(BitacoraEntity(tabla = "estado_cuentas", filaId = 1, accion = "EDITAR", antes = gson.toJson(account), despues = gson.toJson(updated), timestamp = now))
            }
        }
    }

    suspend fun saveMovement(
        id: Long? = null,
        direction: String,
        nature: String = "NORMAL",
        currency: String,
        amountText: String,
        occurredAt: String,
        category: String,
        note: String?,
        thirdParty: String?,
    ) {
        val amount = positive(amountText, "El monto debe ser mayor que cero.")
        MovementPolicy.validate(direction, nature, currency, BigDecimal(amount), category, thirdParty)
        if (nature == "AJUSTE_CUADRE") require(!note.isNullOrBlank()) { "Escribe el motivo del ajuste para conservar el rastro." }
        validateOccurredAt(occurredAt)
        val previous = id?.let { dao.movement(it) }
        require(id == null || previous != null) { "No encontré ese movimiento." }
        val effective = rateAt(occurredAt)
        val storedRate = RateSnapshotPolicy.forEdit(
            previousRecordTime = previous?.fechaHora,
            newRecordTime = occurredAt,
            previous = StoredRateSnapshot(previous?.tasaBcvAlMomento, previous?.horaTasa),
            effectiveForNewRecordTime = StoredRateSnapshot(effective?.valor, effective?.horaFuente),
        )
        val now = nowIso()
        val movement = MovimientoEntity(
            id = id ?: 0,
            tipo = direction,
            naturaleza = nature,
            moneda = currency,
            monto = amount,
            tasaBcvAlMomento = storedRate.value,
            horaTasa = storedRate.sourceTime,
            estadoValuacion = if (currency == "USDT" && storedRate.value == null) "SIN VALUAR" else "VALUADA",
            fechaHora = occurredAt,
            categoria = category.trim(),
            motivo = note?.trim()?.takeIf(String::isNotEmpty),
            provenienciaTercero = thirdParty?.trim()?.takeIf(String::isNotEmpty),
            timestampCreacion = previous?.timestampCreacion ?: now,
            timestampUltimaEdicion = now,
        )
        database.withTransaction {
            if (previous == null) {
                val newId = dao.insertMovement(movement)
                dao.insertAudit(BitacoraEntity(tabla = "movimientos", filaId = newId, accion = "CREAR", despues = gson.toJson(movement.copy(id = newId)), timestamp = now))
                if (isAtOrBeforeLastClose(occurredAt)) {
                    reopenWithDiscrepancy(movementImpact(movement, dao.accountNow()?.anclaFechaHora))
                }
            } else {
                val updated = movement.copy(id = previous.id)
                dao.updateMovement(updated)
                dao.insertAudit(BitacoraEntity(tabla = "movimientos", filaId = previous.id, accion = "EDITAR", antes = gson.toJson(previous), despues = gson.toJson(updated), timestamp = now))
                val anchorAt = dao.accountNow()?.anclaFechaHora
                reopenIfNeeded(previous.fechaHora, occurredAt, movementImpact(previous, anchorAt), movementImpact(updated, anchorAt))
            }
        }
    }

    suspend fun saveManualSale(
        id: Long? = null,
        netBsText: String,
        p2pRateText: String,
        usdtText: String,
        occurredAt: String,
        provenance: String?,
    ) {
        val net = positive(netBsText, "Los bolívares netos deben ser mayores que cero.")
        val p2pRate = positive(p2pRateText, "La tasa P2P debe ser mayor que cero.")
        val usdt = positive(usdtText, "El monto USDT debe ser mayor que cero.")
        validateOccurredAt(occurredAt)
        val previous = id?.let { dao.sale(it) }
        require(id == null || previous != null) { "No encontré esa venta." }
        require(previous == null || (previous.fuente == "MANO" && previous.estado == "VIVA")) { "Las ventas de API y los puentes jubilados conservan su rastro; no se editan a mano." }
        val effective = rateAt(occurredAt)
        val storedRate = RateSnapshotPolicy.forEdit(
            previousRecordTime = previous?.fechaHora,
            newRecordTime = occurredAt,
            previous = StoredRateSnapshot(previous?.tasaBcvAlMomento, previous?.horaTasaBcv),
            effectiveForNewRecordTime = StoredRateSnapshot(effective?.valor, effective?.horaFuente),
        )
        val premium = storedRate.value?.let { P2pPremium.calculate(net, usdt, it) }
        val now = nowIso()
        val sale = VentaP2pEntity(
            id = id ?: 0,
            fechaHora = occurredAt,
            bsNetos = net,
            tasaP2p = p2pRate,
            usdtCantidad = usdt,
            tasaBcvAlMomento = storedRate.value,
            horaTasaBcv = storedRate.sourceTime,
            prima = premium,
            estadoValuacion = if (storedRate.value == null) "SIN VALUAR" else "VALUADA",
            proveniencia = provenance?.trim()?.takeIf(String::isNotEmpty),
            fuente = previous?.fuente ?: "MANO",
            estado = previous?.estado ?: "VIVA",
            timestampCreacion = previous?.timestampCreacion ?: now,
            timestampUltimaEdicion = now,
        )
        database.withTransaction {
            if (previous == null) {
                val newId = dao.insertSale(sale)
                dao.insertAudit(BitacoraEntity(tabla = "ventas_p2p_mano", filaId = newId, accion = "CREAR", despues = gson.toJson(sale.copy(id = newId)), timestamp = now))
                if (isAtOrBeforeLastClose(occurredAt)) {
                    reopenWithDiscrepancy(saleImpact(sale, dao.accountNow()?.anclaFechaHora))
                }
            } else {
                val updated = sale.copy(id = previous.id)
                dao.updateSale(updated)
                dao.insertAudit(BitacoraEntity(tabla = "ventas_p2p_mano", filaId = previous.id, accion = "EDITAR", antes = gson.toJson(previous), despues = gson.toJson(updated), timestamp = now))
                val anchorAt = dao.accountNow()?.anclaFechaHora
                reopenIfNeeded(previous.fechaHora, occurredAt, saleImpact(previous, anchorAt), saleImpact(updated, anchorAt))
            }
        }
    }

    suspend fun saveManualRate(valueText: String, sourceTime: String) {
        val value = positive(valueText, "La tasa BCV debe ser mayor que cero.")
        validateOccurredAt(sourceTime)
        database.withTransaction {
            val row = TasaBcvEntity(valor = value, horaFuente = sourceTime, fuente = "MANUAL", confirmadaPor = "HUMANO")
            val id = dao.insertRate(row)
            dao.insertAudit(BitacoraEntity(tabla = "tasas_bcv", filaId = id, accion = "CREAR", despues = gson.toJson(row.copy(id = id)), timestamp = nowIso()))
        }
    }

    suspend fun proposeRate(): TasaFetchResult {
        val result = tasaClient.fetchOfficialMirror()
        if (result is TasaFetchResult.NoNetwork) logRateError("E-SIN-RED")
        if (result is TasaFetchResult.Unavailable) logRateError("E-TASAS-SIN-FUENTE")
        return result
    }

    suspend fun confirmProposal(proposal: TasaPropuesta) {
        val normalizedTime = proposal.horaFuente.toString()
        database.withTransaction {
            val row = TasaBcvEntity(
                valor = proposal.valor.stripTrailingZeros().toPlainString(),
                horaFuente = normalizedTime,
                fuente = "ESPEJO_DOLARAPI",
                confirmadaPor = "HUMANO",
            )
            val id = dao.insertRate(row)
            dao.insertAudit(BitacoraEntity(tabla = "tasas_bcv", filaId = id, accion = "CREAR", despues = gson.toJson(row.copy(id = id)), timestamp = nowIso()))
        }
    }

    suspend fun auditForMovement(id: Long): List<BitacoraEntity> = dao.auditFor("movimientos", id)

    suspend fun auditFor(table: String, id: Long): List<BitacoraEntity> = dao.auditFor(table, id)

    suspend fun localBackupJson(): String = database.withTransaction {
        BackupSnapshotCodec.encode(
            LocalBackupSnapshot(
                schemaVersion = 1,
                generatedAt = nowIso(),
                movements = dao.allMovements(),
                debts = dao.allDebts(),
                installments = dao.allInstallments(),
                groups = dao.allGroups(),
                groupParticipants = dao.allGroupParticipants(),
                groupContributions = dao.allGroupContributions(),
                sales = dao.allSales(),
                rates = dao.allRates(),
                binanceReads = dao.allBinanceReads(),
                accounts = dao.allAccounts(),
                auditEntries = dao.allAuditEntries(),
                errors = dao.allErrors(),
                settings = dao.allSettings(),
                checklistEntries = dao.allChecklistEntries(),
            ),
        )
    }

    suspend fun logRateError(code: String) {
        require(code in SAFE_RATE_ERROR_CODES) { "El código de error de tasa no está permitido." }
        recordError(BitacoraErrorEntity(capa = "TASAS", mensaje = code, detalleTecnico = code, timestamp = nowIso()))
    }

    suspend fun logDatabaseFailure() {
        recordError(
            BitacoraErrorEntity(
                capa = "DB",
                mensaje = "No se confirmó el guardado local.",
                detalleTecnico = null,
                timestamp = nowIso(),
            ),
        )
    }

    suspend fun logLocalBackupFailure() {
        recordError(
            BitacoraErrorEntity(
                capa = "DB",
                mensaje = "No se completó la copia local privada.",
                detalleTecnico = null,
                timestamp = nowIso(),
            ),
        )
    }

    private suspend fun recordError(entry: BitacoraErrorEntity) {
        database.withTransaction {
            dao.insertError(entry)
            dao.pruneErrorEntries(ERROR_LOG_LIMIT)
        }
    }

    private suspend fun rateAt(occurredAt: String): TasaBcvEntity? {
        val rates = dao.ratesNow()
        val match = RateLookup.latestConfirmedAtOrBefore(
            rates.map { ConfirmedRateCandidate(it.id, it.valor, it.horaFuente, it.confirmadaPor == "HUMANO") },
            occurredAt,
        ) ?: return null
        return rates.firstOrNull { it.id == match.id }
    }

    private suspend fun reopenIfNeeded(oldTime: String, newTime: String, before: BigDecimal, after: BigDecimal) {
        val oldWasAtCut = isAtOrBeforeLastClose(oldTime)
        val newIsAtCut = isAtOrBeforeLastClose(newTime)
        if (oldWasAtCut || newIsAtCut) {
            val oldImpact = if (oldWasAtCut) before else BigDecimal.ZERO
            val newImpact = if (newIsAtCut) after else BigDecimal.ZERO
            reopenWithDiscrepancy(newImpact.subtract(oldImpact))
        }
    }

    private suspend fun isAtOrBeforeLastClose(time: String): Boolean {
        val closedAt = dao.accountNow()?.ultimoCuadreAt ?: return false
        val recordInstant = CuadreCalculator.instantOrNull(time) ?: return false
        val closeInstant = CuadreCalculator.instantOrNull(closedAt) ?: return false
        return !recordInstant.isAfter(closeInstant)
    }

    private suspend fun reopenWithDiscrepancy(delta: BigDecimal) {
        val account = dao.accountNow() ?: return
        val currentDifference = if (account.estadoCuadre == "DESCUADRE_ABIERTO") BigDecimal(account.cifraDescuadre ?: "0") else BigDecimal.ZERO
        val newDifference = ReconciliationMath.afterRecordChange(currentDifference, delta)
        val updated = if (newDifference.signum() == 0) {
            account.copy(estadoCuadre = "CALCULADO", cifraDescuadre = null)
        } else {
            account.copy(estadoCuadre = "DESCUADRE_ABIERTO", cifraDescuadre = newDifference.toPlainString())
        }
        dao.saveAccount(updated)
        dao.insertAudit(BitacoraEntity(tabla = "estado_cuentas", filaId = 1, accion = "EDITAR", antes = gson.toJson(account), despues = gson.toJson(updated), timestamp = nowIso()))
    }

    private fun movementImpact(movement: MovimientoEntity, anchorAt: String?): BigDecimal {
        if (movement.moneda != "Bs" || !CuadreCalculator.affectsBalanceAfterAnchor(movement.fechaHora, anchorAt)) return BigDecimal.ZERO
        val amount = BigDecimal(movement.monto)
        return if (movement.tipo == "INGRESO") amount else amount.negate()
    }

    private fun saleImpact(sale: VentaP2pEntity, anchorAt: String?): BigDecimal {
        if (sale.estado != "VIVA" || !CuadreCalculator.affectsBalanceAfterAnchor(sale.fechaHora, anchorAt)) return BigDecimal.ZERO
        return BigDecimal(sale.bsNetos)
    }

    private fun calculate(account: EstadoCuentaEntity, movements: List<MovimientoEntity>, sales: List<VentaP2pEntity>) =
        CuadreCalculator.calculate(
            CuadreInput(
                account.anclaMonto,
                account.anclaFechaHora,
                movements.map { MovementForCalculation(it.tipo, it.naturaleza, it.moneda, it.monto, it.fechaHora, it.tasaBcvAlMomento) },
                sales.map { SaleForCalculation(it.bsNetos, it.usdtCantidad, it.fechaHora, it.tasaBcvAlMomento, it.estado) },
                account.estadoCuadre,
                account.cifraDescuadre,
            ),
        )

    private fun positive(text: String, message: String): String {
        val number = number(text, message)
        require(number > BigDecimal.ZERO) { message }
        return number.stripTrailingZeros().toPlainString()
    }

    private fun positiveOrZero(text: String, message: String): String {
        val number = number(text, message)
        require(number >= BigDecimal.ZERO) { message }
        return number.stripTrailingZeros().toPlainString()
    }

    private fun number(text: String, message: String): BigDecimal = try {
        val normalized = text.trim().replace(" ", "").let { raw ->
            if (raw.contains(',') && raw.contains('.')) raw.replace(".", "").replace(',', '.') else raw.replace(',', '.')
        }
        BigDecimal(normalized)
    } catch (_: NumberFormatException) {
        throw IllegalArgumentException(message)
    }

    private fun validateOccurredAt(value: String) {
        val instant = CuadreCalculator.instantOrNull(value) ?: throw IllegalArgumentException("La fecha y hora debe ser válida y tener zona horaria.")
        require(!instant.isAfter(java.time.Instant.now())) { "La fecha y hora no puede estar en el futuro." }
    }

    companion object {
        const val ERROR_LOG_LIMIT = 500
        private val SAFE_RATE_ERROR_CODES = setOf("E-SIN-RED", "E-TASAS-SIN-FUENTE")

        fun nowIso(): String = OffsetDateTime.now(ZoneId.of("America/Caracas")).toString()
    }
}

internal data class LocalBackupSnapshot(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("generated_at") val generatedAt: String,
    val movements: List<MovimientoEntity>,
    val debts: List<DeudaEntity>,
    val installments: List<CuotaEntity>,
    val groups: List<BolsoEntity>,
    @SerializedName("group_participants") val groupParticipants: List<BolsoParticipanteEntity>,
    @SerializedName("group_contributions") val groupContributions: List<BolsoParticipacionEntity>,
    val sales: List<VentaP2pEntity>,
    val rates: List<TasaBcvEntity>,
    @SerializedName("binance_reads") val binanceReads: List<LecturaBinanceEntity>,
    val accounts: List<EstadoCuentaEntity>,
    @SerializedName("audit_entries") val auditEntries: List<BitacoraEntity>,
    val errors: List<BitacoraErrorEntity>,
    val settings: List<SettingsEntity>,
    @SerializedName("checklist_entries") val checklistEntries: List<ActaCasillaEntity>,
)

internal object BackupSnapshotCodec {
    private val gson = Gson()

    fun encode(snapshot: LocalBackupSnapshot): String = gson.toJson(snapshot)

    fun decode(json: String): LocalBackupSnapshot? = try {
        gson.fromJson(json, LocalBackupSnapshot::class.java)?.takeIf { it.schemaVersion == 1 }
    } catch (_: Exception) {
        null
    }
}
