package git.artdeell.installer_agent

import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.EventQueue
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import java.lang.instrument.Instrumentation
import java.util.ArrayList
import java.util.Timer
import javax.swing.AbstractButton
import javax.swing.JDialog
import javax.swing.JOptionPane

class Agent(private val optiFineInstallation: Boolean) : AWTEventListener {
    private var forgeWindowHandled = false
    private val componentTimer = Timer()

    override fun eventDispatched(event: AWTEvent) {
        val windowEvent = event as WindowEvent
        val window = windowEvent.window
        if (windowEvent.id != WindowEvent.WINDOW_OPENED) return
        if (forgeWindowHandled && window is JDialog) {
            handleDialog(window)
            return
        }
        if (!forgeWindowHandled) {
            forgeWindowHandled = handleMainWindow(window)
            checkComponentTimer()
        }
    }

    fun checkComponentTimer() {
        if (forgeWindowHandled) {
            componentTimer.cancel()
            componentTimer.purge()
            return
        }
        componentTimer.schedule(ComponentTimeoutTask(), 30000)
    }

    fun handleMainWindow(window: Window): Boolean {
        val components = ArrayList<Component>()
        insertAllComponents(components, window, MainWindowFilter())
        var okButton: AbstractButton? = null
        for (component in components) {
            if (component is AbstractButton) {
                val abstractButton = if (optiFineInstallation) {
                    handleOptiFineButton(component)
                } else {
                    handleForgeButton(component)
                }
                if (abstractButton != null) okButton = abstractButton
            }
        }
        return if (okButton == null) {
            println("Failed to set all the UI components, wil try again in the next window")
            false
        } else {
            EventQueue.invokeLater(okButton::doClick)
            true
        }
    }

    fun handleForgeButton(abstractButton: AbstractButton): AbstractButton? {
        when (abstractButton.text) {
            "Proceed", "OK" -> return abstractButton
            "Install client" -> abstractButton.doClick()
        }
        return null
    }

    fun handleOptiFineButton(abstractButton: AbstractButton): AbstractButton? {
        if ("Install" == abstractButton.text) {
            return abstractButton
        }
        return null
    }

    fun handleDialog(window: Window) {
        val components = ArrayList<Component>()
        insertAllComponents(components, window, DialogFilter())
        if (components.size == 1) {
            val optionPane = components[0] as JOptionPane
            if (optionPane.messageType == JOptionPane.INFORMATION_MESSAGE) {
                println("The install was successful!")
                System.exit(0)
            }
        }
    }

    fun insertAllComponents(components: MutableList<Component>, parent: Container, filter: ComponentFilter) {
        val componentCount = parent.componentCount
        for (i in 0 until componentCount) {
            val component = parent.getComponent(i)
            if (filter.checkComponent(component)) components.add(component)
            if (component is Container) {
                insertAllComponents(components, component, filter)
            }
        }
    }

    companion object {
        @JvmStatic
        fun premain(args: String, inst: Instrumentation) {
            var optifine = false
            if (args != null) {
                optifine = args.contains("OF")
            }
            val agent = Agent(optifine)
            Toolkit.getDefaultToolkit()
                .addAWTEventListener(agent, AWTEvent.WINDOW_EVENT_MASK)
        }
    }
}
