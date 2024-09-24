///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/core/StreamingToolWindowManager.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import com.android.annotations.concurrency.AnyThread
import com.android.annotations.concurrency.UiThread
import com.android.sdklib.deviceprovisioner.DeviceProvisioner
import com.android.tools.idea.avdmanager.AvdLaunchListener
import com.android.tools.idea.deviceprovisioner.DeviceProvisionerService
import com.android.tools.idea.streaming.emulator.EmulatorController
import com.android.tools.idea.streaming.emulator.EmulatorController.ConnectionState
import com.android.tools.idea.streaming.emulator.EmulatorController.ConnectionStateListener
import com.android.tools.idea.streaming.emulator.RunningEmulatorCatalog
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.ActivateToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.MovedOrResized
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.ShowToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.util.IncorrectOperationException
import java.awt.EventQueue

private const val EMULATOR_DISCOVERY_INTERVAL_MILLIS = 1000L

@Suppress("IncorrectServiceRetrieving")
@UiThread
internal class DeviceToolWindowManager(
    private val toolWindow: ToolWindow,
) : RunningEmulatorCatalog.Listener, DumbAware, Disposable {

    private val project
        @AnyThread get() = toolWindow.project

    private val deviceProvisioner: DeviceProvisioner = project.service<DeviceProvisionerService>().deviceProvisioner

    private var initialized = false
    private var contentShown = false
    private var initialContentUpdate = false

    private val emulators = hashSetOf<EmulatorController>()

    private val connectionStateListener = object : ConnectionStateListener {
        @AnyThread
        override fun connectionStateChanged(emulator: EmulatorController, connectionState: ConnectionState) {
            if (connectionState == ConnectionState.DISCONNECTED) {
                EventQueue.invokeLater { // This is safe because this code doesn't touch PSI or VFS.
                    if (removeEmulatorPanel(emulator)) {
                        emulators.remove(emulator)
                    }
                }
            }
        }
    }

    init {
        Disposer.register(toolWindow.disposable, this)

        // Lazily initialize content since we can only have one frame.
        val messageBusConnection = project.messageBus.connect(this)
        messageBusConnection.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {

            // TODO: Override the stateChanged method that takes a ToolWindow when it becomes a public API.
            override fun stateChanged(toolWindowManager: ToolWindowManager, changeType: ToolWindowManagerEventType) {
                val toolWindow = toolWindowManager.getToolWindow(RUNNING_DEVICES_TOOL_WINDOW_ID) ?: return

                when (changeType) {
                    ActivateToolWindow, ShowToolWindow, HideToolWindow, MovedOrResized -> {
                        toolWindowManager.invokeLater {
                            if (!toolWindow.isDisposed) {
                                if (toolWindow.isVisible) {
                                    initialContentUpdate = true
                                    try {
                                        onToolWindowShown()
                                    } finally {
                                        initialContentUpdate = false
                                    }
                                } else {
                                    onToolWindowHidden()
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        })

        messageBusConnection.subscribe(AvdLaunchListener.TOPIC,
            AvdLaunchListener { avd, commandLine, requestType, project ->
                if (project == toolWindow.project && isEmbeddedEmulator(commandLine)) {
                    RunningEmulatorCatalog.getInstance().updateNow()
                }
            })
    }

    private fun onToolWindowShown() {
        if (!initialized) {
            initialized = true
        }

        if (contentShown) {
            return
        }
        contentShown = true

        val emulatorCatalog = RunningEmulatorCatalog.getInstance()
        emulatorCatalog.updateNow()
        emulatorCatalog.addListener(this, EMULATOR_DISCOVERY_INTERVAL_MILLIS)
        assert(emulators.isEmpty())
        emulators.addAll(emulatorCatalog.emulators.filter { it.emulatorId.isEmbedded })

        addPanel(DeviceManagerExplorer(project, deviceProvisioner))
    }

    private fun onToolWindowHidden() {
        if (!contentShown) {
            return
        }
        contentShown = false

        RunningEmulatorCatalog.getInstance().addListener(this, Long.MAX_VALUE) // Don't need frequent updates.
        for (emulator in emulators) {
            emulator.removeConnectionStateListener(connectionStateListener)
        }
        emulators.clear()
        toolWindow.contentManager.removeAllContents(true)
    }

    override fun emulatorAdded(emulator: EmulatorController) {
        if (emulator.emulatorId.isEmbedded) {
            EventQueue.invokeLater { // This is safe because this code doesn't touch PSI or VFS.
                if (contentShown && emulators.add(emulator)) {
                    addEmulatorPanel(emulator)
                }
            }
        }
    }

    override fun emulatorRemoved(emulator: EmulatorController) {
        if (emulator.emulatorId.isEmbedded) {
            EventQueue.invokeLater { // This is safe because this code doesn't touch PSI or VFS.
                if (removeEmulatorPanel(emulator)) {
                    emulators.remove(emulator)
                }
            }
        }
    }

    override fun dispose() {
        onToolWindowHidden()
        RunningEmulatorCatalog.getInstance().removeListener(this)
    }

    private fun addPanel(panel: DeviceManagerExplorer) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, null, false).apply {
            isCloseable = false
        }
        val contentManager = toolWindow.contentManager
        try {
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
        } catch (e: IncorrectOperationException) {
            // Content manager has been disposed already.
            Disposer.dispose(content)
        }
    }

    private fun isEmbeddedEmulator(commandLine: GeneralCommandLine) =
        commandLine.parametersList.parameters.contains("-qt-hide-window")

    private fun addEmulatorPanel(emulator: EmulatorController) {
        emulator.addConnectionStateListener(connectionStateListener)
    }

    private fun removeEmulatorPanel(emulator: EmulatorController): Boolean {
        emulator.removeConnectionStateListener(connectionStateListener)
        return true
    }
}