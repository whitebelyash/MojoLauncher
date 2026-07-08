package net.kdt.pojavlaunch.customcontrols.keyboard

interface CharacterSenderStrategy {
    fun sendBackspace()
    fun sendEnter()
    fun sendChars(chars: CharSequence)
}
