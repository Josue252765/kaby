package com.mofoxx.kaby.data

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSnapshotCodecTest {
    @Test
    fun testRespaldo_jsonLocalVersionadoSeDecodifica() {
        val source = snapshotWithMovement()
        val encoded = BackupSnapshotCodec.encode(source)
        val decoded = BackupSnapshotCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded?.schemaVersion)
        assertEquals(source.generatedAt, decoded?.generatedAt)
        assertEquals(1, decoded?.movements?.size)
        assertEquals("100", decoded?.movements?.single()?.monto)
        assertEquals("Alimentos", decoded?.movements?.single()?.categoria)
    }

    @Test
    fun testRespaldo_noContieneLlave() {
        val encoded = JsonParser.parseString(BackupSnapshotCodec.encode(snapshotWithMovement()))
        assertFalse(hasCredentialField(encoded))
    }

    @Test
    fun testRespaldo_rechazaVersionDeFormatoDesconocida() {
        assertNull(BackupSnapshotCodec.decode("""{"schema_version":2}"""))
        assertNull(BackupSnapshotCodec.decode("esto no es JSON"))
    }

    private fun snapshotWithMovement() = LocalBackupSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-10-21T16:00:00-04:00",
        movements = listOf(
            MovimientoEntity(
                tipo = "GASTO",
                naturaleza = "NORMAL",
                moneda = "Bs",
                monto = "100",
                fechaHora = "2026-10-21T15:00:00-04:00",
                categoria = "Alimentos",
                timestampCreacion = "2026-10-21T15:00:00-04:00",
                timestampUltimaEdicion = "2026-10-21T15:00:00-04:00",
            ),
        ),
        debts = emptyList(),
        installments = emptyList(),
        groups = emptyList(),
        groupParticipants = emptyList(),
        groupContributions = emptyList(),
        sales = emptyList(),
        rates = emptyList(),
        binanceReads = emptyList(),
        accounts = emptyList(),
        auditEntries = emptyList(),
        errors = emptyList(),
        settings = emptyList(),
        checklistEntries = emptyList(),
    )

    private fun hasCredentialField(element: JsonElement): Boolean = when {
        element.isJsonObject -> element.asJsonObject.entrySet().any { (key, value) ->
            val normalized = key.lowercase().filter(Char::isLetterOrDigit)
            listOf("apikey", "secret", "credential", "ciphertext").any(normalized::contains) || hasCredentialField(value)
        }
        element.isJsonArray -> element.asJsonArray.any(::hasCredentialField)
        else -> false
    }
}
