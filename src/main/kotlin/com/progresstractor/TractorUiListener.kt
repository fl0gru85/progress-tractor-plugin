package com.progresstractor

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener

/** Re-installs the tractor UI on startup and after every theme switch (a theme switch resets UI defaults). */
class TractorUiListener : LafManagerListener, AppLifecycleListener {

    override fun lookAndFeelChanged(source: LafManager) = TractorUiInstaller.install()

    override fun appFrameCreated(commandLineArgs: MutableList<String>) = TractorUiInstaller.install()
}
