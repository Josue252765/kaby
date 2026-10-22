package com.mofoxx.kaby.backup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

class BackupWriteCoordinatorTest {
    @Test
    fun testRespaldo_escriturasConcurrentesSeSerializan() = runBlocking {
        val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        try {
            val firstSnapshotStarted = CompletableDeferred<Unit>()
            val releaseFirstSnapshot = CompletableDeferred<Unit>()
            val secondCallStarted = CompletableDeferred<Unit>()
            val secondSnapshotStarted = CompletableDeferred<Unit>()
            val persisted = Collections.synchronizedList(mutableListOf<String>())

            val first = async(dispatcher) {
                BackupWriteCoordinator.write(
                    snapshot = {
                        firstSnapshotStarted.complete(Unit)
                        releaseFirstSnapshot.await()
                        "daily-snapshot"
                    },
                    isValid = { true },
                    persist = { persisted += it },
                )
            }
            firstSnapshotStarted.await()

            val second = async(dispatcher) {
                secondCallStarted.complete(Unit)
                yield()
                BackupWriteCoordinator.write(
                    snapshot = {
                        secondSnapshotStarted.complete(Unit)
                        "background-snapshot"
                    },
                    isValid = { true },
                    persist = { persisted += it },
                )
            }
            secondCallStarted.await()

            val secondStartedBeforeRelease = withTimeoutOrNull(500) {
                secondSnapshotStarted.await()
                true
            } ?: false

            releaseFirstSnapshot.complete(Unit)
            assertTrue(first.await())
            assertTrue(second.await())
            assertFalse("El segundo snapshot no debe adelantarse al primero", secondStartedBeforeRelease)
            assertEquals(listOf("daily-snapshot", "background-snapshot"), persisted.toList())
        } finally {
            dispatcher.close()
        }
    }

    @Test
    fun testRespaldo_snapshotInvalidoNoSeEscribe() = runBlocking(Dispatchers.Default) {
        var persistCalled = false
        val written = BackupWriteCoordinator.write(
            snapshot = { "invalid" },
            isValid = { false },
            persist = { persistCalled = true },
        )
        assertFalse(written)
        assertFalse(persistCalled)
    }
}
