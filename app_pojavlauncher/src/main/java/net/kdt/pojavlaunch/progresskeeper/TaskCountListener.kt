package net.kdt.pojavlaunch.progresskeeper

interface TaskCountListener {
    fun onUpdateTaskCount(taskCount: Int): Boolean
}
