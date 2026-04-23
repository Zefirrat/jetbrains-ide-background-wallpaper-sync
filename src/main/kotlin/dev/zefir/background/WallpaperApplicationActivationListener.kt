package dev.zefir.background

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.IdeFrame

class WallpaperApplicationActivationListener : ApplicationActivationListener {
    override fun applicationActivated(ideFrame: IdeFrame) {
        val service = ApplicationManager.getApplication().getService(WallpaperSyncService::class.java)
        service.start()
    }
}
