package git.artdeell.dnbootstrap.glfw

interface CursorImplementor : GrabListener {
    fun onCursorPosition()
    fun onCursorChanged()
}
