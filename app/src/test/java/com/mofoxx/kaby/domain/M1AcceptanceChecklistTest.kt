package com.mofoxx.kaby.domain

import com.mofoxx.kaby.data.ActaCasillaEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M1AcceptanceChecklistTest {
    @Test
    fun testCasillas_sinResultados_noSeApruebanAutomaticamente() {
        assertEquals(10, M1AcceptanceChecklist.cases.size)
        assertFalse(M1AcceptanceChecklist.isReadyToSeal(emptyList()))
        assertFalse(M1AcceptanceChecklist.isSealed(emptyList()))
    }

    @Test
    fun testCasillas_passRequiereReferenciaDeEvidencia() {
        assertNotNull(M1AcceptanceChecklist.validationError("PASS", "", " "))
        assertNull(M1AcceptanceChecklist.validationError("PASS", "", "TESTING M1: captura 01"))
    }

    @Test
    fun testCasillas_blockedRequiereCausa() {
        assertNotNull(M1AcceptanceChecklist.validationError("BLOCKED", "", ""))
        assertNull(M1AcceptanceChecklist.validationError("BLOCKED", "No hay formato SAF decidido", ""))
    }

    @Test
    fun testCasillas_firmaPropietarioNecesitaTodosLosPass() {
        val passed = M1AcceptanceChecklist.cases.mapIndexed { index, item ->
            ActaCasillaEntity(
                id = index + 1L,
                hito = "M1",
                casillaId = item.id,
                veredicto = "PASS",
                evidencia = "evidencia ${item.id}",
                timestamp = "2026-10-21T12:00:00-04:00",
            )
        }
        assertTrue(M1AcceptanceChecklist.isReadyToSeal(passed))
        val signature = ActaCasillaEntity(
            id = 11,
            hito = "M1",
            casillaId = M1AcceptanceChecklist.OWNER_CONFIRMATION_ID,
            veredicto = "PASS",
            evidencia = "Confirmación en dispositivo",
            timestamp = "2026-10-21T12:01:00-04:00",
        )
        assertTrue(M1AcceptanceChecklist.isSealed(passed + signature))
        assertFalse(M1AcceptanceChecklist.isSealed(passed + signature + passed.first().copy(id = 12, veredicto = "FAIL")))
    }

    private fun assertEquals(expected: Int, actual: Int) = org.junit.Assert.assertEquals(expected, actual)
}
