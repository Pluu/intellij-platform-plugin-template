///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:streaming/src/com/android/tools/idea/streaming/core/StreamingToolWindowManager.kt
///////////////////////////////////////////////////////////////////////////

package com.pluu.plugin.toolWindow.device

import ai.grazie.annotation.TestOnly
import com.android.annotations.concurrency.AnyThread
import com.android.annotations.concurrency.UiThread
import com.android.sdklib.deviceprovisioner.DeviceHandle
import com.android.tools.idea.concurrency.createCoroutineScope
import com.android.tools.idea.deviceprovisioner.DeviceProvisionerService
import com.android.tools.idea.streaming.core.DeviceId
import com.android.tools.idea.streaming.device.DeviceClient
import com.android.tools.idea.streaming.emulator.EmulatorController
import com.android.tools.idea.streaming.emulator.EmulatorController.ConnectionState
import com.android.tools.idea.streaming.emulator.EmulatorController.ConnectionStateListener
import com.android.tools.idea.streaming.emulator.RunningEmulatorCatalog
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.ActivateToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.MovedOrResized
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.ShowToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.IncorrectOperationException
import com.intellij.util.concurrency.AppExecutorUtil.createBoundedApplicationPoolExecutor
import com.intellij.util.containers.ContainerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.awt.EventQueue
import java.util.function.Supplier

private const val EMULATOR_DISCOVERY_INTERVAL_MILLIS = 1000L

private val CONTENT_DEVICE_ID_KEY = Key.create<DeviceId>("DeviceId")

@Suppress("IncorrectServiceRetrieving")
@UiThread
internal class StreamingToolWindowManager @AnyThread constructor(
    private val toolWindow: ToolWindowEx,
) : RunningEmulatorCatalog.Listener, DeviceClientRegistry.Listener, DumbAware, Disposable {

    private val project
        @AnyThread get() = toolWindow.project
    private val deviceProvisioner
        @AnyThread get() = project.service<DeviceProvisionerService>().deviceProvisioner
    private val deviceClientRegistry = service<DeviceClientRegistry>()

    private var initialized = false
    private var contentShown = false
    private var initialContentUpdate = false

    private val emulators = hashSetOf<EmulatorController>()

    private var onlineDevices = mapOf<String, ConnectedDevice>()

    /** Clients and handles of mirrorable devices keyed by serial numbers. */
    private var deviceClients = mutableMapOf<String, DeviceClientWithHandle>()

    // Copy-on-write to allow changes while iterating.
    private val contentManagers = ContainerUtil.createLockFreeCopyOnWriteList<ContentManager>()

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
        RunningEmulatorCatalog.getInstance().addListener(this, EMULATOR_DISCOVERY_INTERVAL_MILLIS * 10)
        deviceClientRegistry.addListener(this)
        PhysicalDeviceWatcher(this)

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
    }

    override fun dispose() {
        deviceClientRegistry.removeListener(this)
        onToolWindowHidden()
        RunningEmulatorCatalog.getInstance().removeListener(this)
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
        emulators.addAll(emulatorCatalog.emulators.filter { it.emulatorId.isEmbedded }) // Ignore standalone emulators.

        deviceClientRegistry.forEachClient { deviceClient ->
            val serialNumber = deviceClient.deviceSerialNumber
            if (serialNumber !in deviceClients) {
                val handle = onlineDevices[serialNumber]?.handle
                if (handle != null) {
                    adoptDeviceClient(serialNumber, handle) { deviceClient }
                }
            }
        }

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
    }

    private fun findContentBySerialNumberOfPhysicalDevice(serialNumber: String): Content? =
        findContent { it.deviceId?.serialNumber == serialNumber }

    private fun findContent(predicate: (Content) -> Boolean): Content? {
        for (contentManager in contentManagers) {
            for (i in 0 until contentManager.contentCount) {
                val content = contentManager.getContent(i) ?: break
                if (predicate(content)) {
                    return content
                }
            }
        }
        return null
    }

    override fun emulatorAdded(emulator: EmulatorController) {
        if (emulator.emulatorId.isEmbedded && emulator.emulatorConfig.isValid) {
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
                emulators.remove(emulator)
                removeEmulatorPanel(emulator)
            }
        }
    }

    override fun deviceClientAdded(client: DeviceClient, requester: Any?) {
        if (requester != this) {
            val serialNumber = client.deviceSerialNumber
            if (findContentBySerialNumberOfPhysicalDevice(serialNumber) == null) {
                val handle = onlineDevices[serialNumber]?.handle ?: return
                adoptDeviceClient(serialNumber, handle) { client }
            }
        }
    }

    override fun deviceClientRemoved(client: DeviceClient, requester: Any?) {
        val serialNumber = client.deviceSerialNumber
        if (requester != this && deviceClients[serialNumber]?.client == client) {
            deviceClients.remove(serialNumber)
        }
    }

    private fun addPanel(panel: DeviceManagerExplorer) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, null, false).apply {
            isCloseable = false
        }
        val contentManager = toolWindow.contentManager
        try {
            contentManager.removeAllContents(true)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
        } catch (_: IncorrectOperationException) {
            // Content manager has been disposed already.
            Disposer.dispose(content)
        }
    }

    private fun addEmulatorPanel(emulator: EmulatorController) {
        emulator.addConnectionStateListener(connectionStateListener)
    }

    private fun removeEmulatorPanel(emulator: EmulatorController): Boolean {
        emulator.removeConnectionStateListener(connectionStateListener)
        return true
    }

    private fun adoptDeviceClient(
        serialNumber: String, deviceHandle: DeviceHandle, clientSupplier: Supplier<DeviceClient>
    ): DeviceClientWithHandle {
        var clientWithHandle = deviceClients[serialNumber]
        if (clientWithHandle == null) {
            clientWithHandle = DeviceClientWithHandle(clientSupplier.get(), deviceHandle)
            deviceClients[serialNumber] = clientWithHandle
        }
        return clientWithHandle
    }

    private inner class PhysicalDeviceWatcher(disposableParent: Disposable) : Disposable {
        private val coroutineScope: CoroutineScope

        init {
            Disposer.register(disposableParent, this)
            val executor = createBoundedApplicationPoolExecutor("EmulatorToolWindowManager.PhysicalDeviceWatcher", 1)
            coroutineScope = createCoroutineScope(executor.asCoroutineDispatcher())
        }

        override fun dispose() {
            deviceClients.clear()
        }
    }

    private data class DeviceClientWithHandle(val client: DeviceClient, val handle: DeviceHandle)
}

