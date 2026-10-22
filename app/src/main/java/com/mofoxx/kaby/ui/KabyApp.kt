package com.mofoxx.kaby.ui

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonParser
import com.mofoxx.kaby.data.BitacoraEntity
import com.mofoxx.kaby.data.EstadoCuentaEntity
import com.mofoxx.kaby.data.KabySnapshot
import com.mofoxx.kaby.data.LecturaBinanceEntity
import com.mofoxx.kaby.data.MovimientoEntity
import com.mofoxx.kaby.data.VentaP2pEntity
import com.mofoxx.kaby.domain.CuadreCalculator
import com.mofoxx.kaby.domain.ReconciliationMath
import com.mofoxx.kaby.domain.MovementForCalculation
import com.mofoxx.kaby.domain.SpendingByDate
import com.mofoxx.kaby.domain.BcvValuation
import com.mofoxx.kaby.tasas.TasaPropuesta
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val CanvasStone = Color(0xFFF3F1EA)
private val SurfaceColor = Color(0xFFFFFEFA)
private val SoftSurface = Color(0xFFE7ECE8)
private val Ink = Color(0xFF172B3A)
private val Paper = Color(0xFFF6F4EE)
private val Muted = Color(0xFF5B686E)
private val Teal = Color(0xFF287366)
private val Terracotta = Color(0xFFB45644)
private val Danger = Color(0xFFA83D3D)
private val Line = Color(0xFFD7DDD9)
private val CardOutline = BorderStroke(1.dp, Line)
private val KabyTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun KabyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8EAE4),
            onPrimaryContainer = Ink,
            inversePrimary = Color(0xFFAAD4C8),
            secondary = Ink,
            onSecondary = Color.White,
            secondaryContainer = SoftSurface,
            onSecondaryContainer = Ink,
            tertiary = Terracotta,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF3DDD6),
            onTertiaryContainer = Ink,
            background = CanvasStone,
            onBackground = Ink,
            surface = SurfaceColor,
            onSurface = Ink,
            surfaceVariant = SoftSurface,
            onSurfaceVariant = Muted,
            inverseSurface = Ink,
            inverseOnSurface = Paper,
            error = Danger,
            onError = Color.White,
            errorContainer = Color(0xFFF4DEDC),
            onErrorContainer = Ink,
            outline = Line,
            outlineVariant = Color(0xFFE7EAE6),
            surfaceTint = Teal,
        ),
        typography = KabyTypography,
        content = content,
    )
}

