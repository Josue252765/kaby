package com.mofoxx.kaby.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneId

data class CuadreInput(
    val anchorAmount: String?,
    val anchorAt: String?,
    val movements: List<MovementForCalculation>,
    val sales: List<SaleForCalculation>,
    val lastReconciliationState: String = "CALCULADO",
    val discrepancy: String? = null,
)

data class MovementForCalculation(
    val direction: String,
    val nature: String,
    val currency: String,
    val amount: String,
    val occurredAt: String,
    val bcvAtMoment: String?,
)

data class SaleForCalculation(
    val netBolivares: String,
    val usdtAmount: String,
    val occurredAt: String,
    val bcvAtMoment: String?,
    val state: String,
)

data class CuadreResult(
    val mercantilBs: BigDecimal?,
    val incomeBs: BigDecimal,
    val expenseBs: BigDecimal,
    val netGainBs: BigDecimal,
    val p2pPremiumBs: BigDecimal,
    val unvaluedRows: Int,
    val reconciliationState: String,
    val discrepancyBs: BigDecimal?,
)

/** Pure arithmetic for the canonical M1 rules. Monetary values stay decimal strings in Room. */
object CuadreCalculator {
    private val ZERO = BigDecimal.ZERO.setScale(2)

    fun calculate(input: CuadreInput): CuadreResult {
        var balanceDelta = BigDecimal.ZERO
        var income = BigDecimal.ZERO
        var expense = BigDecimal.ZERO
        var premium = BigDecimal.ZERO
        var unvalued = 0

        for (movement in input.movements) {
            val amount = decimal(movement.amount) ?: continue
            val afterAnchor = affectsBalanceAfterAnchor(movement.occurredAt, input.anchorAt)
            val isBolivares = movement.currency == "Bs"
            val rate = decimal(movement.bcvAtMoment)
            val amountBs = when {
                isBolivares -> amount
                rate != null -> amount.multiply(rate)
                else -> null
            }
            if (!isBolivares && amountBs == null) unvalued += 1

            if (afterAnchor && isBolivares) {
                when (movement.direction) {
                    "INGRESO" -> balanceDelta = balanceDelta.add(amount)
                    "GASTO" -> balanceDelta = balanceDelta.subtract(amount)
                }
            }

            if (movement.nature == "NORMAL") {
                when (movement.direction) {
                    "INGRESO" -> amountBs?.let { income = income.add(it) }
                    "GASTO" -> amountBs?.let { expense = expense.add(it) }
                }
            }
        }

        for (sale in input.sales) {
            if (sale.state != "VIVA") continue
            if (affectsBalanceAfterAnchor(sale.occurredAt, input.anchorAt)) {
                decimal(sale.netBolivares)?.let { balanceDelta = balanceDelta.add(it) }
            }
            decimal(sale.netBolivares)?.let { income = income.add(it) }
            val net = decimal(sale.netBolivares)
            val usdt = decimal(sale.usdtAmount)
            val rate = decimal(sale.bcvAtMoment)
            if (net != null && usdt != null && rate != null) {
                premium = premium.add(net.subtract(usdt.multiply(rate)).setScale(2, RoundingMode.HALF_UP))
            } else {
                unvalued += 1
            }
        }

        val anchor = decimal(input.anchorAmount)
        val mercantil = anchor?.add(balanceDelta)?.setScale(2, RoundingMode.HALF_UP)
        val incomeRounded = income.setScale(2, RoundingMode.HALF_UP)
        val expenseRounded = expense.setScale(2, RoundingMode.HALF_UP)
        val discrepancy = decimal(input.discrepancy)?.setScale(2, RoundingMode.HALF_UP)

        return CuadreResult(
            mercantilBs = mercantil,
            incomeBs = incomeRounded,
            expenseBs = expenseRounded,
            netGainBs = incomeRounded.subtract(expenseRounded).setScale(2, RoundingMode.HALF_UP),
            p2pPremiumBs = premium.setScale(2, RoundingMode.HALF_UP),
            unvaluedRows = unvalued,
            reconciliationState = if (anchor == null) "SIN_ANCLA" else input.lastReconciliationState,
            discrepancyBs = discrepancy,
        )
    }

    fun instantOrNull(value: String?): Instant? = try {
        value?.let { OffsetDateTime.parse(it).toInstant() }
    } catch (_: Exception) {
        null
    }

