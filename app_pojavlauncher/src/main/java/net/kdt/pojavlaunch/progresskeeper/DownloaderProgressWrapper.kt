package net.kdt.pojavlaunch.progresskeeper

import net.kdt.pojavlaunch.Tools.BYTE_TO_MB
import net.kdt.pojavlaunch.Tools
import kotlin.math.max

class DownloaderProgressWrapper(
    private val mProgressString: Int,
    private val mProgressRecord: String
) : Tools.DownloaderFeedback {

    @JvmField var extraString: String? = null

    override fun updateProgress(curr: Int, max: Int) {
        val va: Array<Any> = if (extraString != null) {
            arrayOf(extraString!!, curr / BYTE_TO_MB, max / BYTE_TO_MB)
        } else {
            arrayOf(curr / BYTE_TO_MB, max / BYTE_TO_MB)
        }
        ProgressKeeper.submitProgress(
            mProgressRecord,
            max(0f, curr.toFloat() / max * 100).toInt(),
            mProgressString,
            *va
        )
    }
}
