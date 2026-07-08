package net.kdt.pojavlaunch.customcontrols

object LayoutSanitizer {
    private fun isValidFormula(formula: String): Boolean {
        return !formula.contains("Infinity") && !formula.contains("NaN")
    }

    private fun isSaneData(controlData: ControlData): Boolean {
        if (controlData.width == 0f || controlData.height == 0f) return false
        return isValidFormula(controlData.dynamicX) && isValidFormula(controlData.dynamicY)
    }

    private fun checkEntry(entry: Any): Boolean {
        return when (entry) {
            is ControlData -> isSaneData(entry)
            is ControlDrawerData -> {
                if (!isSaneData(entry.properties)) return false
                sanitizeList(entry.buttonProperties)
                true
            }
            else -> throw RuntimeException("Unknown data entry ${entry.javaClass.name}")
        }
    }

    private fun sanitizeList(controlDataList: List<*>): Boolean {
        var madeChanges = false
        val iterator = controlDataList.iterator()
        while (iterator.hasNext()) {
            if (!checkEntry(iterator.next()!!)) {
                madeChanges = true
                iterator.remove()
            }
        }
        return madeChanges
    }

    fun sanitizeLayout(controls: CustomControls): Boolean {
        var madeChanges = sanitizeList(controls.mControlDataList)
        if (sanitizeList(controls.mDrawerDataList)) madeChanges = true
        if (sanitizeList(controls.mJoystickDataList)) madeChanges = true
        return madeChanges
    }
}
