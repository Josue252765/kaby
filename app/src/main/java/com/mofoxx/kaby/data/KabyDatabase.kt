package com.mofoxx.kaby.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MovimientoEntity::class,
        DeudaEntity::class,
        CuotaEntity::class,
        BolsoEntity::class,
        BolsoParticipanteEntity::class,
        BolsoParticipacionEntity::class,
        VentaP2pEntity::class,
        TasaBcvEntity::class,
        LecturaBinanceEntity::class,
        EstadoCuentaEntity::class,
        BitacoraEntity::class,
        BitacoraErrorEntity::class,
        SettingsEntity::class,
        ActaCasillaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KabyDatabase : RoomDatabase() {
    abstract fun kabyDao(): KabyDao

    companion object {
        val invariantsCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                installInvariants(db)
            }

            private fun installInvariants(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TRIGGER movimientos_validar_insert BEFORE INSERT ON movimientos BEGIN
                    SELECT CASE WHEN CAST(NEW.monto AS NUMERIC) < 0 THEN RAISE(ABORT, 'E_MONTO_NEGATIVO') END;
                    SELECT CASE WHEN NEW.tipo NOT IN ('GASTO','INGRESO') THEN RAISE(ABORT, 'E_TIPO_INVALIDO') END;
                    SELECT CASE WHEN NEW.naturaleza NOT IN ('NORMAL','AJUSTE_CUADRE','TURNO_BOLSO') THEN RAISE(ABORT, 'E_NATURALEZA_INVALIDA') END;
                    SELECT CASE WHEN NEW.moneda NOT IN ('Bs','USDT') THEN RAISE(ABORT, 'E_MONEDA_INVALIDA') END;
                    SELECT CASE WHEN NEW.moneda='USDT' AND NEW.tasa_bcv_al_momento IS NULL AND NEW.estado_valuacion!='SIN VALUAR' THEN RAISE(ABORT, 'E_SIN_VALUAR') END;
                    SELECT CASE WHEN NEW.tipo='INGRESO' AND NEW.naturaleza='NORMAL' AND (NEW.proveniencia_tercero IS NULL OR LENGTH(TRIM(NEW.proveniencia_tercero))=0) THEN RAISE(ABORT, 'E_PROVENIENCIA_REQUERIDA') END;
                    END""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TRIGGER movimientos_validar_update BEFORE UPDATE ON movimientos BEGIN
                    SELECT CASE WHEN CAST(NEW.monto AS NUMERIC) < 0 THEN RAISE(ABORT, 'E_MONTO_NEGATIVO') END;
                    SELECT CASE WHEN NEW.tipo NOT IN ('GASTO','INGRESO') THEN RAISE(ABORT, 'E_TIPO_INVALIDO') END;
                    SELECT CASE WHEN NEW.naturaleza NOT IN ('NORMAL','AJUSTE_CUADRE','TURNO_BOLSO') THEN RAISE(ABORT, 'E_NATURALEZA_INVALIDA') END;
                    SELECT CASE WHEN NEW.moneda NOT IN ('Bs','USDT') THEN RAISE(ABORT, 'E_MONEDA_INVALIDA') END;
                    SELECT CASE WHEN NEW.moneda='USDT' AND NEW.tasa_bcv_al_momento IS NULL AND NEW.estado_valuacion!='SIN VALUAR' THEN RAISE(ABORT, 'E_SIN_VALUAR') END;
                    SELECT CASE WHEN NEW.tipo='INGRESO' AND NEW.naturaleza='NORMAL' AND (NEW.proveniencia_tercero IS NULL OR LENGTH(TRIM(NEW.proveniencia_tercero))=0) THEN RAISE(ABORT, 'E_PROVENIENCIA_REQUERIDA') END;
                    END""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TRIGGER ventas_validar_insert BEFORE INSERT ON ventas_p2p_mano BEGIN
                    SELECT CASE WHEN CAST(NEW.bs_netos AS NUMERIC)<=0 OR CAST(NEW.usdt_cantidad AS NUMERIC)<=0 OR CAST(NEW.tasa_p2p AS NUMERIC)<=0 THEN RAISE(ABORT, 'E_VENTA_INVALIDA') END;
                    SELECT CASE WHEN NEW.fuente NOT IN ('MANO','API') OR NEW.estado NOT IN ('VIVA','JUBILADA') THEN RAISE(ABORT, 'E_VENTA_ESTADO_INVALIDO') END;
                    SELECT CASE WHEN NEW.tasa_bcv_al_momento IS NULL AND NEW.estado_valuacion!='SIN VALUAR' THEN RAISE(ABORT, 'E_SIN_VALUAR') END;
                    END""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TRIGGER ventas_validar_update BEFORE UPDATE ON ventas_p2p_mano BEGIN
                    SELECT CASE WHEN CAST(NEW.bs_netos AS NUMERIC)<=0 OR CAST(NEW.usdt_cantidad AS NUMERIC)<=0 OR CAST(NEW.tasa_p2p AS NUMERIC)<=0 THEN RAISE(ABORT, 'E_VENTA_INVALIDA') END;
                    SELECT CASE WHEN NEW.fuente NOT IN ('MANO','API') OR NEW.estado NOT IN ('VIVA','JUBILADA') THEN RAISE(ABORT, 'E_VENTA_ESTADO_INVALIDO') END;
                    SELECT CASE WHEN NEW.tasa_bcv_al_momento IS NULL AND NEW.estado_valuacion!='SIN VALUAR' THEN RAISE(ABORT, 'E_SIN_VALUAR') END;
                    END""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TRIGGER tasas_validar_insert BEFORE INSERT ON tasas_bcv BEGIN
                    SELECT CASE WHEN CAST(NEW.valor AS NUMERIC)<=0 THEN RAISE(ABORT, 'E_TASA_INVALIDA') END;
                    SELECT CASE WHEN NEW.fuente NOT IN ('MANUAL','ESPEJO_DOLARAPI','ESPEJO_DOLARVZLA') THEN RAISE(ABORT, 'E_FUENTE_INVALIDA') END;
                    SELECT CASE WHEN NEW.confirmada_por IS NOT NULL AND NEW.confirmada_por!='HUMANO' THEN RAISE(ABORT, 'E_CONFIRMACION_INVALIDA') END;
                    END""".trimIndent(),
                )
                db.execSQL(
                    """CREATE TRIGGER tasas_validar_update BEFORE UPDATE ON tasas_bcv BEGIN
                    SELECT CASE WHEN CAST(NEW.valor AS NUMERIC)<=0 THEN RAISE(ABORT, 'E_TASA_INVALIDA') END;
                    SELECT CASE WHEN NEW.fuente NOT IN ('MANUAL','ESPEJO_DOLARAPI','ESPEJO_DOLARVZLA') THEN RAISE(ABORT, 'E_FUENTE_INVALIDA') END;
                    SELECT CASE WHEN NEW.confirmada_por IS NOT NULL AND NEW.confirmada_por!='HUMANO' THEN RAISE(ABORT, 'E_CONFIRMACION_INVALIDA') END;
                    END""".trimIndent(),
                )
            }
        }
    }
}
