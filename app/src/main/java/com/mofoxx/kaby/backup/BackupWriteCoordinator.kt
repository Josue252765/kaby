package com.mofoxx.kaby.backup

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * WorkManager can run the daily, app-open, and background backup requests at the same time.
 * Keep snapshot generation and replacement of the shared local backup in one critical section.
 */
internal object BackupWriteCoordinator {
    private val mutex = Mutex()

    suspend fun write(
        snapshot: suspend () -> String,
        isValid: (String) -> Boolean,
        persist: suspend (String) -> Unit,
    ): Boolean = mutex.withLock {
        val json = snapshot()
        if (!isValid(json)) return@withLock false
        persist(json)
        true
    }
}
