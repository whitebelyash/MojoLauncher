package net.kdt.pojavlaunch.modloaders

import net.kdt.pojavlaunch.utils.DownloadUtils
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode
import java.util.ArrayList

class OptiFineScraper : DownloadUtils.ParseCallback<OptiFineUtils.OptiFineVersions> {
    private val mOptiFineVersions = OptiFineUtils.OptiFineVersions()
    private var mListInProgress: MutableList<OptiFineUtils.OptiFineVersion>? = null
    private var mGameVersion: String? = null

    init {
        mOptiFineVersions.gameVersions = ArrayList()
        mOptiFineVersions.optifineVersions = ArrayList()
    }

    @Throws(DownloadUtils.ParseException::class)
    override fun process(input: String): OptiFineUtils.OptiFineVersions {
        val htmlCleaner = HtmlCleaner()
        val tagNode = htmlCleaner.clean(input)
        traverseTagNode(tagNode)
        insertVersionContent(null)
        if (mOptiFineVersions.optifineVersions.size < 1 || mOptiFineVersions.gameVersions.size < 1) {
            throw DownloadUtils.ParseException(null)
        }
        return mOptiFineVersions
    }

    fun traverseTagNode(tagNode: TagNode) {
        if (isDownloadLine(tagNode) && mGameVersion != null) {
            traverseDownloadLine(tagNode)
        } else if (isGameVersionTag(tagNode)) {
            insertVersionContent(tagNode)
        } else {
            for (tagNodes in tagNode.childTags) {
                traverseTagNode(tagNodes)
            }
        }
    }

    private fun isDownloadLine(tagNode: TagNode) =
        tagNode.name == "tr" && tagNode.hasAttribute("class") && tagNode.getAttributeByName("class").startsWith("downloadLine")

    private fun isGameVersionTag(tagNode: TagNode) =
        tagNode.name == "h2" && tagNode.text.toString().startsWith("Minecraft ")

    private fun traverseDownloadLine(tagNode: TagNode) {
        val optiFineVersion = OptiFineUtils.OptiFineVersion()
        optiFineVersion.gameVersion = mGameVersion
        for (subNode in tagNode.childTags) {
            if (subNode.name != "td") continue
            when (subNode.getAttributeByName("class")) {
                "colFile" -> optiFineVersion.versionName = subNode.text.toString()
                "colMirror" -> optiFineVersion.downloadUrl = getLinkHref(subNode)
            }
        }
        mListInProgress!!.add(optiFineVersion)
    }

    private fun getLinkHref(parent: TagNode): String? {
        for (subNode in parent.childTags) {
            if (subNode.name == "a" && subNode.hasAttribute("href")) {
                return subNode.getAttributeByName("href").replace("http://", "https://")
            }
        }
        return null
    }

    private fun insertVersionContent(tagNode: TagNode?) {
        if (mListInProgress != null && mGameVersion != null) {
            mOptiFineVersions.gameVersions.add(mGameVersion)
            mOptiFineVersions.optifineVersions.add(mListInProgress!!)
        }
        if (tagNode != null) {
            mGameVersion = tagNode.text.toString()
            mListInProgress = ArrayList()
        }
    }
}
