package eu.darken.apl.watchlist.core.alerts

import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import eu.darken.apl.common.coroutine.AppScope
import eu.darken.apl.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.apl.common.debug.logging.log
import eu.darken.apl.common.debug.logging.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WatchlistMonitorManager @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val workManager: WorkManager,
    private val watchlistMonitor: WatchlistMonitor,
) {

    private var isInit = false
    fun setup() {
        log(TAG) { "setup()" }
        require(!isInit)
        isInit = true

        runBlocking { setupPeriodicWorker() }

//        appScope.launch {
//            try {
//                alertMonitor.checkAlerts()
//            } catch (e: Exception) {
//                log(TAG, ERROR) { "Failed to refresh: ${e.asLog()}" }
//            }
//        }
    }

    private suspend fun setupPeriodicWorker() {
        val workRequest = PeriodicWorkRequestBuilder<WatchlistWorker>(
            Duration.ofHours(1),
            Duration.ofMinutes(10)
        ).apply {
            setInputData(Data.Builder().build())
        }.build()

        log(TAG, VERBOSE) { "Worker request: $workRequest" }

        val operation = workManager.enqueueUniquePeriodicWork(
            "alerts.monitor.worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )

        operation.await()
    }

    companion object {
        val TAG = logTag("Watchlist", "Monitor", "Manager")
    }
}