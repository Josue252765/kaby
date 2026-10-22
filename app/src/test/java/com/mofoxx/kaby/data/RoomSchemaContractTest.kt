package com.mofoxx.kaby.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RoomSchemaContractTest {
    @Test
    fun testRoom_esquemaV1_exportadoConNombresCanonicos() {
        val schemaFile = File("schemas/com.mofoxx.kaby.data.KabyDatabase/1.json")
        assertTrue("Room schema export missing at ${schemaFile.absolutePath}", schemaFile.isFile)
        val entities = JsonParser.parseString(schemaFile.readText())
            .asJsonObject.getAsJsonObject("database").getAsJsonArray("entities")
        val byName = entities.associateBy { it.asJsonObject.get("tableName").asString }
        assertEquals(
            setOf(
                "movimientos", "deudas", "cuotas", "bolsos", "bolso_participantes", "bolso_participaciones",
                "ventas_p2p_mano", "tasas_bcv", "lecturas_binance", "estado_cuentas", "bitacora",
                "bitacora_errores", "settings", "acta_casillas",
            ),
            byName.keys,
        )
        assertColumns(byName, "movimientos", "tipo", "naturaleza", "moneda", "monto", "tasa_bcv_al_momento", "fecha_hora", "proveniencia_tercero", "timestamp_ultima_edicion")
        assertColumns(byName, "tasas_bcv", "valor", "hora_fuente", "fuente", "confirmada_por")
        assertColumns(byName, "estado_cuentas", "ancla_monto", "ancla_fecha_hora", "ultimo_cuadre_at", "estado_cuadre", "cifra_descuadre", "saldo_usdt", "leido_en")
        assertColumns(byName, "ventas_p2p_mano", "bs_netos", "tasa_p2p", "usdt_cantidad", "prima", "order_number", "estado")
    }

    @Test
    fun testRoom_montosGuardadosComoTextoDecimal() {
        val schemaFile = File("schemas/com.mofoxx.kaby.data.KabyDatabase/1.json")
        val entities = JsonParser.parseString(schemaFile.readText())
            .asJsonObject.getAsJsonObject("database").getAsJsonArray("entities")
            .associateBy { it.asJsonObject.get("tableName").asString }
        listOf("movimientos" to "monto", "ventas_p2p_mano" to "bs_netos", "tasas_bcv" to "valor", "deudas" to "saldo_pendiente")
            .forEach { (table, column) ->
                val fields = entities.getValue(table).asJsonObject.getAsJsonArray("fields")
                val field = fields.first { it.asJsonObject.get("columnName").asString == column }.asJsonObject
                assertEquals("TEXT", field.get("affinity").asString)
            }
    }

    private fun assertColumns(entities: Map<String, com.google.gson.JsonElement>, table: String, vararg expected: String) {
        val entity = entities[table]
        assertNotNull("Missing Room entity $table", entity)
        val actual = entity!!.asJsonObject.getAsJsonArray("fields")
            .map { it.asJsonObject.get("columnName").asString }.toSet()
        assertTrue("$table misses columns ${(expected.toSet() - actual)}", actual.containsAll(expected.toSet()))
    }
}
