package com.mofoxx.kaby.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KabySnapshotConfirmedRateTest {
    @Test
    fun testTasas_mirrorSinConfirmarNoEsTasaVigente() {
        val snapshot = snapshot(
            rate(value = "883.3963", time = "2026-10-21T00:00:00-04:00", confirmedBy = null),
        )

        assertNull(snapshot.latestConfirmedRate)
    }

    @Test
    fun testTasas_usaSoloLaConfirmadaMasRecientePorInstante() {
        val snapshot = snapshot(
            rate(value = "800", time = "2026-10-20T05:00:00-04:00", confirmedBy = "HUMANO"),
            rate(value = "810", time = "2026-10-20T04:00:00Z", confirmedBy = "HUMANO"),
            rate(value = "900", time = "2026-10-20T08:00:00Z", confirmedBy = null),
        )

        assertEquals("800", snapshot.latestConfirmedRate?.valor)
    }

    private fun snapshot(vararg rates: TasaBcvEntity) = KabySnapshot(
        account = null,
        movements = emptyList(),
        sales = emptyList(),
        rates = rates.toList(),
    )

    private fun rate(value: String, time: String, confirmedBy: String?) = TasaBcvEntity(
        valor = value,
        horaFuente = time,
        fuente = "ESPEJO_DOLARAPI",
        confirmadaPor = confirmedBy,
    )
}
