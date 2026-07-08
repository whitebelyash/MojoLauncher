package git.artdeell.installer_agent

import java.awt.Component
import javax.swing.JOptionPane
import javax.swing.JProgressBar

class DialogFilter : ComponentFilter {
    override fun checkComponent(component: Component): Boolean {
        return component is JOptionPane || component is JProgressBar
    }
}