    fun affectsBalanceAfterAnchor(occurredAt: String, anchorAt: String?): Boolean {
        if (anchorAt == null) return true
        val occurred = instantOrNull(occurredAt) ?: return false
        val anchor = instantOrNull(anchorAt) ?: return false
        return occurred.isAfter(anchor)
    }

    fun decimal(value: String?): BigDecimal? = try {
        value?.takeIf(String::isNotBlank)?.let(::BigDecimal)
    } catch (_: NumberFormatException) {
        null
    }

    fun money(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
}

object ReconciliationMath {
    /** discrepancy is real balance minus calculated balance; effectDelta is the record's effect on the calculated balance. */
    fun againstObserved(realBalance: BigDecimal, calculatedBalance: BigDecimal): BigDecimal =
        realBalance.subtract(calculatedBalance).setScale(2, RoundingMode.HALF_UP)

    fun afterRecordChange(discrepancy: BigDecimal?, effectDelta: BigDecimal): BigDecimal =
        (discrepancy ?: BigDecimal.ZERO).subtract(effectDelta).setScale(2, RoundingMode.HALF_UP)
}

data class ConfirmedRateCandidate(val id: Long, val value: String, val sourceTime: String, val confirmed: Boolean)

object RateLookup {
    fun latestConfirmedAtOrBefore(rates: List<ConfirmedRateCandidate>, recordTime: String): ConfirmedRateCandidate? {
        val recordInstant = CuadreCalculator.instantOrNull(recordTime) ?: return null
        return rates.asSequence()
            .filter { it.confirmed }
            .filter { candidate ->
                CuadreCalculator.instantOrNull(candidate.sourceTime)?.let { !it.isAfter(recordInstant) } == true
            }
            .maxWithOrNull(compareBy<ConfirmedRateCandidate> { CuadreCalculator.instantOrNull(it.sourceTime) ?: Instant.MIN }.thenBy { it.id })
    }
}

data class StoredRateSnapshot(val value: String?, val sourceTime: String?)

object RateSnapshotPolicy {
    fun forEdit(
        previousRecordTime: String?,
        newRecordTime: String,
        previous: StoredRateSnapshot,
        effectiveForNewRecordTime: StoredRateSnapshot,
    ): StoredRateSnapshot {
        val sameInstant = previousRecordTime?.let(CuadreCalculator::instantOrNull) == CuadreCalculator.instantOrNull(newRecordTime)
        return if (sameInstant && previous.value != null) previous else effectiveForNewRecordTime
    }
}

data class DatedTotal(val amount: BigDecimal, val usdEquivalent: BigDecimal, val unvaluedRows: Int)

object SpendingByDate {
    fun forDate(movements: List<MovementForCalculation>, date: LocalDate, zone: ZoneId): DatedTotal {
        return summarize(
            movements.asSequence().filter { row -> instantOrNull(row.occurredAt)?.atZone(zone)?.toLocalDate() == date },
        )
    }

    fun all(movements: List<MovementForCalculation>): DatedTotal = summarize(movements.asSequence())

    private fun summarize(movements: Sequence<MovementForCalculation>): DatedTotal {
        var total = BigDecimal.ZERO
        var usdTotal = BigDecimal.ZERO
        var unvalued = 0
        movements
            .filter { it.direction == "GASTO" && it.nature == "NORMAL" }
            .forEach { row ->
                val amount = decimal(row.amount) ?: return@forEach
                val rate = decimal(row.bcvAtMoment)?.takeIf { it > BigDecimal.ZERO }
                val amountBs = when (row.currency) {
                    "Bs" -> amount
                    "USDT" -> rate?.let(amount::multiply)
                    else -> null
                }
                if (amountBs == null) {
                    unvalued += 1
                    return@forEach
                }
                total = total.add(amountBs)
                val amountUsd = rate?.let { BcvValuation.usdEquivalent(amountBs, it) }
                if (amountUsd == null) unvalued += 1 else usdTotal = usdTotal.add(amountUsd)
            }
        return DatedTotal(
            amount = total.setScale(2, RoundingMode.HALF_UP),
            usdEquivalent = usdTotal.setScale(2, RoundingMode.HALF_UP),
            unvaluedRows = unvalued,
        )
    }

    private fun instantOrNull(value: String) = CuadreCalculator.instantOrNull(value)
    private fun decimal(value: String?) = CuadreCalculator.decimal(value)
}
