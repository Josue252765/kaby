package com.mofoxx.kaby.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CuadreCalculatorTest {
    @Test
    fun testCuadre_datasetCanonico_saldoCoincide() {
        val result = CuadreCalculator.calculate(canonicalInput())
        assertEquals("101522.50", result.mercantilBs?.toPlainString())
        assertEquals("CALCULADO", result.reconciliationState)
    }

    @Test
    fun testCuadre_ventaP2P_casaUnicaAntiDoble() {
        val oneSale = SaleForCalculation("50000", "50", "2026-09-07T11:00:00-04:00", "880", "VIVA")
        val live = CuadreCalculator.calculate(CuadreInput("1000", "2026-09-01T08:00:00-04:00", emptyList(), listOf(oneSale)))
        val retired = CuadreCalculator.calculate(CuadreInput("1000", "2026-09-01T08:00:00-04:00", emptyList(), listOf(oneSale.copy(state = "JUBILADA"))))
        assertEquals("51000.00", live.mercantilBs?.toPlainString())
        assertEquals("1000.00", retired.mercantilBs?.toPlainString())
        assertEquals("50000.00", live.incomeBs.subtract(retired.incomeBs).toPlainString())
    }

    @Test
    fun testCuadre_turno_saldoSi_metricasNo() {
        val result = CuadreCalculator.calculate(canonicalInput())
        assertEquals("101522.50", result.mercantilBs?.toPlainString())
        assertEquals("90212.50", result.incomeBs.toPlainString())
        assertEquals("1590.00", result.expenseBs.toPlainString())
        val canonical = canonicalInput()
        val withoutTurn = CuadreCalculator.calculate(canonical.copy(movements = canonical.movements.filterNot { it.nature == "TURNO_BOLSO" }))
        assertEquals("400.00", result.mercantilBs!!.subtract(withoutTurn.mercantilBs!!).toPlainString())
        assertEquals(result.incomeBs, withoutTurn.incomeBs)
        assertEquals(result.expenseBs, withoutTurn.expenseBs)
    }

    @Test
    fun testCuadre_ajusteCuadre_noEngordaDoble() {
        val result = CuadreCalculator.calculate(
            CuadreInput(
                "1000", "2026-09-01T08:00:00-04:00",
                listOf(MovementForCalculation("INGRESO", "AJUSTE_CUADRE", "Bs", "25.50", "2026-09-02T10:00:00-04:00", "880")),
                emptyList(),
            ),
        )
        assertEquals("1025.50", result.mercantilBs?.toPlainString())
        assertEquals("0.00", result.incomeBs.toPlainString())
        assertEquals("0.00", result.expenseBs.toPlainString())
    }

    @Test
    fun testCuadre_sinAncla_estadoSIN_ANCLA() {
        val result = CuadreCalculator.calculate(CuadreInput(null, null, emptyList(), emptyList()))
        assertNull(result.mercantilBs)
        assertEquals("SIN_ANCLA", result.reconciliationState)
    }

    @Test
    fun testCuadre_anclaPuedeSerCero() {
        val result = CuadreCalculator.calculate(CuadreInput("0", "2026-09-01T08:00:00-04:00", emptyList(), emptyList()))
        assertEquals("0.00", result.mercantilBs?.toPlainString())
        assertEquals("CALCULADO", result.reconciliationState)
    }

    @Test
    fun testCuadre_movimientoAnteriorAlAnclaNoCambiaSaldoInicial() {
        val input = CuadreInput(
            "1000", "2026-09-01T08:00:00-04:00",
            listOf(MovementForCalculation("GASTO", "NORMAL", "Bs", "90", "2026-09-01T07:59:59-04:00", "880")),
            emptyList(),
        )
        assertEquals("1000.00", CuadreCalculator.calculate(input).mercantilBs?.toPlainString())
        assertEquals(false, CuadreCalculator.affectsBalanceAfterAnchor("2026-09-01T08:00:00-04:00", input.anchorAt))
    }

    @Test
    fun testGananciaNeta_datasetCanonico_definicion() {
        val result = CuadreCalculator.calculate(canonicalInput())
        assertEquals("88622.50", result.netGainBs.toPlainString())
    }

    @Test
    fun testPrima_formulaConSigno() {
        val result = CuadreCalculator.calculate(canonicalInput())
        assertEquals("10212.50", result.p2pPremiumBs.toPlainString())
    }

    @Test
    fun testPrima_casoNegativo_conSigno() {
        val result = CuadreCalculator.calculate(
            CuadreInput(null, null, emptyList(), listOf(SaleForCalculation("6450", "7.5", "2026-09-01T09:00:00-04:00", "880", "VIVA"))),
        )
        assertEquals("-150.00", result.p2pPremiumBs.toPlainString())
    }

    @Test
    fun testPrima_redondeoCentavos() {
        val result = CuadreCalculator.calculate(
            CuadreInput(
                null,
                null,
                emptyList(),
                listOf(
                    SaleForCalculation("1.005", "1", "2026-09-01T09:00:00-04:00", "1", "VIVA"),
                    SaleForCalculation("1.005", "1", "2026-09-01T09:01:00-04:00", "1", "VIVA"),
                ),
            ),
        )
        assertEquals("0.02", result.p2pPremiumBs.toPlainString())
    }

    @Test
    fun testCuadre_otraPlata_provenienciaObligatoria() {
        assertThrows(IllegalArgumentException::class.java) {
            MovementPolicy.validate("INGRESO", "NORMAL", "Bs", java.math.BigDecimal("5000"), "remesa", " ")
        }
        MovementPolicy.validate("INGRESO", "NORMAL", "Bs", java.math.BigDecimal("5000"), "remesa", "familiar · apoyo")
    }

    @Test
    fun testBsEnMano_revaluaConBcvNueva() {
        val amount = java.math.BigDecimal("12500")
        assertEquals("14.20", BcvValuation.usdEquivalent(amount, java.math.BigDecimal("880"))?.toPlainString())
        assertEquals("13.89", BcvValuation.usdEquivalent(amount, java.math.BigDecimal("900"))?.toPlainString())
        assertEquals(null, BcvValuation.usdEquivalent(amount, null))
    }

    @Test
    fun testTasa_usaUltimaConfirmadaAnteriorAlMovimiento() {
        val rates = listOf(
            ConfirmedRateCandidate(1, "880", "2026-09-01T08:05:00-04:00", true),
            ConfirmedRateCandidate(2, "900", "2026-09-21T08:05:00-04:00", false),
            ConfirmedRateCandidate(3, "885", "2026-09-20T09:00:00-04:00", true),
        )
        val selected = RateLookup.latestConfirmedAtOrBefore(rates, "2026-09-20T10:00:00-04:00")
        assertEquals("885", selected?.value)
        assertEquals("880", RateLookup.latestConfirmedAtOrBefore(rates, "2026-09-15T10:00:00-04:00")?.value)
        assertEquals(null, RateLookup.latestConfirmedAtOrBefore(rates, "2026-08-30T10:00:00-04:00"))
    }

    @Test
    fun testTasa_mismoInstanteDesempataConInsercionMasReciente() {
        val sameInstant = listOf(
            ConfirmedRateCandidate(1, "880", "2026-09-01T08:00:00-04:00", true),
            ConfirmedRateCandidate(2, "881", "2026-09-01T12:00:00Z", true),
        )
        assertEquals("881", RateLookup.latestConfirmedAtOrBefore(sameInstant, "2026-09-01T08:00:00-04:00")?.value)
    }

    @Test
    fun testBsEnMano_montoCongeladoNoRevalua() {
        val previous = StoredRateSnapshot("880", "2026-09-01T08:05:00-04:00")
        val newlyAddedRate = StoredRateSnapshot("900", "2026-09-02T08:00:00-04:00")
        assertEquals(
            previous,
            RateSnapshotPolicy.forEdit("2026-09-03T12:30:00-04:00", "2026-09-03T12:30:00-04:00", previous, newlyAddedRate),
        )
        assertEquals(
            newlyAddedRate,
            RateSnapshotPolicy.forEdit("2026-09-03T12:30:00-04:00", "2026-09-04T12:30:00-04:00", previous, newlyAddedRate),
        )
    }

    @Test
    fun testSinValuar_sePuedeValuarTrasUnaTasaConfirmada() {
        val previous = StoredRateSnapshot(null, null)
        val effective = StoredRateSnapshot("880", "2026-09-01T08:05:00-04:00")
        assertEquals(effective, RateSnapshotPolicy.forEdit("2026-09-03T12:30:00-04:00", "2026-09-03T12:30:00-04:00", previous, effective))
    }

    @Test
    fun testGastadoHoy_valuaSoloLaFechaLocalYExponeLosSinValuar() {
        val entries = listOf(
            MovementForCalculation("GASTO", "NORMAL", "Bs", "100", "2026-09-20T23:30:00Z", "880"),
            MovementForCalculation("GASTO", "NORMAL", "USDT", "2", "2026-09-21T01:00:00Z", null),
            MovementForCalculation("GASTO", "AJUSTE_CUADRE", "Bs", "50", "2026-09-21T08:00:00-04:00", "880"),
        )
        val result = SpendingByDate.forDate(entries, java.time.LocalDate.of(2026, 9, 20), java.time.ZoneId.of("America/Caracas"))
        assertEquals("100.00", result.amount.toPlainString())
        assertEquals("0.11", result.usdEquivalent.toPlainString())
        assertEquals(1, result.unvaluedRows)
    }

    @Test
    fun testGastosAcumulados_usaTasaDeCadaRegistroYMarcaEquivalenciaParcial() {
        val entries = listOf(
            MovementForCalculation("GASTO", "NORMAL", "Bs", "1000", "2026-09-01T12:00:00-04:00", "1000"),
            MovementForCalculation("GASTO", "NORMAL", "USDT", "5", "2026-09-02T12:00:00-04:00", "1000"),
            MovementForCalculation("GASTO", "NORMAL", "Bs", "100", "2026-09-03T12:00:00-04:00", null),
            MovementForCalculation("INGRESO", "NORMAL", "Bs", "9000", "2026-09-03T13:00:00-04:00", "1000"),
            MovementForCalculation("GASTO", "AJUSTE_CUADRE", "Bs", "500", "2026-09-03T14:00:00-04:00", "1000"),
        )

        val result = SpendingByDate.all(entries)

        assertEquals("6100.00", result.amount.toPlainString())
        assertEquals("6.00", result.usdEquivalent.toPlainString())
        assertEquals(1, result.unvaluedRows)
    }

    private fun canonicalInput() = CuadreInput(
        anchorAmount = "12500.00",
        anchorAt = "2026-09-01T08:00:00-04:00",
        movements = listOf(
            MovementForCalculation("GASTO", "NORMAL", "Bs", "1000", "2026-09-03T12:30:00-04:00", "880"),
            MovementForCalculation("GASTO", "NORMAL", "Bs", "350", "2026-09-03T18:10:00-04:00", "880"),
            MovementForCalculation("INGRESO", "NORMAL", "Bs", "5000", "2026-09-05T10:00:00-04:00", "880"),
            MovementForCalculation("INGRESO", "NORMAL", "Bs", "200", "2026-09-12T15:00:00-04:00", "880"),
            MovementForCalculation("GASTO", "NORMAL", "Bs", "120", "2026-09-15T09:30:00-04:00", "880"),
            MovementForCalculation("INGRESO", "TURNO_BOLSO", "Bs", "400", "2026-09-16T11:00:00-04:00", "880"),
            MovementForCalculation("GASTO", "NORMAL", "Bs", "120", "2026-09-10T13:00:00-04:00", "880"),
        ),
        sales = listOf(
            SaleForCalculation("50000", "50", "2026-09-07T11:00:00-04:00", "880", "VIVA"),
            SaleForCalculation("24962.50", "25", "2026-09-14T17:20:00-04:00", "880", "VIVA"),
            SaleForCalculation("10050", "10", "2026-09-20T10:00:00-04:00", "880", "VIVA"),
        ),
    )
}
