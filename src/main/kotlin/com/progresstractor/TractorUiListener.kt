package com.progresstractor

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener

/**
 * Re-installs the tractor UI on startup and after every theme switch (a theme switch resets UI defaults),
 * and installs/uninstalls it when the plugin is loaded or unloaded without an IDE restart.
 */
class TractorUiListener : LafManagerListener, AppLifecycleListener, DynamicPluginListener {

    override fun lookAndFeelChanged(source: LafManager) = TractorUiInstaller.install()

    override fun appFrameCreated(commandLineArgs: MutableList<String>) = TractorUiInstaller.install()

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.isOwn()) TractorUiInstaller.install(refreshWindows = true)
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.isOwn()) TractorUiInstaller.uninstall()
    }

    private fun IdeaPluginDescriptor.isOwn() = pluginId.idString == PLUGIN_ID

    private companion object {
        const val PLUGIN_ID = "com.progresstractor"
    }
}
