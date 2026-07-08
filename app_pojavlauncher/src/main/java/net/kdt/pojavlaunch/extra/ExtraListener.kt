package net.kdt.pojavlaunch.extra

fun interface ExtraListener<T> {
    fun onValueSet(key: String, value: T): Boolean
}
