package com.mofoxx.kaby.tasas

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DolarApiTasaClientTest {
    private val client = DolarApiTasaClient()

    @Test
    fun testTasas_parseoAmbasTZ() {
        val official = client.parse(JsonParser.parseString("""
            [{"moneda":"USD","fuente":"oficial","promedio":883.3963,"fechaActualizacion":"2026-10-21T00:00:00-04:00"}]
        """))
        val utc = client.parse(JsonParser.parseString("""
            [{"moneda":"USD","fuente":"oficial","promedio":883.3963,"fechaActualizacion":"2026-10-21T04:00:00Z"}]
        """))
        assertEquals("-04:00", official?.horaFuente?.offset?.id)
        assertEquals("Z", utc?.horaFuente?.offset?.id)
        assertEquals(official?.horaFuente?.toInstant(), utc?.horaFuente?.toInstant())
        assertEquals("DOLARAPI", official?.proveedor)
    }

    @Test
    fun testTasas_rechazaParaleloComoBCVOficial() {
        val proposal = client.parse(JsonParser.parseString("""
            [{"moneda":"USD","fuente":"paralelo","promedio":900,"fechaActualizacion":"2026-10-21T04:00:00Z"}]
        """))
        assertNull(proposal)
    }
}
