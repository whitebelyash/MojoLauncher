package git.artdeell.dnbootstrap.glfw

interface ClipboardProvider {
    fun getClipboardString(): String?
    fun setClipboardString(str: String)
}
