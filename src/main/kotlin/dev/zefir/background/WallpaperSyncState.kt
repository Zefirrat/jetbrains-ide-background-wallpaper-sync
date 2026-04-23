package dev.zefir.background

data class WallpaperSyncState(
    var enabled: Boolean = true,
    var opacityPercent: Int = 15,
    var fill: String = "scale",
    var anchor: String = "center",
    var refreshIntervalMillis: Int = 30_000,
    var refreshIntervalSeconds: Int? = null,
    var lastAppliedPath: String? = null,
    var lastAppliedSignature: String? = null,
)
