package com.mofoxx.kaby.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mofoxx.kaby.data.ActaCasillaEntity
import com.mofoxx.kaby.domain.AcceptanceCase
import com.mofoxx.kaby.domain.M1AcceptanceChecklist

@Composable
fun M1ChecklistPage(
    entries: List<ActaCasillaEntity>,
    onBack: () -> Unit,
    onRecord: (String, String, String, String) -> Unit,
    onSeal: () -> Unit,
) {
    val latest = M1AcceptanceChecklist.latestByCase(entries)
    val readyToSeal = M1AcceptanceChecklist.isReadyToSeal(entries)
    val sealed = M1AcceptanceChecklist.isSealed(entries)
    var selectedCase by remember { mutableStateOf<AcceptanceCase?>(null) }
    var confirmSeal by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Prueba por hito · M1", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Registra aquí lo que tú ejecutes en este teléfono. Las casillas sin resultado aparecen como NOT_TESTED; Kaby no las marca por pasar pruebas automáticas.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sealed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("M1 confirmado por el propietario")
                    Text(M1AcceptanceChecklist.latestOwnerConfirmation(entries)?.timestamp.orEmpty())
                }
            }
        }
        M1AcceptanceChecklist.cases.forEach { item ->
            val result = latest[item.id]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.criterion, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Veredicto: ${result?.veredicto ?: "NOT_TESTED"}")
                    result?.causaBloqueo?.takeIf(String::isNotBlank)?.let { Text("Causa: $it") }
                    result?.evidencia?.takeIf(String::isNotBlank)?.let { Text("Evidencia: $it") }
                    result?.let { Text("Registrado: ${it.timestamp}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    OutlinedButton(onClick = { selectedCase = item }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (result == null) "Registrar resultado" else "Actualizar resultado")
                    }
                }
            }
        }
        if (readyToSeal && !sealed) {
            Button(onClick = { confirmSeal = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar M1 como propietario")
            }
        } else if (!sealed) {
            Text(
                "M1 no se puede confirmar todavía: cada casilla debe tener PASS y una referencia de evidencia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver a Ajustes") }
    }

    selectedCase?.let { item ->
        ChecklistResultDialog(
            item = item,
            current = latest[item.id],
            onDismiss = { selectedCase = null },
            onSave = { verdict, cause, evidence ->
                onRecord(item.id, verdict, cause, evidence)
                selectedCase = null
            },
        )
    }

    if (confirmSeal) {
        AlertDialog(
            onDismissRequest = { confirmSeal = false },
            title = { Text("Confirmar M1") },
            text = {
                Text("Este toque registra tu confirmación local de que ejecutaste las diez casillas y revisaste sus evidencias. Kaby no ejecutó ni aprobó esas pruebas por ti.")
            },
            confirmButton = {
                Button(onClick = { confirmSeal = false; onSeal() }) { Text("Confirmo como propietario") }
            },
            dismissButton = { TextButton(onClick = { confirmSeal = false }) { Text("Volver a revisar") } },
        )
    }
}

@Composable
private fun ChecklistResultDialog(
    item: AcceptanceCase,
    current: ActaCasillaEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var verdict by remember(item.id) { mutableStateOf(current?.veredicto ?: "NOT_TESTED") }
    var cause by remember(item.id) { mutableStateOf(current?.causaBloqueo.orEmpty()) }
    var evidence by remember(item.id) { mutableStateOf(current?.evidencia.orEmpty()) }
    var validationMessage by remember(item.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Elige el resultado real de la prueba.")
                M1AcceptanceChecklist.verdicts.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { option ->
                            FilterChip(
                                selected = verdict == option,
                                onClick = { verdict = option; validationMessage = null },
                                label = { Text(option) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = cause,
                    onValueChange = { cause = it; validationMessage = null },
                    label = { Text("Causa del bloqueo (si aplica)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = evidence,
                    onValueChange = { evidence = it; validationMessage = null },
                    label = { Text("Referencia de evidencia") },
                    supportingText = { Text("PASS y FAIL requieren una referencia; puede ser la ubicación de una captura o una nota de contraste.") },
                    modifier = Modifier.fillMaxWidth(),
                )
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val issue = M1AcceptanceChecklist.validationError(verdict, cause, evidence)
                if (issue == null) onSave(verdict, cause, evidence) else validationMessage = issue
            }) { Text("Guardar resultado") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
