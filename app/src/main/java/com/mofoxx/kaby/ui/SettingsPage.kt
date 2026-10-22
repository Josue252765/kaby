package com.mofoxx.kaby.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mofoxx.kaby.data.BackupSnapshotCodec
import com.mofoxx.kaby.data.LecturaBinanceEntity
import com.mofoxx.kaby.data.KabySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

@Composable
internal fun SettingsPage(viewModel: KabyViewModel, snapshot: KabySnapshot?, motion: Boolean,
    onMotion: (Boolean) -> Unit, onOpenChecklist: () -> Unit, onRates: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by viewModel.binanceStatus.collectAsState()
    val reads by viewModel.binanceReads.collectAsState()
    val saving by viewModel.isSavingEntry.collectAsState()
    var connecting by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf<LecturaBinanceEntity?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var visibleReads by remember { mutableIntStateOf(5) }
    var backupStatus by remember { mutableStateOf("Comprobando copia privada…") }
    suspend fun inspectBackup(): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "local-backup/kaby-latest.json")
        if (!file.isFile) "Todavía no hay una copia privada local."
        else runCatching { BackupSnapshotCodec.decode(file.readText()) }.getOrNull()?.let {
            "Copia local legible · ${java.time.Instant.ofEpochMilli(file.lastModified()).atZone(java.time.ZoneId.of("America/Caracas"))}"
        } ?: "No pude validar la copia local. Genera una nueva."
    }
    LaunchedEffect(Unit) { backupStatus = inspectBackup() }
    val imported = snapshot?.sales.orEmpty().mapNotNull { it.orderNumber }.toSet()
    val pending = reads.filter { it.orderNumber !in imported }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column(Modifier.padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Conexiones y datos", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Text("Consulta fuentes, revisa el respaldo local y ajusta el movimiento de la app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsSection("Binance", "Consulta privada · solo lectura") {
                Text("USDT disponible · Funding", style = MaterialTheme.typography.labelLarge)
                Text(snapshot?.account?.saldoUsdt?.let { "$it USDT" } ?: "Sin lectura", style = MaterialTheme.typography.headlineSmall)
                Text(snapshot?.account?.leidoEn?.let { "Última lectura: ${displayBinanceTime(it)}" } ?: "Conecta para consultar tu cuenta.")
                Text("Funding disponible no incluye Spot, Earn ni fondos bloqueados. No se suma al saldo de Mercantil.", style = MaterialTheme.typography.bodySmall)
                Surface(
                    color = if (status.detail.startsWith("E-")) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (status.detail.startsWith("E-")) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(status.detail, Modifier.fillMaxWidth().padding(12.dp))
                }
                if (status.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    OutlinedButton(onClick = viewModel::cancelBinance) { Text("Cancelar consulta") }
                } else Button(onClick = { connecting = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Traer datos de Binance") }
                Text("La llave se usa solo para esta consulta; vuelve a pegarla la próxima vez. No se guarda en disco.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsSection("P2P por conciliar · ${pending.size}", "Ventas completadas USDT/VES leídas de Binance") {
                Text("El total de Binance no confirma el neto recibido en tu banco. Revisa cada venta y vincula su registro manual si ya la anotaste.")
                if (reads.isEmpty()) Text("Aún no hay lecturas guardadas. Una consulta completa traerá el período disponible, hasta seis meses.")
                else Text("${reads.size} lecturas guardadas · ${reads.size - pending.size} conciliadas. Una lectura por sí sola no modifica el cuadre.")
            }
        }
        items(pending.take(visibleReads), key = { it.orderNumber }) { read ->
            SettingsSection("${read.amount} USDT", displayBinanceTime(read.fechaHoraOrden)) {
                Text("Total Binance: ${read.totalPrice} VES · tasa ${read.unitPrice}")
                Text("Orden ${read.orderNumber}", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { reviewing = read }, enabled = !saving) { Text("Conciliar con Mercantil") }
            }
        }
        if (pending.size > visibleReads) item {
            OutlinedButton(onClick = { visibleReads += 20 }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver más lecturas · ${minOf(visibleReads, pending.size)} de ${pending.size}")
            }
        }
        item {
            SettingsSection("Respaldo privado", "Una copia dentro de este teléfono") {
                Text(backupStatus)
                Text("No protege frente a pérdida del teléfono o desinstalación. La transferencia por archivos sigue pendiente de elegir su cifrado.", style = MaterialTheme.typography.bodySmall)
                Button(enabled = !backupBusy, onClick = {
                    backupBusy = true
                    scope.launch {
                        try {
                            val ok = viewModel.createPrivateBackup(context)
                            backupStatus = if (ok) inspectBackup() else "No se completó la copia. La anterior se conserva."
                        } catch (_: Exception) { backupStatus = "No se completó la copia. Revisa el almacenamiento del teléfono." }
                        finally { backupBusy = false }
                    }
                }) { Text(if (backupBusy) "Guardando copia…" else "Crear copia ahora") }
            }
        }
        item {
            SettingsSection("Apariencia y accesibilidad", "Respeta la escala de animación de Android.") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Animaciones", Modifier.weight(1f))
                    Switch(checked = motion, onCheckedChange = onMotion)
                }
                Text("La preferencia queda guardada. También se respeta la escala de animaciones de Android.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onRates) { Text("Administrar tasa BCV") }
                OutlinedButton(onClick = onOpenChecklist, modifier = Modifier.heightIn(min = 48.dp)) { Text("Estado de preparación · M1") }
            }
        }
    }
    if (connecting) BinanceCredentialsDialog(onDismiss = { connecting = false }, onRead = { key, secret ->
        connecting = false
        viewModel.readBinance(key, secret)
    })
    reviewing?.let { read ->
        var net by remember(read.orderNumber) { mutableStateOf("") }
        var manualId by remember(read.orderNumber) { mutableStateOf<Long?>(null) }
        var distinct by remember(read.orderNumber) { mutableStateOf(false) }
        var reviewError by remember(read.orderNumber) { mutableStateOf<String?>(null) }
        val candidates = snapshot?.sales.orEmpty().filter { it.fuente == "MANO" && it.estado == "VIVA" && BigDecimal(it.usdtCantidad).compareTo(BigDecimal(read.amount)) == 0 }
        AlertDialog(onDismissRequest = { if (!saving) reviewing = null }, title = { Text("Confirmar venta recibida") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Orden ${read.orderNumber}\n${read.amount} USDT · total Binance ${read.totalPrice} VES")
                OutlinedTextField(value = net, onValueChange = { net = it }, label = { Text("Bs netos recibidos en Mercantil") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), enabled = !saving)
                Text("Si ya la anotaste, elige el puente. Solo ese registro será jubilado; quedará en el rastro.")
                candidates.forEach { sale ->
                    FilterChip(selected = manualId == sale.id, enabled = !saving, onClick = { manualId = sale.id; net = sale.bsNetos; distinct = false },
                        label = { Text("#${sale.id} · ${sale.bsNetos} Bs · ${displayBinanceTime(sale.fechaHora)}") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = distinct, enabled = !saving, onCheckedChange = { distinct = it; if (it) manualId = null })
                    Text("Verifiqué que esta venta no está anotada a mano.", Modifier.weight(1f))
                }
                Text("Revisa también los movimientos de ingreso para evitar duplicarla.", style = MaterialTheme.typography.bodySmall)
                reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            TextButton(enabled = !saving && net.isNotBlank() && (manualId != null || distinct), onClick = {
                viewModel.reconcileBinance(read.orderNumber, net, manualId) {
                    if (it) reviewing = null
                    else reviewError = "No se confirmó el guardado. Usa un neto positivo; si seleccionaste un puente, debe coincidir en Bs y USDT. Revisa si la orden ya fue conciliada."
                }
            }) { Text(if (saving) "Guardando…" else "Confirmar neto") }
        }, dismissButton = { TextButton(enabled = !saving, onClick = { reviewing = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun BinanceCredentialsDialog(onDismiss: () -> Unit, onRead: (String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { key = ""; secret = ""; onDismiss() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); key = ""; secret = "" }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn, usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(20.dp).imePadding(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Conectar Binance", style = MaterialTheme.typography.headlineSmall)
                Text("En Binance → API Management, crea una llave HMAC generada por el sistema. Habilita únicamente lectura. Nunca retiros, trading o transferencias.")
                Text("Pégala solo en tu teléfono. No uses llaves reales en Appetize ni las envíes por chat. El guardado cifrado está pendiente de la prueba de respaldo del teléfono.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(key, { key = it }, label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(secret, { secret = it }, label = { Text("SECRET") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(accepted, { accepted = it })
                    Text("Es mi cuenta y la llave tiene solo lectura.", Modifier.weight(1f))
                }
                Button(enabled = accepted && key.isNotBlank() && secret.isNotBlank(), onClick = {
                    val k = key; val s = secret; key = ""; secret = ""; onRead(k, s)
                }, modifier = Modifier.fillMaxWidth()) { Text("Consultar y olvidar llave") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, detail: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

private fun displayBinanceTime(value: String): String = runCatching {
    java.time.OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.of("America/Caracas"))
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " · Caracas"
}.getOrDefault(value)
