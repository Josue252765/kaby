package com.mofoxx.kaby.domain

import com.mofoxx.kaby.data.ActaCasillaEntity

data class AcceptanceCase(
    val id: String,
    val title: String,
    val criterion: String,
)

object M1AcceptanceChecklist {
    const val MILESTONE = "M1"
    const val OWNER_CONFIRMATION_ID = "m1_confirmacion_propietario"

    val cases = listOf(
        AcceptanceCase("ancla", "Ancla", "El saldo inicial y su fecha/hora se ven en pantalla."),
        AcceptanceCase("registro_rapido", "Registro rápido", "Registrar un gasto común requiere como máximo tres toques, sin contar la escritura."),
        AcceptanceCase("saldo_gasto", "Saldo y gastos", "Saldo, gastos y lista por fecha coinciden con el oráculo de TESTING §1.5."),
        AcceptanceCase("ganancia_neta", "Ganancia neta", "La definición acompaña la cifra y el turno no aumenta ingresos ni gastos."),
        AcceptanceCase("proveniencia", "Otra plata que entra", "Un ingreso normal exige quién/por qué y se cuenta en saldo e ingresos."),
        AcceptanceCase("venta_p2p", "Venta P2P manual", "La prima conserva el signo; sin tasa anterior confirmada la venta queda sin valuar."),
        AcceptanceCase("rastro_edicion", "Edición y rastro", "La edición muestra su hora y el rastro CREAR/EDITAR se entiende en palabras."),
        AcceptanceCase("tasas", "Tasa BCV", "La entrada manual funciona; la propuesta del espejo exige confirmación y sin red conserva la última hora."),
        AcceptanceCase("respaldo_saf", "Exportar y restaurar", "SAF exporta e importa en el baúl de prueba y el resultado coincide con el oráculo."),
        AcceptanceCase("errores", "Errores y continuidad", "Los errores de red/tasa/sin valuación muestran su acción y la app sigue navegable."),
    )

    val verdicts = listOf("PASS", "FAIL", "PARTIAL", "BLOCKED", "NOT_TESTED")

    fun latestByCase(entries: List<ActaCasillaEntity>): Map<String, ActaCasillaEntity> =
        entries
            .filter { it.hito == MILESTONE && it.casillaId != OWNER_CONFIRMATION_ID }
            .sortedBy { it.id }
            .associateBy { it.casillaId }

    fun latestOwnerConfirmation(entries: List<ActaCasillaEntity>): ActaCasillaEntity? =
        entries
            .filter { it.hito == MILESTONE && it.casillaId == OWNER_CONFIRMATION_ID }
            .maxByOrNull { it.id }

    fun isReadyToSeal(entries: List<ActaCasillaEntity>): Boolean {
        val latest = latestByCase(entries)
        return cases.all { item ->
            latest[item.id]?.let { it.veredicto == "PASS" && !it.evidencia.isNullOrBlank() } == true
        }
    }

    fun isSealed(entries: List<ActaCasillaEntity>): Boolean {
        val latest = latestByCase(entries)
        val confirmation = latestOwnerConfirmation(entries) ?: return false
        if (confirmation.veredicto != "PASS" || confirmation.evidencia.isNullOrBlank()) return false
        return isReadyToSeal(entries) && cases.all { item -> (latest[item.id]?.id ?: Long.MAX_VALUE) < confirmation.id }
    }

    fun validationError(verdict: String, cause: String, evidence: String): String? {
        if (verdict !in verdicts) return "Elige un veredicto válido."
        return when (verdict) {
            "PASS", "FAIL" -> if (evidence.isBlank()) "Agrega una referencia de evidencia para este resultado." else null
            "PARTIAL" -> if (evidence.isBlank() && cause.isBlank()) "Describe qué se comprobó o qué faltó." else null
            "BLOCKED" -> if (cause.isBlank()) "Explica qué impide ejecutar esta casilla." else null
            else -> null
        }
    }
}
