package com.mofoxx.kaby.ui

import com.mofoxx.kaby.data.MovimientoEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementListDateTest {
    @Test
    fun testListaAgrupaMovimientosPorFechaDeCaracas() {
        val caracasDate = localCaracasDate("2026-09-04T03:30:00Z")

        assertEquals(LocalDate.of(2026, 9, 3), caracasDate)
        assertEquals("Jueves 3 de septiembre (Caracas)", formatCaracasDay(caracasDate))
    }

    @Test
    fun testRastro_marcaSoloUnaEdicionPosteriorAlRegistro() {
        assertTrue(hasBeenEdited("2026-09-21T09:00:00-04:00", "2026-09-21T13:01:00Z"))
        assertFalse(hasBeenEdited("2026-09-21T09:00:00-04:00", "2026-09-21T13:00:00Z"))
        assertFalse(hasBeenEdited("2026-09-21T13:01:00Z", "2026-09-21T13:00:00Z"))
    }

    @Test
    fun testProvenienciasSugeridas_soloIngresosNormalesYSinDuplicados() {
        val movements = listOf(
            movement(id = 4, type = "INGRESO", nature = "NORMAL", provenance = "Pago de trabajo"),
            movement(id = 3, type = "INGRESO", nature = "NORMAL", provenance = "  Pago de trabajo  "),
            movement(id = 2, type = "INGRESO", nature = "AJUSTE_CUADRE", provenance = "Ajuste"),
            movement(id = 1, type = "GASTO", nature = "NORMAL", provenance = "No sugerir"),
        )

        assertEquals(listOf("Pago de trabajo"), recentThirdPartySuggestions(movements))
    }

    private fun movement(id: Long, type: String, nature: String, provenance: String) = MovimientoEntity(
        id = id,
        tipo = type,
        naturaleza = nature,
        moneda = "Bs",
        monto = "10",
        fechaHora = "2026-10-21T10:00:00-04:00",
        categoria = "Trabajo",
        provenienciaTercero = provenance,
        timestampCreacion = "2026-10-21T10:00:00-04:00",
        timestampUltimaEdicion = "2026-10-21T10:00:00-04:00",
    )
}
