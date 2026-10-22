package com.mofoxx.kaby.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mofoxx.kaby.KabyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DailyBackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as KabyApplication
            val written = PrivateBackup.write(applicationContext, app.repository)
            if (!written) {
                app.repository.logLocalBackupFailure()
                return Result.retry()
            }
            Result.success()
        } catch (_: Exception) {
            runCatching { (applicationContext as KabyApplication).repository.logLocalBackupFailure() }
            Result.retry()
        }
    }
}

object PrivateBackup {
    suspend fun write(context: Context, repository: com.mofoxx.kaby.data.CuadreRepository): Boolean {
        return BackupWriteCoordinator.write(
                snapshot = { repository.localBackupJson() },
                isValid = { com.mofoxx.kaby.data.BackupSnapshotCodec.decode(it) != null },
                persist = { json ->
                    withContext(Dispatchers.IO) {
                        val directory = File(context.filesDir, "local-backup").apply { mkdirs() }
                        val temporary = File(directory, "kaby-latest.json.tmp")
                        val destination = File(directory, "kaby-latest.json")
                        FileOutputStream(temporary).use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                            stream.fd.sync()
                        }
                        try {
                            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                },
            )
    }
}
