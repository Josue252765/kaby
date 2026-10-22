package com.mofoxx.kaby.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KabyDao {
    @Query("SELECT * FROM lecturas_binance ORDER BY fecha_hora_orden DESC")
    fun observeBinanceReads(): Flow<List<LecturaBinanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBinanceRead(read: LecturaBinanceEntity)

    @Query("SELECT * FROM lecturas_binance WHERE order_number = :number")
    suspend fun binanceRead(number: String): LecturaBinanceEntity?

    @Query("SELECT * FROM ventas_p2p_mano WHERE order_number = :number")
    suspend fun saleByOrder(number: String): VentaP2pEntity?

    @Query("SELECT * FROM estado_cuentas WHERE id = 1")
    fun observeAccount(): Flow<EstadoCuentaEntity?>

    @Query("SELECT * FROM estado_cuentas WHERE id = 1")
    suspend fun accountNow(): EstadoCuentaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: EstadoCuentaEntity)

    @Query("SELECT * FROM movimientos ORDER BY fecha_hora DESC, id DESC")
    fun observeMovements(): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun movement(id: Long): MovimientoEntity?

    @Insert
    suspend fun insertMovement(movement: MovimientoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMovement(movement: MovimientoEntity)

    @Query("SELECT * FROM ventas_p2p_mano ORDER BY fecha_hora DESC, id DESC")
    fun observeSales(): Flow<List<VentaP2pEntity>>

    @Query("SELECT * FROM ventas_p2p_mano WHERE id = :id")
    suspend fun sale(id: Long): VentaP2pEntity?

    @Insert
    suspend fun insertSale(sale: VentaP2pEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSale(sale: VentaP2pEntity)

    @Query("SELECT * FROM tasas_bcv ORDER BY hora_fuente DESC, id DESC")
    fun observeRates(): Flow<List<TasaBcvEntity>>

    @Query("SELECT * FROM tasas_bcv")
    suspend fun ratesNow(): List<TasaBcvEntity>

    @Insert
    suspend fun insertRate(rate: TasaBcvEntity): Long

    @Insert
    suspend fun insertAudit(entry: BitacoraEntity)

    @Query("SELECT * FROM bitacora WHERE tabla = :table AND fila_id = :rowId ORDER BY timestamp DESC, id DESC")
    suspend fun auditFor(table: String, rowId: Long): List<BitacoraEntity>

    @Insert
    suspend fun insertError(entry: BitacoraErrorEntity)

    @Query("""
        DELETE FROM bitacora_errores
        WHERE id NOT IN (
            SELECT id FROM bitacora_errores ORDER BY id DESC LIMIT :limit
        )
    """)
    suspend fun pruneErrorEntries(limit: Int)

    @Query("SELECT * FROM acta_casillas WHERE hito = 'M1' ORDER BY id ASC")
    fun observeM1ChecklistEntries(): Flow<List<ActaCasillaEntity>>

    @Insert
    suspend fun insertChecklistEntry(entry: ActaCasillaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun settingsNow(): SettingsEntity?

    @Query("SELECT * FROM movimientos ORDER BY id")
    suspend fun allMovements(): List<MovimientoEntity>

    @Query("SELECT * FROM ventas_p2p_mano ORDER BY id")
    suspend fun allSales(): List<VentaP2pEntity>

    @Query("SELECT * FROM tasas_bcv ORDER BY id")
    suspend fun allRates(): List<TasaBcvEntity>

    @Query("SELECT * FROM deudas ORDER BY id")
    suspend fun allDebts(): List<DeudaEntity>

    @Query("SELECT * FROM cuotas ORDER BY id")
    suspend fun allInstallments(): List<CuotaEntity>

    @Query("SELECT * FROM bolsos ORDER BY id")
    suspend fun allGroups(): List<BolsoEntity>

    @Query("SELECT * FROM bolso_participantes ORDER BY id")
    suspend fun allGroupParticipants(): List<BolsoParticipanteEntity>

    @Query("SELECT * FROM bolso_participaciones ORDER BY id")
    suspend fun allGroupContributions(): List<BolsoParticipacionEntity>

    @Query("SELECT * FROM estado_cuentas ORDER BY id")
    suspend fun allAccounts(): List<EstadoCuentaEntity>

    @Query("SELECT * FROM bitacora ORDER BY id")
    suspend fun allAuditEntries(): List<BitacoraEntity>

    @Query("SELECT * FROM bitacora_errores ORDER BY id")
    suspend fun allErrors(): List<BitacoraErrorEntity>

    @Query("SELECT * FROM settings ORDER BY id")
    suspend fun allSettings(): List<SettingsEntity>

    @Query("SELECT * FROM acta_casillas ORDER BY id")
    suspend fun allChecklistEntries(): List<ActaCasillaEntity>

    @Query("SELECT * FROM lecturas_binance ORDER BY order_number")
    suspend fun allBinanceReads(): List<LecturaBinanceEntity>
}
