package dev.zefir.background

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import java.awt.Window
import java.nio.file.Path

object BackgroundImageApplier {
    private const val EDITOR_PROP = "idea.background.editor"

    fun apply(path: Path, opacityPercent: Int, fill: String, anchor: String) {
        val value = buildString {
            append(path.toString())
            append(',')
            append(opacityPercent.coerceIn(0, 100))
            append(',')
            append(fill)
            append(',')
            append(anchor)
        }

        PropertiesComponent.getInstance().setValue(EDITOR_PROP, value)
        repaintAllWindows()
    }

    fun clear() {
        PropertiesComponent.getInstance().unsetValue(EDITOR_PROP)
        repaintAllWindows()
    }

    private fun repaintAllWindows() {
        ApplicationManager.getApplication().invokeLater {
            if (invokeInternalRepaint()) {
                return@invokeLater
            }

            Window.getWindows().forEach { window ->
                window.invalidate()
                window.repaint()
            }
        }
    }

    private fun invokeInternalRepaint(): Boolean {
        return runCatching {
            val utilClass = Class.forName("com.intellij.openapi.wm.impl.IdeBackgroundUtil")
            val repaintMethod = utilClass.getMethod("repaintAllWindows")
            repaintMethod.invoke(null)
        }.isSuccess
    }
}

