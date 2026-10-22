package com.mofoxx.kaby.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val naturaleza: String,
    val moneda: String,
    val monto: String,
    @ColumnInfo(name = "tasa_bcv_al_momento") val tasaBcvAlMomento: String? = null,
    @ColumnInfo(name = "hora_tasa") val horaTasa: String? = null,
    @ColumnInfo(name = "estado_valuacion") val estadoValuacion: String = "VALUADA",
    @ColumnInfo(name = "fecha_hora") val fechaHora: String,
    val categoria: String,
    val motivo: String? = null,
    val origen: String = "MANO",
    @ColumnInfo(name = "proveniencia_tercero") val provenienciaTercero: String? = null,
    @ColumnInfo(name = "ref_deuda") val refDeuda: Long? = null,
    @ColumnInfo(name = "ref_cuota") val refCuota: Long? = null,
    @ColumnInfo(name = "tasa_pago") val tasaPago: String? = null,
    @ColumnInfo(name = "tasa_pago_tipo") val tasaPagoTipo: String? = null,
    @ColumnInfo(name = "hora_tasa_pago") val horaTasaPago: String? = null,
    @ColumnInfo(name = "ref_bolso") val refBolso: Long? = null,
    @ColumnInfo(name = "ref_ronda") val refRonda: Int? = null,
    @ColumnInfo(name = "timestamp_creacion") val timestampCreacion: String,
    @ColumnInfo(name = "timestamp_ultima_edicion") val timestampUltimaEdicion: String,
)

@Entity(tableName = "deudas")
data class DeudaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "tipo_ente") val tipoEnte: String,
    @ColumnInfo(name = "moneda_de_cuota") val monedaDeCuota: String,
    val periodicidad: String,
    @ColumnInfo(name = "monto_total") val montoTotal: String,
    @ColumnInfo(name = "cuota_periodo") val cuotaPeriodo: String,
    @ColumnInfo(name = "fecha_inicio") val fechaInicio: String,
    @ColumnInfo(name = "saldo_pendiente") val saldoPendiente: String,
    val estado: String,
    val notas: String? = null,
)

@Entity(
    tableName = "cuotas",
    foreignKeys = [ForeignKey(entity = DeudaEntity::class, parentColumns = ["id"], childColumns = ["deuda_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("deuda_id"), Index(value = ["deuda_id", "secuencia"], unique = true)],
)
data class CuotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "deuda_id") val deudaId: Long,
    val secuencia: Int,
    val vencimiento: String,
    val monto: String,
    val estado: String,
)

@Entity(tableName = "bolsos")
data class BolsoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    @ColumnInfo(name = "modo_cuota") val modoCuota: String,
    @ColumnInfo(name = "cuota_por_ronda") val cuotaPorRonda: String? = null,
    @ColumnInfo(name = "ronda_actual") val rondaActual: Int,
)

@Entity(tableName = "bolso_participantes")
data class BolsoParticipanteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val notas: String? = null,
)

@Entity(
    tableName = "bolso_participaciones",
    foreignKeys = [
        ForeignKey(entity = BolsoEntity::class, parentColumns = ["id"], childColumns = ["bolso_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = BolsoParticipanteEntity::class, parentColumns = ["id"], childColumns = ["participante_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("bolso_id"), Index("participante_id"), Index(value = ["bolso_id", "participante_id", "ronda"], unique = true)],
)
data class BolsoParticipacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bolso_id") val bolsoId: Long,
    @ColumnInfo(name = "participante_id") val participanteId: Long,
    val ronda: Int,
    @ColumnInfo(name = "cuota_comprometida") val cuotaComprometida: String,
    val estado: String,
)

@Entity(tableName = "ventas_p2p_mano", indices = [Index(value = ["order_number"], unique = true)])
data class VentaP2pEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fecha_hora") val fechaHora: String,
    @ColumnInfo(name = "bs_netos") val bsNetos: String,
    @ColumnInfo(name = "tasa_p2p") val tasaP2p: String,
    @ColumnInfo(name = "usdt_cantidad") val usdtCantidad: String,
    @ColumnInfo(name = "tasa_bcv_al_momento") val tasaBcvAlMomento: String? = null,
    @ColumnInfo(name = "hora_tasa_bcv") val horaTasaBcv: String? = null,
    val prima: String? = null,
    @ColumnInfo(name = "estado_valuacion") val estadoValuacion: String = "VALUADA",
    val proveniencia: String? = null,
    @ColumnInfo(name = "order_number") val orderNumber: String? = null,
    val fuente: String = "MANO",
    val estado: String = "VIVA",
    @ColumnInfo(name = "timestamp_creacion") val timestampCreacion: String,
    @ColumnInfo(name = "timestamp_ultima_edicion") val timestampUltimaEdicion: String,
)

