package com.progresstractor

import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.UIManager

/** Registers [TractorProgressBarUI] (via [TractorProgressBarUIFactory]) as the Swing UI delegate for every `JProgressBar`. */
object TractorUiInstaller {

    private const val PROGRESS_BAR_UI_KEY = "ProgressBarUI"

    private val uiClass = TractorProgressBarUIFactory::class.java

    /** The theme's own `ProgressBarUI` value, restored by [uninstall]. */
    private var previousUi: Any? = null

    /** @param refreshWindows re-create the UI of already visible progress bars (needed when loaded without restart). */
    fun install(refreshWindows: Boolean = false) {
        val current = UIManager.get(PROGRESS_BAR_UI_KEY)
        if (current != uiClass.name) previousUi = current
        UIManager.put(PROGRESS_BAR_UI_KEY, uiClass.name)
        // Map the class name to the class itself so Swing doesn't need the plugin class loader.
        UIManager.getDefaults()[uiClass.name] = uiClass
        if (refreshWindows) refreshAllWindows()
    }

    /** Restores the theme's progress bar UI so the plugin class loader can be released on dynamic unload. */
    fun uninstall() {
        UIManager.put(PROGRESS_BAR_UI_KEY, previousUi)
        UIManager.getDefaults().remove(uiClass.name)
        previousUi = null
        refreshAllWindows()
    }

    private fun refreshAllWindows() {
        Window.getWindows().forEach(SwingUtilities::updateComponentTreeUI)
    }
}
