package dev.zefir.background

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

@State(name = "WallpaperSyncSettings", storages = [Storage("rider-background-plugin.xml")])
@Service(Service.Level.APP)
class WallpaperSyncService : PersistentStateComponent<WallpaperSyncState> {
    companion object {
        private const val MIN_REFRESH_INTERVAL_MILLIS = 30_000
    }

    private val logger = thisLogger()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "wallpaper-sync").apply { isDaemon = true }
    }

    @Volatile
    private var task: ScheduledFuture<*>? = null
    @Volatile
    private var jnaFailureLogged = false

    private var state = WallpaperSyncState()

    @Synchronized
    fun start() {
        if (!state.enabled) {
            return
        }

        if (task != null) {
            return
        }

        refreshNow(force = false, showNotification = false)
        task = scheduler.scheduleWithFixedDelay(
            { refreshNow(force = false, showNotification = false) },
            state.refreshIntervalMillis.toLong(),
            state.refreshIntervalMillis.toLong(),
            TimeUnit.MILLISECONDS,
        )
    }

    @Synchronized
    fun updateSettings(newState: WallpaperSyncState) {
        val normalized = newState.copy(
            opacityPercent = newState.opacityPercent.coerceIn(0, 100),
            refreshIntervalMillis = newState.refreshIntervalMillis.coerceAtLeast(MIN_REFRESH_INTERVAL_MILLIS),
            refreshIntervalSeconds = null,
        )
        state = normalized
        restart()
        if (normalized.enabled) {
            refreshNow(force = true, showNotification = false)
        } else {
            clear(showNotification = false)
        }
    }

    @Synchronized
    private fun restart() {
        task?.cancel(false)
        task = null
        start()
    }

    fun refreshNow(force: Boolean, showNotification: Boolean) {
        if (!WindowsWallpaperReader.isSupported()) {
            if (showNotification) {
                notify("Windows wallpaper sync is only supported on Windows.", NotificationType.WARNING)
            }
            return
        }

        try {
            val wallpaperPath = WindowsWallpaperReader.readCurrentWallpaperPath()
            if (wallpaperPath.isNullOrBlank()) {
                if (showNotification) {
                    notify("Windows did not return a wallpaper path.", NotificationType.WARNING)
                }
                return
            }

            val normalizedPath = Path.of(wallpaperPath).toAbsolutePath().normalize()
            if (!Files.isRegularFile(normalizedPath)) {
                if (showNotification) {
                    notify("Wallpaper file does not exist: $normalizedPath", NotificationType.WARNING)
                }
                return
            }

            val signature = buildFileSignature(normalizedPath)

            BackgroundImageApplier.apply(
                path = normalizedPath,
                opacityPercent = state.opacityPercent,
                fill = state.fill,
                anchor = state.anchor,
            )
            state.lastAppliedPath = normalizedPath.toString()
            state.lastAppliedSignature = signature
            jnaFailureLogged = false

            if (showNotification) {
                notify("Applied wallpaper: $normalizedPath", NotificationType.INFORMATION)
            }
        } catch (t: Throwable) {
            if (WindowsWallpaperReader.isInitializationFailure(t)) {
                if (!jnaFailureLogged) {
                    logger.warn("Failed to initialize Windows wallpaper access", t)
                    jnaFailureLogged = true
                }
            } else {
                logger.warn("Failed to refresh Windows wallpaper background", t)
            }
            if (showNotification) {
                notify("Failed to sync wallpaper: ${t.message ?: t.javaClass.simpleName}", NotificationType.ERROR)
            }
        }
    }

    fun clear(showNotification: Boolean) {
        BackgroundImageApplier.clear()
        state.lastAppliedPath = null
        state.lastAppliedSignature = null
        if (showNotification) {
            notify("Cleared IDE background applied by the plugin.", NotificationType.INFORMATION)
        }
    }

    override fun getState(): WallpaperSyncState = state

    override fun loadState(state: WallpaperSyncState) {
        val migratedMillis = when {
            state.refreshIntervalMillis > 0 -> state.refreshIntervalMillis
            state.refreshIntervalSeconds != null -> state.refreshIntervalSeconds!!.coerceAtLeast(1) * 1000
            else -> 30_000
        }

        this.state = state.copy(
            opacityPercent = state.opacityPercent.coerceIn(0, 100),
            refreshIntervalMillis = migratedMillis.coerceAtLeast(MIN_REFRESH_INTERVAL_MILLIS),
            refreshIntervalSeconds = null,
        )
    }

    private fun notify(content: String, type: NotificationType) {
        SwingUtilities.invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Zefirrat. Wallpaper Background Sync.")
                .createNotification(content, type)
                .notify(null)
        }
    }

    private fun buildFileSignature(path: Path): String {
        val attrs = Files.readAttributes(path, "basic:lastModifiedTime,size")
        return "${path}::${attrs["lastModifiedTime"]}::${attrs["size"]}"
    }
}
