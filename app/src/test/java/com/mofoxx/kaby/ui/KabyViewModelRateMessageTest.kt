package com.mofoxx.kaby.ui

import com.mofoxx.kaby.data.TasaBcvEntity
import com.mofoxx.kaby.tasas.TasaFetchResult
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KabyViewModelRateMessageTest {
    @Test
    fun testTasas_sinRed_ultimaConHoraAviso() {
        val latestRate = TasaBcvEntity(
            valor = "883.3963",
            horaFuente = "2026-10-21T00:00:00-04:00",
            fuente = "MANUAL",
            confirmadaPor = "HUMANO",
        )

        val message = rateFetchErrorMessage(TasaFetchResult.NoNetwork, latestRate)

        assertNotNull(message)
        assertTrue(message!!.contains("E-SIN-RED"))
        assertTrue(message.contains("2026-10-21T00:00:00-04:00"))
        assertFalse(message.contains("883.3963"))
    }

    @Test
    fun testTasas_sinRed_sinTasaAnteriorNoInventaUna() {
        val message = rateFetchErrorMessage(TasaFetchResult.NoNetwork, null)

        assertNotNull(message)
        assertTrue(message!!.contains("No hay una tasa confirmada previa"))
    }

    @Test
    fun testErrores_copiableSinDatos() {
        val rate = TasaBcvEntity(
            valor = "883.3963",
            horaFuente = "2026-10-21T00:00:00-04:00",
            fuente = "MANUAL",
            confirmadaPor = "HUMANO",
        )
        val safeMessage = rateFetchErrorMessage(TasaFetchResult.NoNetwork, rate)!!
        val copied = diagnosticCopyText("2026-10-21 09:00", safeMessage)

        assertTrue(copied.contains("E-SIN-RED"))
        assertFalse(copied.contains("883.3963"))
        assertFalse(copied.contains("nota privada"))
        assertFalse(copied.contains("tercero de prueba"))
        assertFalse(copied.contains("SECRET"))
    }
}
