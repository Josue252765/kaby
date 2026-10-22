package com.mofoxx.kaby.ui

import com.mofoxx.kaby.data.BitacoraEntity
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KabyAmountFormatTest {
    @Test
    fun testEquivalenteBcv_usaUSDYLaVentaConservaUSDT() {
        val amount = BigDecimal("1234.50")

        assertTrue(formatUsd(amount).startsWith("USD "))
        assertFalse(formatUsd(amount).contains("USDT"))
        assertTrue(formatUsdt(amount).endsWith(" USDT"))
    }

    @Test
    fun testEquivalenciaDelMovimiento_usaLaTasaGuardadaYNoInventaUna() {
        val bsExpense = movementDisplayValuation("880", "Bs", "880")
        val usdtExpense = movementDisplayValuation("1", "USDT", "880")

        assertEquals(BigDecimal("1.00"), bsExpense?.usd)
        assertEquals(BigDecimal("880"), bsExpense?.bolivares)
        assertEquals(BigDecimal("1.00"), usdtExpense?.usd)
        assertEquals(BigDecimal("880"), usdtExpense?.bolivares)
        assertNull(movementDisplayValuation("880", "Bs", null))
    }

    @Test
    fun testEstadoVentaMuestraPalabrasAmigables() {
        assertEquals("Activa", humanizeSaleState("VIVA"))
        assertEquals("Cancelada", humanizeSaleState("CANCELADA"))
    }

    @Test
    fun testRastro_edicionMuestraAntesDespuesEnPalabras() {
        val entry = BitacoraEntity(
            tabla = "movimientos",
            filaId = 7,
            accion = "EDITAR",
            antes = """{"monto":"118.50","moneda":"Bs","categoria":"comida","tipo":"GASTO"}""",
            despues = """{"monto":"120.00","moneda":"Bs","categoria":"comida","tipo":"GASTO","timestampUltimaEdicion":"2026-10-21T18:00:00-04:00"}""",
            timestamp = "2026-10-21T18:00:00-04:00",
        )

        val description = auditDescription(entry)

        assertTrue(description.contains("antes:"))
        assertTrue(description.contains("después:"))
        assertTrue(description.contains("Gasto · comida"))
        assertTrue(description.contains("editado el"))
        assertFalse(description.contains("{"))
        assertFalse(description.contains("GASTOcomida"))
    }

    @Test
    fun testRastro_creacionDescribeVentaSinJson() {
        val entry = BitacoraEntity(
            tabla = "ventas_p2p_mano",
            filaId = 12,
            accion = "CREAR",
            despues = """{"bsNetos":"50000.00","fechaHora":"2026-10-21T12:00:00-04:00"}""",
            timestamp = "2026-10-21T12:00:00-04:00",
        )

        val description = auditDescription(entry)

        assertTrue(description.contains("después:"))
        assertTrue(description.contains("venta P2P"))
        assertTrue(description.contains("creado el"))
        assertFalse(description.contains("{"))
    }
}
