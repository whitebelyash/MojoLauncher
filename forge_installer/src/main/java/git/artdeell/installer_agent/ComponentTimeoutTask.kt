package git.artdeell.installer_agent

import java.util.TimerTask

class ComponentTimeoutTask : TimerTask() {
    override fun run() {
        println("Initialization timed out!")
        System.exit(17)
    }
}