private enum class Page(val navLabel: String, val title: String = navLabel) {
    HOME("Inicio"), MOVEMENTS("Registro", "Movimientos"), SALES("P2P"), RATES("Tasas"), SETTINGS("Ajustes"), FORM("Registrar"), CHECKLIST("Prueba M1")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KabyApp(viewModel: KabyViewModel) {
    KabyTheme {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = androidx.compose.ui.platform.LocalContext.current
        val preferences = remember(context) { context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE) }
        val systemMotionScale = remember(context) {
            runCatching { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
                .getOrDefault(1f)
        }
        var motion by rememberSaveable { mutableStateOf(preferences.getBoolean("motion", true)) }
        val pageOpacity = remember { androidx.compose.animation.core.Animatable(1f) }
        val ui by viewModel.state.collectAsState()
        val binanceReads by viewModel.binanceReads.collectAsState()
        val isSavingEntry by viewModel.isSavingEntry.collectAsState()
        val isFetchingRate by viewModel.isFetchingRate.collectAsState()
        val recentProvenances = remember(ui.snapshot?.movements) {
            recentThirdPartySuggestions(ui.snapshot?.movements.orEmpty())
        }
        val snackbarHostState = remember { SnackbarHostState() }
        var page by rememberSaveable { mutableStateOf(Page.HOME) }
        var editingMovement by remember { mutableStateOf<MovimientoEntity?>(null) }
        var editingSale by remember { mutableStateOf<VentaP2pEntity?>(null) }
        var formReturnPage by rememberSaveable { mutableStateOf(Page.HOME) }
        var formIsSale by rememberSaveable { mutableStateOf(false) }
        var formNature by rememberSaveable { mutableStateOf("NORMAL") }
        var formDirection by rememberSaveable { mutableStateOf("GASTO") }
        var formAmount by rememberSaveable { mutableStateOf("") }
        var auditTitle by remember { mutableStateOf<String?>(null) }

        BackHandler(enabled = isSavingEntry) { }
        BackHandler(enabled = !isSavingEntry && page != Page.HOME) {
            when (page) {
                Page.FORM -> {
                    editingMovement = null
                    editingSale = null
                    formIsSale = false
                    page = formReturnPage
                }
                Page.CHECKLIST -> page = Page.SETTINGS
                else -> page = Page.HOME
            }
        }

        LaunchedEffect(ui.message, page) {
            val message = ui.message ?: return@LaunchedEffect
            if (page != Page.RATES && (message.startsWith("E-SIN-RED") || message.startsWith("E-TASAS-SIN-FUENTE"))) {
                return@LaunchedEffect
            }
            val isError = message.startsWith("E-")
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = isError,
                duration = if (isError) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            viewModel.clearMessage()
        }

        DisposableEffect(lifecycleOwner, viewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) viewModel.fetchRate()
                if (event == Lifecycle.Event.ON_STOP) viewModel.cancelBinance()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(page, motion, systemMotionScale) {
            if (motion && systemMotionScale > 0f) {
                pageOpacity.snapTo(.65f)
                pageOpacity.animateTo(1f, androidx.compose.animation.core.tween((180 * systemMotionScale).toInt().coerceAtMost(300)))
            } else pageOpacity.snapTo(1f)
            if (page == Page.RATES) viewModel.fetchRate()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("KABY", style = MaterialTheme.typography.labelLarge, color = Teal, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                            Text(page.title, style = MaterialTheme.typography.titleMedium, color = Ink)
                        }
                    },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = CanvasStone,
                titleContentColor = Ink,
                scrolledContainerColor = CanvasStone,
            ),
                )
            },
            bottomBar = {
                if (page != Page.FORM && page != Page.CHECKLIST) {
                    Surface(color = SurfaceColor, shadowElevation = 4.dp, border = BorderStroke(1.dp, Line)) {
                        NavigationBar(containerColor = SurfaceColor, tonalElevation = 0.dp) {
                            listOf(Page.HOME, Page.MOVEMENTS, Page.SALES, Page.RATES, Page.SETTINGS).forEach { destination ->
                                NavigationBarItem(
                                    selected = page == destination,
                                    onClick = { page = destination },
                                    icon = { DestinationIcon(destination) },
                                    label = { Text(destination.navLabel, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Teal,
                                        selectedTextColor = Ink,
                                        indicatorColor = Color(0xFFD8EAE4),
                                        unselectedIconColor = Muted,
                                        unselectedTextColor = Muted,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    if (data.visuals.message.startsWith("E-")) {
                        MessageCard(
                            message = data.visuals.message,
                            onDismiss = data::dismiss,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    } else {
                        androidx.compose.material3.Snackbar(
                            action = {
                                TextButton(onClick = data::dismiss) { Text("Cerrar") }
                            },
                            containerColor = Ink,
                            contentColor = Paper,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(data.visuals.message, maxLines = 2)
                        }
                    }
                }
            },
            floatingActionButton = {
                if (page == Page.HOME || page == Page.MOVEMENTS) {
                    FloatingActionButton(onClick = {
                        formReturnPage = page
                        editingMovement = null
                        editingSale = null
                        formIsSale = false
                        formNature = "NORMAL"
                        formDirection = "GASTO"
                        formAmount = ""
                        page = Page.FORM
                        }, modifier = Modifier.semantics { contentDescription = "Registrar movimiento" }, containerColor = Teal, contentColor = Color.White) {
                        Text("+", fontSize = 28.sp, fontWeight = FontWeight.Normal)
                    }
                }
            },
            containerColor = CanvasStone,
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).graphicsLayer { alpha = pageOpacity.value }) {
                when (page) {
                    Page.HOME -> HomePage(
                        snapshot = ui.snapshot,
                        loading = ui.loading,
                        loadError = ui.loadError,
                        binanceReads = binanceReads,
                        onOpenMovements = { page = Page.MOVEMENTS },
                        onOpenRates = { page = Page.RATES },
                        onOpenSettings = { page = Page.SETTINGS },
                        onOpenSales = { page = Page.SALES },
                        onAnchor = viewModel::saveAnchor,
                        onReconcile = viewModel::reconcile,
                        onAdjustment = { direction, amount ->
                            formReturnPage = Page.HOME
                            editingMovement = null
                            editingSale = null
                            formIsSale = false
                            formNature = "AJUSTE_CUADRE"
                            formDirection = direction
                            formAmount = amount
                            page = Page.FORM
                        },
                    )
                    Page.MOVEMENTS -> MovementPage(
                        movements = ui.snapshot?.movements.orEmpty(),
                        onAdd = {
                            formReturnPage = Page.MOVEMENTS
                            editingMovement = null
                            editingSale = null
                            formIsSale = false
                            formNature = "NORMAL"
                            formDirection = "GASTO"
                            formAmount = ""
                            page = Page.FORM
                        },
                        onEdit = { formReturnPage = Page.MOVEMENTS; editingMovement = it; page = Page.FORM },
                        onAudit = { auditTitle = "Rastro del movimiento"; viewModel.loadAudit("movimientos", it.id) },
                    )
                    Page.SALES -> SalesPage(
                        sales = ui.snapshot?.sales.orEmpty(),
                        onAdd = { formReturnPage = Page.SALES; editingSale = null; formIsSale = true; page = Page.FORM },
                        onEdit = { formReturnPage = Page.SALES; editingSale = it; formIsSale = true; page = Page.FORM },
                        onAudit = { auditTitle = "Rastro de la venta"; viewModel.loadAudit("ventas_p2p_mano", it.id) },
                    )
                    Page.RATES -> RatesPage(
                        snapshot = ui.snapshot,
                        proposal = ui.proposal,
                        isFetching = isFetchingRate,
                        onManualRate = viewModel::saveManualRate,
                        onFetch = viewModel::fetchRate,
                        onConfirm = viewModel::confirmProposal,
                    )
                    Page.SETTINGS -> SettingsPage(viewModel, ui.snapshot, motion,
                        onMotion = { motion = it; preferences.edit { putBoolean("motion", it) } },
                        onOpenChecklist = { page = Page.CHECKLIST }, onRates = { page = Page.RATES })
                    Page.CHECKLIST -> M1ChecklistPage(
                        entries = ui.m1ChecklistEntries,
                        onBack = { page = Page.SETTINGS },
                        onRecord = viewModel::recordM1ChecklistResult,
                        onSeal = viewModel::sealM1ByOwner,
                    )
                    Page.FORM -> {
                        if (formIsSale) {
                            SaleForm(editingSale, isSavingEntry, onSave = { id, net, p2p, usdt, date, provenance ->
                                viewModel.saveSale(id, net, p2p, usdt, date, provenance, onComplete = { succeeded ->
                                    if (succeeded) {
                                        editingSale = null
                                        formIsSale = false
                                        page = formReturnPage
                                    }
                                })
                            }, onCancel = { editingSale = null; formIsSale = false; page = formReturnPage })
                        } else {
                            MovementForm(editingMovement, initialNature = formNature, initialDirection = formDirection, initialAmount = formAmount, recentProvenances = recentProvenances, isSaving = isSavingEntry, onSave = { id, direction, nature, currency, amount, date, category, note, thirdParty ->
                                viewModel.saveMovement(id, direction, nature, currency, amount, date, category, note, thirdParty, onComplete = { succeeded ->
                                    if (succeeded) {
                                        editingMovement = null
                                        formNature = "NORMAL"
                                        page = formReturnPage
                                    }
                                })
                            }, onCancel = { editingMovement = null; page = formReturnPage })
                        }
                    }
                }
                if (auditTitle != null) {
                    AlertDialog(
                        onDismissRequest = { auditTitle = null },
                        title = { Text(auditTitle!!) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ui.auditEntries.isEmpty()) Text("Todavía no hay cambios registrados.")
                                ui.auditEntries.forEach { Text(auditDescription(it)) }
                            }
                        },
                        confirmButton = { TextButton(onClick = { auditTitle = null }) { Text("Cerrar") } },
                        containerColor = SurfaceColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    snapshot: KabySnapshot?,
    loading: Boolean,
    loadError: Boolean,
    binanceReads: List<LecturaBinanceEntity>,
    onOpenMovements: () -> Unit,
    onOpenRates: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSales: () -> Unit,
    onAnchor: (String, String) -> Unit,
    onReconcile: (String, String, String) -> Unit,
    onAdjustment: (String, String) -> Unit,
) {
    if (snapshot == null) {
        SectionCard(
            title = "SALDO MERCANTIL",
            detail = if (loadError) "No pude leer los datos guardados en este teléfono. Reabre Kaby para volver a intentarlo."
                else if (loading) "Cargando registros locales…" else "Sin datos disponibles.",
        ) { Text("—", style = MaterialTheme.typography.displaySmall) }
        return
    }
    val account = snapshot?.account
    val summary = snapshot?.summary
    var anchorAmount by rememberSaveable { mutableStateOf("") }
    var anchorDate by rememberSaveable { mutableStateOf(nowCaracasText()) }
    var realBalance by rememberSaveable { mutableStateOf("") }
    var realDate by rememberSaveable { mutableStateOf(nowCaracasText()) }
    var showReconcile by rememberSaveable { mutableStateOf(false) }
    var reconcileError by remember { mutableStateOf<String?>(null) }
    var review by remember { mutableStateOf<Pair<BigDecimal, BigDecimal>?>(null) }
    var showGainDefinition by rememberSaveable { mutableStateOf(false) }
    val zone = ZoneId.of("America/Caracas")
    val todaySpend = SpendingByDate.forDate(
        snapshot?.movements.orEmpty().map(MovimientoEntity::toMovementForCalculation),
        LocalDate.now(zone),
        zone,
    )
    val status = summary?.reconciliationState ?: "SIN_ANCLA"
    val laterBankRows = account?.ultimoCuadreAt?.let { checkedAt ->
        val cut = CuadreCalculator.instantOrNull(checkedAt)
        cut == null || snapshot.movements.any { CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(cut) == true && it.moneda == "Bs" } ||
            snapshot.sales.any { it.estado == "VIVA" && CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(cut) == true }
    } ?: true
    val squaredAtCut = status == "CALCULADO" && account?.ultimoCuadreAt != null && !laterBankRows
    val rate = snapshot?.latestConfirmedRate
    val today = LocalDate.now(zone)
    val todayDirectIncome = snapshot.movements.asSequence()
        .filter { it.tipo == "INGRESO" && it.naturaleza == "NORMAL" && it.moneda == "Bs" }
        .filter { CuadreCalculator.instantOrNull(it.fechaHora)?.atZone(zone)?.toLocalDate() == today }
        .fold(BigDecimal.ZERO) { total, row -> total.add(BigDecimal(row.monto)) }
    val todayP2p = snapshot.sales.asSequence().filter { it.estado == "VIVA" }
        .filter { CuadreCalculator.instantOrNull(it.fechaHora)?.atZone(zone)?.toLocalDate() == today }
        .fold(BigDecimal.ZERO) { total, row -> total.add(BigDecimal(row.bsNetos)) }
    val pendingP2p = binanceReads.count { read -> snapshot.sales.none { it.orderNumber == read.orderNumber } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("TU RESUMEN", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(formatCaracasDay(LocalDate.now(zone)), style = MaterialTheme.typography.titleMedium, color = Muted)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink, contentColor = Paper),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color(0xFF304555)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SALDO MERCANTIL", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC1D0D2), letterSpacing = 1.sp)
                        ReconciliationPill(
                            label = when (status) {
                                "DESCUADRE_ABIERTO" -> "Descuadre abierto"
                                "CALCULADO" -> if (squaredAtCut) "Cuadrado al corte" else "Calculado"
                                else -> "Falta ancla"
                            },
                            isWarning = status == "DESCUADRE_ABIERTO",
                        )
                    }
                    Text(
                        summary?.mercantilBs?.let(::formatBs) ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                    )
                    Text(
                        if (account?.anclaMonto == null) "Registra el saldo real del banco para iniciar."
                        else "Desde tu ancla y los registros guardados.",
                        color = Color(0xFFD5DEDF),
                    )
                    if (summary?.mercantilBs != null && rate != null) {
                        val dollars = BcvValuation.usdEquivalent(summary.mercantilBs, BigDecimal(rate.valor))
                        Surface(color = Color(0xFF243D4A), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Equivalencia referencial", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC1D0D2))
                                    Text(dollars?.let(::formatUsd) ?: "Sin valuar", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                }
                                Text("BCV ${formatRate(rate.valor)}", color = Color(0xFFD5DEDF), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Tasa confirmada por ti · ${formatDate(rate.horaFuente)}", color = Color(0xFFC1D0D2), style = MaterialTheme.typography.bodySmall)
                    } else if (rate == null) {
                        Text("Equivalencia USD no disponible sin BCV confirmado.", color = Color(0xFFD5DEDF))
                        TextButton(onClick = onOpenRates) { Text("Revisar tasa BCV", color = Color(0xFF9FD5C8)) }
                    }
                    if (status == "DESCUADRE_ABIERTO") {
                        val discrepancy = summary?.discrepancyBs ?: BigDecimal.ZERO
                        Text("Diferencia observada · ${formatBs(discrepancy)}", color = Color(0xFFFFC4B8), fontWeight = FontWeight.SemiBold)
                        account?.ultimoCuadreAt?.let { Text("Comparado · ${formatDate(it)}", color = Color(0xFFC1D0D2), style = MaterialTheme.typography.bodySmall) }
                        Button(
                            onClick = {
                                onAdjustment(
                                    if (discrepancy.signum() >= 0) "INGRESO" else "GASTO",
                                    discrepancy.abs().stripTrailingZeros().toPlainString(),
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3DDD6), contentColor = Ink),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Registrar ajuste de cuadre") }
                    } else if (account?.anclaMonto != null) {
                        Text(
                            account.ultimoCuadreAt?.let { "Última comparación · ${formatDate(it)}" }
                                ?: "Calculado desde el ancla · todavía sin cuadre real",
                            color = Color(0xFFC1D0D2),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if ((summary?.unvaluedRows ?: 0) > 0) {
                        Text("${summary?.unvaluedRows} registro(s) sin tasa; no se contaron como cero.", color = Color(0xFFFFC4B8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (account?.anclaMonto == null) item {
            SectionCard(title = "Iniciar cuenta", detail = "Saldo real de Mercantil al comenzar") {
                OutlinedTextField(anchorAmount, { anchorAmount = it }, label = { Text("Saldo inicial · Bs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                DateField(anchorDate, { anchorDate = it })
                PrimaryButton("Guardar saldo inicial") { onAnchor(anchorAmount, anchorDate) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ServiceTile(
                    label = "BCV",
                    value = rate?.let { "${formatRate(it.valor)} Bs/USD" } ?: "Sin tasa confirmada",
                    detail = rate?.let { "Confirmada por ti · ${formatDate(it.horaFuente)}" } ?: "Consultar o registrar tasa",
                    onClick = onOpenRates,
                    modifier = Modifier.weight(1f),
                )
                ServiceTile(
                    label = "BINANCE · FUNDING",
                    value = account?.leidoEn?.let { "${account.saldoUsdt ?: "—"} USDT" } ?: "Sin lectura guardada",
                    detail = account?.leidoEn?.let { "Lectura · ${formatDate(it)}" } ?: "Consulta desde Ajustes",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionCard(title = "Hoy", detail = "Movimientos guardados · hora Caracas") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricValue(
                        title = "Gastos",
                        value = formatBs(todaySpend.amount),
                        detail = if (todaySpend.unvaluedRows > 0) "Parcial · falta tasa" else "Bs y USDT valuados",
                        modifier = Modifier.weight(1f),
                    )
                    MetricValue(
                        title = "Entradas directas",
                        value = formatBs(todayDirectIncome),
                        detail = "Terceros · Bs",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (todayP2p.signum() != 0) Text("Ventas P2P netas · ${formatBs(todayP2p)}", color = Muted)
                TextButton(onClick = { showGainDefinition = true }) {
                    Text("Ganancia neta acumulada · ${summary?.netGainBs?.let(::formatBs) ?: "—"}${if ((summary?.unvaluedRows ?: 0) > 0) " · parcial" else ""}")
                }
                if (todaySpend.unvaluedRows > 0) Text("${todaySpend.unvaluedRows} gasto(s) sin equivalencia confirmada; el total es parcial.", color = Terracotta)
            }
        }
        if (account?.anclaMonto == null || rate == null || status == "DESCUADRE_ABIERTO" || pendingP2p > 0) item {
            SectionCard(title = "Pendientes", detail = "Según tus registros locales") {
                if (account?.anclaMonto == null) Text("Configurar saldo inicial de Mercantil", color = Terracotta)
                if (rate == null) TextButton(onClick = onOpenRates) { Text("Confirmar tasa BCV") }
                if (status == "DESCUADRE_ABIERTO") Text("Revisar diferencia ${summary?.discrepancyBs?.let(::formatBs) ?: "—"}", color = Terracotta)
                if (pendingP2p > 0) TextButton(onClick = onOpenSettings) { Text("$pendingP2p venta(s) Binance por conciliar") }
            }
        }
        item {
            SectionCard(title = "Ir a", detail = "Detalle de tus registros") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenMovements, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Registro") }
                    OutlinedButton(onClick = onOpenSales, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("P2P") }
                }
            }
        }
        if (account?.anclaMonto != null) item {
            SectionCard(title = "Cuadrar con Mercantil", detail = "Compara con el saldo del banco en la misma fecha y hora.") {
                if (showReconcile) {
                    OutlinedTextField(realBalance, { realBalance = it }, label = { Text("Saldo observado · Bs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    DateField(realDate, { realDate = it })
                    reconcileError?.let { Text(it, color = Danger) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PrimaryButton("Comparar", modifier = Modifier.weight(1f)) {
                            review = homeReconciliationPreview(snapshot, realBalance, realDate)
                            reconcileError = if (review == null) "Revisa saldo y fecha: no pueden ser futuros ni anteriores al ancla." else null
                        }
                        OutlinedButton(onClick = { showReconcile = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") }
                    }
                } else PrimaryButton("Iniciar cuadre") { showReconcile = true }
            }
        }
    }
    review?.let { (observed, calculated) ->
        val difference = ReconciliationMath.againstObserved(observed, calculated)
        AlertDialog(
            onDismissRequest = { review = null },
            title = { Text("Confirmar comparación") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Corte · $realDate (Caracas)")
                    Text("Calculado · ${formatBs(calculated)}")
                    Text("Observado · ${formatBs(observed)}")
                    Text("Diferencia · ${formatBs(difference)}")
                    if (difference.signum() != 0) Text("Quedará abierta hasta registrar una corrección con motivo.")
                }
            },
            confirmButton = { TextButton(onClick = {
                onReconcile(realBalance, realDate, calculated.toPlainString())
                review = null
                showReconcile = false
            }) { Text("Confirmar cuadre") } },
            dismissButton = { TextButton(onClick = { review = null }) { Text("Volver") } },
            containerColor = SurfaceColor,
        )
    }
    if (showGainDefinition) {
        AlertDialog(
            onDismissRequest = { showGainDefinition = false },
            title = { Text("Ganancia neta acumulada") },
            text = { Text("Ingresos normales y ventas P2P vivas menos gastos normales valuados. No cuenta turnos del bolso ni ajustes de cuadre. Los registros sin tasa no se tratan como cero.") },
            confirmButton = { TextButton(onClick = { showGainDefinition = false }) { Text("Entendido") } },
            containerColor = SurfaceColor,
        )
    }
}

internal fun homeReconciliationPreview(snapshot: KabySnapshot, observedText: String, localDateTime: String): Pair<BigDecimal, BigDecimal>? {
    val observed = runCatching {
        val raw = observedText.trim().replace(" ", "")
        BigDecimal(if (raw.contains(',') && raw.contains('.')) raw.replace(".", "").replace(',', '.') else raw.replace(',', '.'))
    }.getOrNull()?.takeIf { it >= BigDecimal.ZERO } ?: return null
    val cut = runCatching {
        LocalDateTime.parse(localDateTime.trim(), KabyViewModel.DATE_TIME_FORMAT)
            .atZone(ZoneId.of("America/Caracas")).toInstant()
    }.getOrNull() ?: return null
    val anchor = CuadreCalculator.instantOrNull(snapshot.account?.anclaFechaHora) ?: return null
    if (cut.isBefore(anchor) || cut.isAfter(java.time.Instant.now())) return null
    val calculated = KabySnapshot(
        snapshot.account,
        snapshot.movements.filter { CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(cut) == false },
        snapshot.sales.filter { CuadreCalculator.instantOrNull(it.fechaHora)?.isAfter(cut) == false },
        snapshot.rates,
    ).summary.mercantilBs ?: return null
    return observed to calculated
}

@Composable
private fun ServiceTile(label: String, value: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).semantics(mergeDescendants = true) { contentDescription = "$label. $value. $detail" },
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = CardOutline,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Teal, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2)
        }
    }
}

@Composable
private fun ReconciliationPill(label: String, isWarning: Boolean) {
    Surface(
        color = if (isWarning) Color(0xFFF3DDD6) else Color(0xFFDDECE6),
        contentColor = if (isWarning) Danger else Teal,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (isWarning) Danger.copy(alpha = 0.22f) else Teal.copy(alpha = 0.18f)),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MovementPage(
    movements: List<MovimientoEntity>,
    onAdd: () -> Unit,
    onEdit: (MovimientoEntity) -> Unit,
    onAudit: (MovimientoEntity) -> Unit,
) {
    val zone = ZoneId.of("America/Caracas")
    var filter by rememberSaveable { mutableStateOf("TODOS") }
    val filtered = when (filter) {
        "GASTO" -> movements.filter { it.tipo == "GASTO" }
        "INGRESO" -> movements.filter { it.tipo == "INGRESO" }
        else -> movements
    }
    val allSpending = SpendingByDate.all(filtered.map(MovimientoEntity::toMovementForCalculation))
    val spendingLabel = if (filtered.any { it.tipo == "GASTO" && it.naturaleza == "NORMAL" && it.moneda == "USDT" && it.tasaBcvAlMomento == null }) {
        "Gastos valuados"
    } else "Gastos"
    val directIncome = directIncomeBs(filtered)
    val movementsByDate = filtered.groupBy { localCaracasDate(it.fechaHora) }
        .toSortedMap(compareByDescending { it })
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "Tu actividad", detail = "Gastos e ingresos anotados, agrupados por fecha.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TODOS", "GASTO", "INGRESO").forEach { value ->
                        FilterChip(
                            selected = filter == value,
                            onClick = { filter = value },
                            label = { Text(when (value) { "GASTO" -> "Gastos"; "INGRESO" -> "Ingresos"; else -> "Todo" }) },
                        )
                    }
                }
                Text(when (filter) {
                    "GASTO" -> "$spendingLabel · ${formatBs(allSpending.amount)}"
                    "INGRESO" -> "Entradas directas · ${formatBs(directIncome)}"
                    else -> "$spendingLabel · ${formatBs(allSpending.amount)} · Entradas directas · ${formatBs(directIncome)}"
                }, color = Muted)
                if (filter != "INGRESO") Text(spendingUsdDetail(allSpending), color = Muted)
                if (allSpending.unvaluedRows > 0 && filter != "INGRESO") {
                    Text("${allSpending.unvaluedRows} gasto(s) sin tasa confirmada no tienen equivalencia completa.", color = Terracotta)
                }
                if (movements.isNotEmpty()) OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Registrar movimiento") }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyCard(
                    when {
                        movements.isEmpty() -> "Todavía no hay movimientos. Registra un gasto o una entrada y aquí verás el historial por día."
                        filter == "GASTO" -> "No hay gastos en el historial guardado."
                        filter == "INGRESO" -> "No hay ingresos en el historial guardado."
                        else -> "No hay registros para este filtro."
                    },
                )
            }
        }
        movementsByDate.forEach { (date, dayMovements) ->
            val dailySpend = SpendingByDate.forDate(
                dayMovements.map(MovimientoEntity::toMovementForCalculation),
                date,
                zone,
            )
            item(key = "day-$date") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(formatCaracasDay(date), style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("${dayMovements.size} ${if (dayMovements.size == 1) "registro" else "registros"}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text(
                        if (filter == "INGRESO") "Entradas · ${formatBs(directIncomeBs(dayMovements))}"
                        else "Gastos · ${formatBs(dailySpend.amount)}",
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            items(dayMovements, key = { it.id }) { movement ->
                val valuation = movementDisplayValuation(movement.monto, movement.moneda, movement.tasaBcvAlMomento)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(18.dp), border = CardOutline,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(movement.categoria, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                                Text("${humanizeMovementKind(movement.tipo)} · ${formatDate(movement.fechaHora)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatAmount(movement.monto, movement.moneda), color = if (movement.tipo == "INGRESO") Teal else Ink, fontWeight = FontWeight.Bold)
                        }
                        if (hasBeenEdited(movement.timestampCreacion, movement.timestampUltimaEdicion)) {
                            Text("Editado · ${formatDate(movement.timestampUltimaEdicion)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (valuation != null) {
                            Text("Referencia · ${formatUsd(valuation.usd)} · BCV ${formatRate(valuation.rate.toPlainString())} Bs/USD", color = Muted, style = MaterialTheme.typography.bodySmall)
                        } else if (movement.moneda == "USDT") {
                            Text("Sin equivalencia: falta una tasa BCV confirmada anterior al registro.", color = Terracotta, style = MaterialTheme.typography.bodySmall)
                        }
                        movement.motivo?.takeIf(String::isNotBlank)?.let { Text(it, color = Muted) }
                        movement.provenienciaTercero?.takeIf(String::isNotBlank)?.let { Text("De quién y por qué: $it", color = Muted) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onEdit(movement) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar") }
                            OutlinedButton(onClick = { onAudit(movement) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver cambios") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesPage(sales: List<VentaP2pEntity>, onAdd: () -> Unit, onEdit: (VentaP2pEntity) -> Unit, onAudit: (VentaP2pEntity) -> Unit) {
    var showPremiumDefinition by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = "Ventas P2P", detail = "Registro de USDT vendido y bolívares netos confirmados.") {
                Text("Una lectura de Binance no confirma el neto recibido en Mercantil. Concíliala antes de sumarla.", color = Muted)
                PrimaryButton("Registrar venta P2P") { onAdd() }
            }
        }
        if (sales.isEmpty()) item {
            EmptyCard("Aún no hay ventas guardadas. Cuando registres una, aquí verás el monto recibido, la tasa P2P y la prima frente al BCV.")
        }
        items(sales, key = { it.id }) { sale ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(20.dp), border = CardOutline,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${formatUsdt(BigDecimal(sale.usdtCantidad))} USDT", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                            Text(formatDate(sale.fechaHora), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        StateTag(if (sale.estado == "VIVA") "ACTIVA" else humanizeSaleState(sale.estado), isWarning = sale.estadoValuacion == "SIN VALUAR")
                    }
                    androidx.compose.material3.HorizontalDivider(color = Line)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bs netos", color = Muted)
                        Text(formatBs(BigDecimal(sale.bsNetos)), color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tasa P2P", color = Muted)
                        Text("${formatRate(sale.tasaP2p)} Bs/USDT", color = Ink)
                    }
                    Text(
                        sale.prima?.let { "Prima P2P · ${formatBs(BigDecimal(it))}" } ?: "Prima sin valuar · falta tasa BCV confirmada anterior",
                        color = if (sale.prima == null) Terracotta else Teal,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { showPremiumDefinition = true },
                    )
                    if (sale.estadoValuacion == "SIN VALUAR") Text("Registra una tasa BCV anterior a la venta y luego edítala para calcular la prima.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("${if (sale.fuente == "API") "Orden leída de Binance · neto confirmado por ti" else "Registro manual"} · ${humanizeSaleState(sale.estado)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    if (hasBeenEdited(sale.timestampCreacion, sale.timestampUltimaEdicion)) Text("Editada · ${formatDate(sale.timestampUltimaEdicion)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sale.fuente == "MANO" && sale.estado == "VIVA") OutlinedButton(onClick = { onEdit(sale) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar") }
                        OutlinedButton(onClick = { onAudit(sale) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ver cambios") }
                    }
                }
            }
        }
    }
    if (showPremiumDefinition) {
        AlertDialog(
            onDismissRequest = { showPremiumDefinition = false },
            title = { Text("Prima P2P") },
            text = { Text("Bolívares netos recibidos menos (USDT vendidos × tasa BCV confirmada antes de la venta). Se redondea por venta y conserva el signo: puede ser positiva o negativa.") },
            confirmButton = { TextButton(onClick = { showPremiumDefinition = false }) { Text("Entendido") } },
            containerColor = SurfaceColor,
        )
    }
}

@Composable
private fun RatesPage(
    snapshot: KabySnapshot?,
    proposal: TasaPropuesta?,
    isFetching: Boolean,
    onManualRate: (String, String) -> Unit,
    onFetch: () -> Unit,
    onConfirm: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    var dateTime by rememberSaveable { mutableStateOf(nowCaracasText()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val current = snapshot?.latestConfirmedRate
            SectionCard(title = "Referencia BCV", detail = "La tasa que Kaby usa debe conservar su fuente y hora.") {
                if (current == null) {
                    Text("No hay una tasa confirmada. Las equivalencias que la necesitan se muestran como incompletas.", color = Muted)
                } else {
                    Text("${formatRate(current.valor)} Bs/USD", style = MaterialTheme.typography.displaySmall, color = Ink)
                    StateTag(if (current.fuente == "MANUAL") "DEL TABLERO" else "ESPEJO NO OFICIAL", isWarning = current.fuente != "MANUAL")
                    Text("Confirmada por ti · ${formatDate(current.horaFuente)}", color = Muted)
                }
                PrimaryButton(
                    label = if (isFetching) "Consultando fuente…" else "Consultar tasa propuesta",
                    enabled = !isFetching,
                    loading = isFetching,
                ) { onFetch() }
                proposal?.let { ProposedRate(it, onConfirm) }
            }
        }
        item {
            SectionCard(title = "Registrar desde el tablero", detail = "Copia el valor y la hora que ves en la fuente del BCV.") {
                OutlinedTextField(value, { value = it }, label = { Text("Bolívares por USD") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
                DateField(dateTime, { dateTime = it })
                PrimaryButton("Guardar tasa confirmada") { onManualRate(value, dateTime) }
            }
        }
        item { Text("Historial confirmado", style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(top = 6.dp)) }
        if (snapshot?.rates.isNullOrEmpty()) {
            item { EmptyCard("Las tasas que confirmes aparecerán aquí con su fuente y hora.") }
        }
        items(snapshot?.rates.orEmpty(), key = { it.id }) { rate ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(16.dp), border = CardOutline,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${formatRate(rate.valor)} Bs/USD", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("${rate.fuente.replace('_', ' ')} · ${formatDate(rate.horaFuente)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    StateTag(if (rate.confirmadaPor == "HUMANO") "CONFIRMADA" else "PENDIENTE", isWarning = rate.confirmadaPor != "HUMANO")
                }
            }
        }
    }
}

@Composable
private fun ProposedRate(proposal: TasaPropuesta, onConfirm: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F5F2)),
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFFC7DCD4)),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${proposal.proveedor} · propuesta no oficial", color = Ink, fontWeight = FontWeight.SemiBold)
                StateTag("ESPEJO", isWarning = true)
            }
            Text("${formatRate(proposal.valor.toPlainString())} Bs/USD", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text("Hora publicada por la fuente · ${formatDate(proposal.horaFuente.toString())}", color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("No se guarda hasta que la confirmes. Contrasta el valor con el tablero del BCV.", color = Muted)
            PrimaryButton("Confirmar propuesta") { onConfirm() }
        }
    }
}

@Composable
private fun StateTag(label: String, isWarning: Boolean = false) {
    val foreground = if (isWarning) Terracotta else Teal
    val background = if (isWarning) Color(0xFFF3DDD6) else Color(0xFFDDECE6)
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, foreground.copy(alpha = .18f))) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = .35.sp, maxLines = 1)
    }
}

@Composable
private fun MovementForm(
    initial: MovimientoEntity?,
    initialNature: String,
    initialDirection: String,
    initialAmount: String,
    recentProvenances: List<String>,
    isSaving: Boolean,
    onSave: (Long?, String, String, String, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var direction by remember(initial?.id, initialDirection) { mutableStateOf(initial?.tipo ?: initialDirection) }
    var nature by remember(initial?.id, initialNature) { mutableStateOf(initial?.naturaleza ?: initialNature) }
    var currency by remember(initial?.id, initialDirection) { mutableStateOf(initial?.moneda ?: "Bs") }
    var amount by remember(initial?.id, initialAmount) { mutableStateOf(initial?.monto ?: initialAmount) }
    var category by remember(initial?.id, initialNature) { mutableStateOf(initial?.categoria ?: if (initialNature == "AJUSTE_CUADRE") "Ajuste de cuadre" else "") }
    var note by remember(initial?.id, initialNature) { mutableStateOf(initial?.motivo ?: if (initialNature == "AJUSTE_CUADRE") "Cierre del descuadre" else "") }
    var thirdParty by remember(initial?.id) { mutableStateOf(initial?.provenienciaTercero.orEmpty()) }
    var dateTime by remember(initial?.id) { mutableStateOf(initial?.fechaHora?.let(::localDateTimeText) ?: nowCaracasText()) }
    var showNote by remember(initial?.id, initialNature) { mutableStateOf(!initial?.motivo.isNullOrBlank() || initialNature == "AJUSTE_CUADRE") }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (initial == null) "Registrar movimiento" else "Editar movimiento", style = MaterialTheme.typography.headlineSmall)
            Text("Empieza por el monto. La fecha de hoy se completa sola y puedes cambiarla.", color = Muted)
            if (nature == "AJUSTE_CUADRE") {
                Text("Este ajuste mueve el saldo, pero no cambia ingresos ni gastos. Deja el motivo en la nota.", color = Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = direction == "GASTO", enabled = !isSaving, onClick = { direction = "GASTO" }, label = { Text("Gasté") })
                FilterChip(selected = direction == "INGRESO", enabled = !isSaving, onClick = { direction = "INGRESO"; currency = "Bs" }, label = { Text(if (nature == "AJUSTE_CUADRE") "Ajuste suma" else "Otra plata que entra") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = currency == "Bs", enabled = !isSaving, onClick = { currency = "Bs" }, label = { Text("Bs") })
                if (direction == "GASTO") FilterChip(selected = currency == "USDT", enabled = !isSaving, onClick = { currency = "USDT" }, label = { Text("USDT") })
            }
            Text(
                when {
                    direction == "INGRESO" -> "Entra a Mercantil · Bs"
                    currency == "Bs" -> "Sale de Mercantil · Bs"
                    else -> "Sale de USDT · Mercantil no cambia"
                },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface), shape = RoundedCornerShape(16.dp), border = CardOutline) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Monto", color = Muted, style = MaterialTheme.typography.labelLarge)
                    Text(
                        amount.ifBlank { "—" },
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Ink,
                        maxLines = 2,
                    )
                    NumericKeypad(amount = amount, enabled = !isSaving, onAmountChange = { amount = it })
                }
            }
            OutlinedTextField(category, { category = it }, enabled = !isSaving, label = { Text("Categoría") }, supportingText = { Text("Escribe la categoría que usas") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (direction == "INGRESO" && nature == "NORMAL") {
                OutlinedTextField(thirdParty, { thirdParty = it }, enabled = !isSaving, label = { Text("De quién y por qué") }, modifier = Modifier.fillMaxWidth())
                if (recentProvenances.isNotEmpty()) {
                    Text("Usados antes", color = Muted, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recentProvenances.forEach { suggestion ->
                            FilterChip(
                                selected = thirdParty == suggestion,
                                enabled = !isSaving,
                                onClick = { thirdParty = suggestion },
                                label = {
                                    Text(suggestion.take(20).let { if (suggestion.length > 20) "$it…" else it }, maxLines = 1)
                                },
                            )
                        }
                    }
                }
            }
            DateField(dateTime, { dateTime = it }, enabled = !isSaving)
            if (showNote) {
                OutlinedTextField(note, { note = it }, enabled = !isSaving, label = { Text(if (nature == "AJUSTE_CUADRE") "Motivo del ajuste" else "Nota") }, modifier = Modifier.fillMaxWidth())
            } else {
                TextButton(onClick = { showNote = true }, enabled = !isSaving) { Text("Añadir nota opcional") }
            }
            Text("Las ediciones conservan el antes, el después y la hora en el rastro.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        FormActions(
            primaryLabel = when {
                isSaving -> "Guardando…"
                initial != null -> "Guardar cambios"
                nature == "AJUSTE_CUADRE" -> "Guardar ajuste"
                direction == "INGRESO" -> "Guardar entrada"
                else -> "Guardar gasto"
            },
            isLoading = isSaving,
            isEnabled = !isSaving,
            onSave = { onSave(initial?.id, direction, nature, currency, amount, dateTime, category, note, thirdParty) },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun NumericKeypad(amount: String, enabled: Boolean = true, onAmountChange: (String) -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(",", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    FilledTonalButton(
                        enabled = enabled,
                        onClick = {
                            when (key) {
                                "⌫" -> onAmountChange(amount.dropLast(1))
                                "," -> if (amount.isNotBlank() && !amount.contains(',') && !amount.contains('.')) onAmountChange("$amount,")
                                else -> {
                                    val decimals = amount.substringAfter(',', amount.substringAfter('.', ""))
                                    if (!amount.contains(',') && !amount.contains('.') || decimals.length < 2) onAmountChange(amount + key)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftSurface, contentColor = Ink),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (key == "⌫") "Borrar" else key, fontSize = if (key == "⌫") 14.sp else 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleForm(
    initial: VentaP2pEntity?,
    isSaving: Boolean,
    onSave: (Long?, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var net by remember(initial?.id) { mutableStateOf(initial?.bsNetos.orEmpty()) }
    var rate by remember(initial?.id) { mutableStateOf(initial?.tasaP2p.orEmpty()) }
    var usdt by remember(initial?.id) { mutableStateOf(initial?.usdtCantidad.orEmpty()) }
    var provenance by remember(initial?.id) { mutableStateOf(initial?.proveniencia.orEmpty()) }
    var dateTime by remember(initial?.id) { mutableStateOf(initial?.fechaHora?.let(::localDateTimeText) ?: nowCaracasText()) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (initial == null) "Venta P2P a mano" else "Editar venta P2P", style = MaterialTheme.typography.headlineSmall)
            Text("Anota lo recibido neto, los USDT vendidos y la tasa de esa venta.", color = Muted)
            OutlinedTextField(net, { net = it }, enabled = !isSaving, label = { Text("Bolívares netos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(rate, { rate = it }, enabled = !isSaving, label = { Text("Tasa P2P · Bs/USDT") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(usdt, { usdt = it }, enabled = !isSaving, label = { Text("USDT vendidos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            DateField(dateTime, { dateTime = it }, enabled = !isSaving)
            OutlinedTextField(provenance, { provenance = it }, enabled = !isSaving, label = { Text("Nota opcional") }, modifier = Modifier.fillMaxWidth())
        }
        FormActions(
            primaryLabel = when {
                isSaving -> "Guardando…"
                initial == null -> "Guardar venta"
                else -> "Guardar cambios"
            },
            isLoading = isSaving,
            isEnabled = !isSaving,
            onSave = { onSave(initial?.id, net, rate, usdt, dateTime, provenance) },
            onCancel = onCancel,
        )
    }
}

@Composable
private fun FormActions(
    primaryLabel: String,
    isLoading: Boolean,
    isEnabled: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(color = SurfaceColor, contentColor = Ink, border = CardOutline) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                label = primaryLabel,
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
                loading = isLoading,
                onClick = onSave,
            )
            OutlinedButton(onClick = onCancel, enabled = isEnabled, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
private fun DateField(value: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Fecha y hora · Caracas") },
        supportingText = { Text("Formato: AAAA-MM-DD HH:MM · editable hacia atrás") },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun SectionCard(title: String, detail: String, content: @Composable () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(20.dp), border = CardOutline) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = Muted)
            content()
        }
    }
}

@Composable
private fun MetricValue(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick == null) modifier else modifier
        .clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = "$title. $value. $detail" }
    Column(clickableModifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Muted, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 2)
        Text(detail, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun spendingUsdDetail(total: com.mofoxx.kaby.domain.DatedTotal): String =
    if (total.unvaluedRows == 0) {
        "Equivalente USD · ${formatUsd(total.usdEquivalent)}"
    } else {
        "USD parcial · ${formatUsd(total.usdEquivalent)} · ${total.unvaluedRows} sin tasa"
    }

private fun directIncomeBs(movements: List<MovimientoEntity>): BigDecimal =
    movements.asSequence()
        .filter { it.tipo == "INGRESO" && it.naturaleza == "NORMAL" && it.moneda == "Bs" }
        .map { BigDecimal(it.monto) }
        .fold(BigDecimal.ZERO, BigDecimal::add)

@Composable
private fun SetupLine(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = Muted, fontSize = 20.sp)
        Text(label)
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(18.dp), border = CardOutline) {
        Text(text, Modifier.fillMaxWidth().padding(16.dp), color = Muted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Text(label, maxLines = 1)
            }
        } else {
            Text(label, maxLines = 2)
        }
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Surface(
        color = SoftSurface,
        contentColor = Ink,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Muted.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message, color = if (message.startsWith("E-")) Danger else Ink)
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val safeTime = OffsetDateTime.now(ZoneId.of("America/Caracas")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT))
                    val diagnostic = diagnosticCopyText(safeTime, message)
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Diagnóstico Kaby", diagnostic))) }
                }) { Text("Copiar") }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: Page) {
    val color = LocalContentColor.current
    Canvas(Modifier.width(24.dp).height(24.dp)) {
        val scale = size.minDimension / 24f
        val stroke = 2f * scale
        fun point(x: Float, y: Float) = Offset(x * scale, y * scale)
        fun path(vararg coordinates: Float) = Path().apply {
            moveTo(coordinates[0] * scale, coordinates[1] * scale)
            for (index in 2 until coordinates.size step 2) lineTo(coordinates[index] * scale, coordinates[index + 1] * scale)
        }
        when (destination) {
            Page.HOME -> {
                drawPath(path(3f, 11f, 12f, 3f, 21f, 11f), color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path(6f, 10f, 6f, 21f, 18f, 21f, 18f, 10f), color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color, point(10f, 21f), point(10f, 15f), stroke)
                drawLine(color, point(14f, 21f), point(14f, 15f), stroke)
            }
            Page.MOVEMENTS -> {
                listOf(6f, 12f, 18f).forEach { y ->
                    drawCircle(color, 1.3f * scale, point(5f, y))
                    drawLine(color, point(9f, y), point(20f, y), stroke, cap = StrokeCap.Round)
                }
            }
            Page.SALES -> {
                drawLine(color, point(4f, 8f), point(20f, 8f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(16f, 4f), point(20f, 8f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(16f, 12f), point(20f, 8f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(20f, 16f), point(4f, 16f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(8f, 12f), point(4f, 16f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(8f, 20f), point(4f, 16f), stroke, cap = StrokeCap.Round)
            }
            Page.RATES -> {
                drawLine(color, point(4f, 4f), point(4f, 20f), stroke, cap = StrokeCap.Round)
                drawLine(color, point(4f, 20f), point(21f, 20f), stroke, cap = StrokeCap.Round)
                drawPath(path(6f, 16f, 10f, 12f, 14f, 14f, 19f, 7f), color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Page.SETTINGS -> {
                drawCircle(color, 8f * scale, point(12f, 12f), style = Stroke(stroke))
                drawCircle(color, 2.4f * scale, point(12f, 12f))
                listOf(0f to 2f, 0f to -2f, 2f to 0f, -2f to 0f).forEach { (dx, dy) ->
                    val start = point(12f + dx * 5.7f, 12f + dy * 5.7f)
                    val end = point(12f + dx * 9.2f, 12f + dy * 9.2f)
                    drawLine(color, start, end, stroke, cap = StrokeCap.Round)
                }
            }
            else -> Unit
        }
    }
}

// KabyDao returns movements newest first; suggestions come only from the owner's own income history.
internal fun recentThirdPartySuggestions(movements: List<MovimientoEntity>): List<String> =
    movements.asSequence()
        .filter { it.tipo == "INGRESO" && it.naturaleza == "NORMAL" }
        .mapNotNull { it.provenienciaTercero?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
        .take(3)
        .toList()

internal fun diagnosticCopyText(safeTime: String, message: String): String =
    "Kaby 1.0.0 · M1 · $safeTime Caracas · $message"

internal fun auditDescription(entry: BitacoraEntity): String {
    val value = runCatching {
        val json = JsonParser.parseString(entry.despues).asJsonObject
        val amount = json.get("monto")?.asString ?: json.get("bsNetos")?.asString.orEmpty()
        val currency = json.get("moneda")?.asString ?: "Bs"
        val category = json.get("categoria")?.asString ?: "venta P2P"
        val kind = json.get("tipo")?.asString ?: ""
        val time = json.get("timestampUltimaEdicion")?.asString ?: json.get("fechaHora")?.asString ?: entry.timestamp
        val before = entry.antes?.let { raw ->
            val beforeJson = JsonParser.parseString(raw).asJsonObject
            val beforeAmount = beforeJson.get("monto")?.asString ?: beforeJson.get("bsNetos")?.asString.orEmpty()
            val beforeCategory = beforeJson.get("categoria")?.asString ?: "venta P2P"
            val beforeKind = beforeJson.get("tipo")?.asString.orEmpty()
            val beforeLabel = listOfNotNull(
                beforeKind.takeIf(String::isNotBlank)?.let(::humanizeMovementKind),
                beforeCategory,
            ).joinToString(" · ")
            "antes: ${formatAmount(beforeAmount, beforeJson.get("moneda")?.asString ?: "Bs")} · $beforeLabel"
        }
        val action = if (entry.accion == "EDITAR") "editado" else "creado"
        val label = listOfNotNull(kind.takeIf(String::isNotBlank)?.let(::humanizeMovementKind), category).joinToString(" · ")
        listOfNotNull(before, "después: ${formatAmount(amount, currency)} · $label", "$action el ${formatDate(time)}").joinToString("\n")
    }.getOrNull()
    return value ?: "${entry.accion.lowercase().replaceFirstChar(Char::uppercase)} el ${formatDate(entry.timestamp)}"
}

private fun humanizeMovementKind(value: String): String = when (value) {
    "GASTO" -> "Gasto"
    "INGRESO" -> "Ingreso"
    else -> value.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
}

internal fun humanizeSaleState(value: String): String = when (value) {
    "VIVA" -> "Activa"
    else -> value.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
}

private fun nowCaracasText(): String = OffsetDateTime.now(ZoneId.of("America/Caracas")).format(KabyViewModel.DATE_TIME_FORMAT)

private fun MovimientoEntity.toMovementForCalculation() = MovementForCalculation(
    tipo,
    naturaleza,
    moneda,
    monto,
    fechaHora,
    tasaBcvAlMomento,
)

internal fun localCaracasDate(value: String): LocalDate = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("America/Caracas")).toLocalDate()
}.getOrDefault(LocalDate.MIN)

internal fun hasBeenEdited(createdAt: String, updatedAt: String): Boolean {
    val created = CuadreCalculator.instantOrNull(createdAt) ?: return false
    val updated = CuadreCalculator.instantOrNull(updatedAt) ?: return false
    return updated.isAfter(created)
}

internal fun formatCaracasDay(value: LocalDate): String {
    if (value == LocalDate.MIN) return "Fecha no reconocida"
    val locale = Locale.forLanguageTag("es-VE")
    val day = value.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale))
    return "${day.replaceFirstChar { it.titlecase(locale) }} (Caracas)"
}

private fun localDateTimeText(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("America/Caracas")).format(KabyViewModel.DATE_TIME_FORMAT)
}.getOrDefault(nowCaracasText())

private fun formatDate(value: String): String = runCatching {
    val date = OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("America/Caracas"))
    date.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.forLanguageTag("es-VE"))) + " (Caracas)"
}.getOrDefault(value)

private fun formatBs(value: BigDecimal): String = "Bs ${decimalFormat().format(value)}"
internal fun formatUsd(value: BigDecimal): String = "USD ${decimalFormat().format(value)}"
internal fun formatUsdt(value: BigDecimal): String = "${decimalFormat().format(value)} USDT"
internal data class MovementDisplayValuation(val usd: BigDecimal, val bolivares: BigDecimal, val rate: BigDecimal)

internal fun movementDisplayValuation(amountText: String, currency: String, rateText: String?): MovementDisplayValuation? {
    val amount = runCatching { BigDecimal(amountText) }.getOrNull() ?: return null
    val rate = rateText?.let { runCatching { BigDecimal(it) }.getOrNull() } ?: return null
    if (rate <= BigDecimal.ZERO) return null
    val amountBs = when (currency) {
        "Bs" -> amount
        "USDT" -> amount.multiply(rate)
        else -> return null
    }
    val usd = BcvValuation.usdEquivalent(amountBs, rate) ?: return null
    return MovementDisplayValuation(usd, amountBs, rate)
}

private fun formatRate(value: String): String = runCatching {
    DecimalFormat("#,##0.0000", DecimalFormatSymbols(Locale.forLanguageTag("es-VE"))).format(BigDecimal(value))
}.getOrDefault(value)
private fun formatAmount(value: String, currency: String): String = runCatching {
    val amount = BigDecimal(value)
    if (currency == "USDT") formatUsdt(amount) else formatBs(amount)
}.getOrDefault("$value $currency")
private fun decimalFormat() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("es-VE")))
