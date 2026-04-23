package dev.zefir.background.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import dev.zefir.background.WallpaperSyncService
import dev.zefir.background.WallpaperSyncState
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class WallpaperSettingsConfigurable : Configurable {
    private val service: WallpaperSyncService
        get() = ApplicationManager.getApplication().getService(WallpaperSyncService::class.java)

    private lateinit var enabledCheckBox: JCheckBox
    private lateinit var opacitySpinner: JSpinner
    private lateinit var refreshSpinner: JSpinner
    private lateinit var fillComboBox: JComboBox<String>
    private lateinit var anchorComboBox: JComboBox<String>
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Zefirrat. Wallpaper Background Sync."

    override fun createComponent(): JComponent {
        enabledCheckBox = JCheckBox("Enable wallpaper sync")
        opacitySpinner = JSpinner(SpinnerNumberModel(15, 0, 100, 1))
        refreshSpinner = JSpinner(SpinnerNumberModel(30.0, 30.0, 3600.0, 1.0))
        (refreshSpinner.editor as? JSpinner.NumberEditor)?.format?.minimumFractionDigits = 1
        (refreshSpinner.editor as? JSpinner.NumberEditor)?.format?.maximumFractionDigits = 1
        fillComboBox = JComboBox(arrayOf("scale", "tile"))
        anchorComboBox = JComboBox(arrayOf("center", "top_center", "top_left", "top_right"))

        panel = JPanel(GridBagLayout()).apply {
            val gc = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(6, 6, 6, 6)
                weightx = 1.0
                gridx = 0
                gridy = 0
            }

            add(enabledCheckBox, gc)

            gc.gridy++
            add(labeledRow("Opacity (%)", opacitySpinner), gc)

            gc.gridy++
            add(labeledRow("Refresh interval (sec)", refreshSpinner), gc)

            gc.gridy++
            add(labeledRow("Fill mode", fillComboBox), gc)

            gc.gridy++
            add(labeledRow("Anchor", anchorComboBox), gc)
        }

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val state = service.state
        return enabledCheckBox.isSelected != state.enabled ||
            opacitySpinner.value as Int != state.opacityPercent ||
            refreshIntervalMillisFromUi() != state.refreshIntervalMillis ||
            fillComboBox.selectedItem != state.fill ||
            anchorComboBox.selectedItem != state.anchor
    }

    override fun apply() {
        service.updateSettings(
            WallpaperSyncState(
                enabled = enabledCheckBox.isSelected,
                opacityPercent = opacitySpinner.value as Int,
                refreshIntervalMillis = refreshIntervalMillisFromUi(),
                refreshIntervalSeconds = null,
                fill = fillComboBox.selectedItem as String,
                anchor = anchorComboBox.selectedItem as String,
                lastAppliedPath = service.state.lastAppliedPath,
                lastAppliedSignature = service.state.lastAppliedSignature,
            ),
        )
    }

    override fun reset() {
        val state = service.state
        enabledCheckBox.isSelected = state.enabled
        opacitySpinner.value = state.opacityPercent
        refreshSpinner.value = state.refreshIntervalMillis / 1000.0
        fillComboBox.selectedItem = state.fill
        anchorComboBox.selectedItem = state.anchor
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun labeledRow(label: String, component: JComponent): JPanel {
        return JPanel(GridBagLayout()).apply {
            val left = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = Insets(0, 0, 0, 12)
            }
            val right = GridBagConstraints().apply {
                gridx = 1
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.WEST
            }
            add(JLabel(label), left)
            add(component, right)
        }
    }

    private fun refreshIntervalMillisFromUi(): Int {
        val seconds = (refreshSpinner.value as Number).toDouble()
        return (seconds * 1000).toInt().coerceAtLeast(30_000)
    }
}
