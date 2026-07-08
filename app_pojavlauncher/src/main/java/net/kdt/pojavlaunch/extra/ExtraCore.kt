package net.kdt.pojavlaunch.extra

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("UNCHECKED_CAST")
object ExtraCore {
    private val mValueMap = ConcurrentHashMap<String, Any>()
    private val mListenerMap = ConcurrentHashMap<String, ConcurrentLinkedQueue<WeakReference<ExtraListener<Any>>>>()

    @JvmStatic
    fun setValue(key: String, value: Any?) {
        if (value == null || key.isEmpty()) return
        mValueMap[key] = value
        val extraListenerList = mListenerMap[key] ?: return
        val iterator = extraListenerList.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            if (listener.get() == null) {
                iterator.remove()
                continue
            }
            if (listener.get()!!.onValueSet(key, value)) {
                removeExtraListenerFromValue(key, listener.get()!!)
            }
        }
    }

    @JvmStatic
    fun getValue(key: String): Any? = mValueMap[key]

    @JvmStatic
    fun getValue(key: String, defaultValue: Any): Any {
        val value = mValueMap[key]
        return value ?: defaultValue
    }

    @JvmStatic
    fun removeValue(key: String) = mValueMap.remove(key)

    @JvmStatic
    fun consumeValue(key: String): Any? {
        val value = mValueMap[key]
        mValueMap.remove(key)
        return value
    }

    @JvmStatic
    fun removeAllValues() = mValueMap.clear()

    @JvmStatic
    fun addExtraListener(key: String, listener: ExtraListener<*>) {
        val listenerList = mListenerMap.getOrPut(key) { ConcurrentLinkedQueue() }
        listenerList.add(WeakReference(listener as ExtraListener<Any>))
    }

    @JvmStatic
    fun removeExtraListenerFromValue(key: String, listener: ExtraListener<*>) {
        val listenerList = mListenerMap.getOrPut(key) { ConcurrentLinkedQueue() }
        val iterator = listenerList.iterator()
        while (iterator.hasNext()) {
            val actualListener = iterator.next().get()
            if (actualListener == null || actualListener === listener) {
                iterator.remove()
            }
        }
    }

    @JvmStatic
    fun removeAllExtraListenersFromValue(key: String) {
        val listenerList = mListenerMap.getOrPut(key) { ConcurrentLinkedQueue() }
        listenerList.clear()
    }

    @JvmStatic
    fun removeAllExtraListeners() = mListenerMap.clear()
}
