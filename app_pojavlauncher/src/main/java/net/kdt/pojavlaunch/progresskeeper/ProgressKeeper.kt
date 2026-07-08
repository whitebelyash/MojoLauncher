package net.kdt.pojavlaunch.progresskeeper

import java.util.*

object ProgressKeeper {
    private val sProgressListeners = HashMap<String, MutableList<ProgressListener>>()
    private val sProgressStates = HashMap<String, ProgressState>()
    private val sTaskCountListeners = ArrayList<TaskCountListener>()

    @Synchronized
    fun submitProgress(progressRecord: String, progress: Int, resid: Int, vararg va: Any?) {
        val progressState = sProgressStates[progressRecord]
        val shouldCallStarted = progressState == null
        val shouldCallEnded = resid == -1 && progress == -1
        if (shouldCallEnded) {
            sProgressStates.remove(progressRecord)
        } else if (shouldCallStarted) {
            sProgressStates[progressRecord] = ProgressState().also {
                it.progress = progress
                it.resid = resid
                it.varArg = va
            }
        }
        if (shouldCallEnded || shouldCallStarted) updateTaskCount(sProgressStates.size)

        val effectiveState = sProgressStates[progressRecord]
        if (effectiveState != null) {
            effectiveState.progress = progress
            effectiveState.resid = resid
            effectiveState.varArg = va
        }

        val progressListeners = sProgressListeners[progressRecord]
        if (progressListeners != null) {
            for (listener in progressListeners) {
                when {
                    shouldCallStarted -> listener.onProgressStarted()
                    shouldCallEnded -> listener.onProgressEnded()
                    else -> listener.onProgressUpdated(progress, resid, *va)
                }
            }
        }
    }

    private fun updateTaskCount(count: Int) {
        synchronized(sTaskCountListeners) {
            val iterator = sTaskCountListeners.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().onUpdateTaskCount(count)) iterator.remove()
            }
        }
    }

    @Synchronized
    fun hasProgressKey(key: String): Boolean = sProgressStates.containsKey(key)

    @Synchronized
    fun addListener(progressRecord: String, listener: ProgressListener) {
        val state = sProgressStates[progressRecord]
        if (state != null && (state.resid != -1 || state.progress != -1)) {
            listener.onProgressStarted()
            listener.onProgressUpdated(state.progress, state.resid, *state.varArg)
        } else {
            listener.onProgressEnded()
        }
        val listenerList = sProgressListeners.getOrPut(progressRecord) { ArrayList() }
        listenerList.add(listener)
    }

    @Synchronized
    fun removeListener(progressRecord: String, listener: ProgressListener) {
        sProgressListeners[progressRecord]?.remove(listener)
    }

    fun addTaskCountListener(listener: TaskCountListener) {
        addTaskCountListener(listener, true)
    }

    fun addTaskCountListener(listener: TaskCountListener, runUpdate: Boolean) {
        if (runUpdate) synchronized(this) {
            listener.onUpdateTaskCount(sProgressStates.size)
        }
        synchronized(sTaskCountListeners) {
            if (!sTaskCountListeners.contains(listener)) sTaskCountListeners.add(listener)
        }
    }

    fun removeTaskCountListener(listener: TaskCountListener) {
        synchronized(sTaskCountListeners) {
            sTaskCountListeners.remove(listener)
        }
    }

    fun waitUntilDone(runnable: Runnable) {
        if (getTaskCount() == 0) {
            runnable.run()
            return
        }
        val listener = TaskCountListener { taskCount ->
            if (taskCount == 0) {
                runnable.run()
                true
            } else {
                false
            }
        }
        addTaskCountListener(listener)
    }

    @Synchronized
    fun getTaskCount(): Int = sProgressStates.size

    fun hasOngoingTasks(): Boolean = getTaskCount() > 0
}
