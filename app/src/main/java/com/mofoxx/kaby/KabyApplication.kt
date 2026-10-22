package com.mofoxx.kaby

import android.app.Application
import androidx.room.Room
import com.mofoxx.kaby.data.CuadreRepository
import com.mofoxx.kaby.data.KabyDatabase
import com.mofoxx.kaby.tasas.DolarApiTasaClient
import com.mofoxx.kaby.tasas.RateRefreshScheduler
import com.mofoxx.kaby.backup.BackupScheduler

class KabyApplication : Application() {
    val database: KabyDatabase by lazy {
        Room.databaseBuilder(this, KabyDatabase::class.java, "kaby.db")
            .addCallback(KabyDatabase.invariantsCallback)
            .build()
    }

    val repository: CuadreRepository by lazy {
        CuadreRepository(database, DolarApiTasaClient())
    }

    override fun onCreate() {
        super.onCreate()
        BackupScheduler.schedule(this)
        RateRefreshScheduler.schedule(this)
    }
}