private class ConnectedDevice(val handle: DeviceHandle)

@Service(Service.Level.APP)
internal class DeviceClientRegistry : Disposable {

    private val clientsBySerialNumber = LinkedHashMap<String, DeviceClient>()
        /** The returned map may only be accessed on the UI thread. */
        @UiThread
        get
    private val listeners = ContainerUtil.createLockFreeCopyOnWriteList<Listener>()

    /**
     * Terminates mirroring of the device and deletes the client. All listeners are notified by
     * calling [Listener.deviceClientRemoved].
     */
    @UiThread
    fun removeDeviceClient(serialNumber: String, requester: Any?) {
        clientsBySerialNumber.remove(serialNumber)?.also { client ->
            for (listener in listeners) {
                try {
                    listener.deviceClientRemoved(client, requester)
                } catch (_: Exception) {
                    // logger.error(e)
                }
            }
            Disposer.dispose(client)
        }
    }

    /** Iterates over existing device clients. The passed in consumer should not create or delete clients. */
    @UiThread
    fun forEachClient(consumer: (DeviceClient) -> Unit) {
        for (client in clientsBySerialNumber.values) {
            consumer(client)
        }
    }

    @UiThread
    fun isEmpty(): Boolean =
        clientsBySerialNumber.isEmpty()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    override fun dispose() {
    }

    /** Removes all device clients from the registry. */
    @TestOnly
    @UiThread
    fun clear() {
        for (serialNumber in clientsBySerialNumber.keys.toList()) {
            removeDeviceClient(serialNumber, null)
        }
    }

    interface Listener {

        @UiThread
        fun deviceClientAdded(client: DeviceClient, requester: Any?)

        @UiThread
        fun deviceClientRemoved(client: DeviceClient, requester: Any?)
    }
}

private var Content.deviceId: DeviceId?
    get() = CONTENT_DEVICE_ID_KEY.get(this)
    set(deviceId) = CONTENT_DEVICE_ID_KEY.set(this, deviceId)