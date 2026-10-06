package com.progresstractor

import javax.swing.UIManager

/** Registers [TractorProgressBarUI] (via [TractorProgressBarUIFactory]) as the Swing UI delegate for every `JProgressBar`. */
object TractorUiInstaller {

    private const val PROGRESS_BAR_UI_KEY = "ProgressBarUI"

    fun install() {
        val uiClass = TractorProgressBarUIFactory::class.java
        UIManager.put(PROGRESS_BAR_UI_KEY, uiClass.name)
        // Map the class name to the class itself so Swing doesn't need the plugin class loader.
        UIManager.getDefaults()[uiClass.name] = uiClass
    }
}
