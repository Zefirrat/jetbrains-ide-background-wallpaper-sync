package dev.zefir.background.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import dev.zefir.background.WallpaperSyncService

class SyncWallpaperNowAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val service = ApplicationManager.getApplication().getService(WallpaperSyncService::class.java)
        service.refreshNow(force = true, showNotification = true)
    }
}
