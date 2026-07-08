package git.artdeell.installer_agent

import java.awt.Component
import javax.swing.JButton
import javax.swing.JRadioButton
import javax.swing.JTextField

class MainWindowFilter : ComponentFilter {
    override fun checkComponent(component: Component): Boolean {
        return component is JRadioButton || component is JTextField || component is JButton
    }
}