@Entity(tableName = "tasas_bcv", indices = [Index("hora_fuente")])
data class TasaBcvEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val valor: String,
    @ColumnInfo(name = "hora_fuente") val horaFuente: String,
    val fuente: String,
    @ColumnInfo(name = "confirmada_por") val confirmadaPor: String? = null,
)

@Entity(tableName = "lecturas_binance")
data class LecturaBinanceEntity(
    @PrimaryKey @ColumnInfo(name = "order_number") val orderNumber: String,
    @ColumnInfo(name = "trade_type") val tradeType: String,
    @ColumnInfo(name = "fecha_hora_orden") val fechaHoraOrden: String,
    @ColumnInfo(name = "unit_price") val unitPrice: String,
    @ColumnInfo(name = "total_price") val totalPrice: String,
    val amount: String,
    @ColumnInfo(name = "leido_en") val leidoEn: String,
    @ColumnInfo(name = "ventana_consulta") val ventanaConsulta: String,
)

@Entity(tableName = "estado_cuentas")
data class EstadoCuentaEntity(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "ancla_monto") val anclaMonto: String? = null,
    @ColumnInfo(name = "ancla_fecha_hora") val anclaFechaHora: String? = null,
    @ColumnInfo(name = "ultimo_cuadre_at") val ultimoCuadreAt: String? = null,
    @ColumnInfo(name = "estado_cuadre") val estadoCuadre: String = "SIN_ANCLA",
    @ColumnInfo(name = "cifra_descuadre") val cifraDescuadre: String? = null,
    @ColumnInfo(name = "saldo_usdt") val saldoUsdt: String? = null,
    @ColumnInfo(name = "leido_en") val leidoEn: String? = null,
)

@Entity(tableName = "bitacora", indices = [Index(value = ["tabla", "fila_id"])])
data class BitacoraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tabla: String,
    @ColumnInfo(name = "fila_id") val filaId: Long,
    val accion: String,
    val antes: String? = null,
    val despues: String,
    val timestamp: String,
)

@Entity(tableName = "bitacora_errores")
data class BitacoraErrorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capa: String,
    val mensaje: String,
    @ColumnInfo(name = "detalle_tecnico") val detalleTecnico: String? = null,
    val timestamp: String,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "moneda_referencia") val monedaReferencia: String = "Bs",
    @ColumnInfo(name = "ultima_exportacion_externa") val ultimaExportacionExterna: String? = null,
    @ColumnInfo(name = "guia_alarmas_oem_vista") val guiaAlarmasOemVista: Boolean = false,
    @ColumnInfo(name = "version_formato_respaldo") val versionFormatoRespaldo: Int = 1,
)

@Entity(tableName = "acta_casillas", indices = [Index(value = ["hito", "casilla_id"])])
data class ActaCasillaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hito: String,
    @ColumnInfo(name = "casilla_id") val casillaId: String,
    val veredicto: String,
    @ColumnInfo(name = "causa_bloqueo") val causaBloqueo: String? = null,
    val evidencia: String? = null,
    val timestamp: String,
)
