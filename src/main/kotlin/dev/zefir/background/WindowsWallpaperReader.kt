package dev.zefir.background

import com.intellij.openapi.application.PathManager
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.nio.file.Files
import java.nio.file.Path

object WindowsWallpaperReader {
    private const val SPI_GETDESKWALLPAPER = 0x0073
    private const val MAX_PATH = 260
    @Volatile
    private var user32Instance: User32Ext? = null
    @Volatile
    private var initializationFailure: Throwable? = null

    private interface User32Ext : StdCallLibrary {
        fun SystemParametersInfoW(
            uiAction: Int,
            uiParam: Int,
            pvParam: CharArray,
            fWinIni: Int,
        ): Boolean
    }

    fun isSupported(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    fun readCurrentWallpaperPath(): String? {
        if (!isSupported()) {
            return null
        }

        val buffer = CharArray(MAX_PATH)
        val ok = loadUser32().SystemParametersInfoW(SPI_GETDESKWALLPAPER, buffer.size, buffer, 0)
        if (!ok) {
            error("SystemParametersInfoW(SPI_GETDESKWALLPAPER) failed")
        }

        val value = Native.toString(buffer)
        return value?.takeIf { it.isNotBlank() }
    }

    fun isInitializationFailure(t: Throwable): Boolean {
        return t === initializationFailure || t.cause === initializationFailure || t is WallpaperReaderUnavailableException
    }

    private fun configureBundledJna() {
        if (System.getProperty("jna.boot.library.path").isNullOrBlank()) {
            bundledJnaDirectory()?.let { System.setProperty("jna.boot.library.path", it.toString()) }
        }
    }

    private fun loadUser32(): User32Ext {
        user32Instance?.let { return it }
        initializationFailure?.let { throw WallpaperReaderUnavailableException(it) }

        return synchronized(this) {
            user32Instance?.let { return@synchronized it }
            initializationFailure?.let { throw WallpaperReaderUnavailableException(it) }

            try {
                configureBundledJna()
                Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS).also {
                    user32Instance = it
                }
            } catch (t: Throwable) {
                initializationFailure = t
                throw WallpaperReaderUnavailableException(t)
            }
        }
    }

    private fun bundledJnaDirectory(): Path? {
        val ideHome = Path.of(PathManager.getHomePath())
        val archDir = when {
            System.getProperty("os.arch").contains("aarch64", ignoreCase = true) ||
                System.getProperty("os.arch").contains("arm64", ignoreCase = true) -> "aarch64"
            else -> "amd64"
        }

        val candidate = ideHome.resolve("lib").resolve("jna").resolve(archDir)
        return candidate.takeIf(Files::isDirectory)
    }
}

class WallpaperReaderUnavailableException(cause: Throwable) : IllegalStateException("Windows wallpaper API is unavailable", cause)
