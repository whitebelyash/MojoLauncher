package git.artdeell.installer_agent

import java.awt.Component

fun interface ComponentFilter {
    fun checkComponent(component: Component): Boolean
}
