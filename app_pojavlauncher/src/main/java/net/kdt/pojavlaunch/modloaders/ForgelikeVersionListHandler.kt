package net.kdt.pojavlaunch.modloaders

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.util.ArrayList

class ForgelikeVersionListHandler : DefaultHandler() {
    private var mForgeVersions: MutableList<String>? = null
    private var mCurrentVersion: StringBuilder? = null

    @Throws(SAXException::class)
    override fun startDocument() {
        mForgeVersions = ArrayList()
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        mCurrentVersion?.append(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if (qName == "version") mCurrentVersion = StringBuilder()
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        if (qName == "version") {
            mForgeVersions?.add(mCurrentVersion.toString())
            mCurrentVersion = null
        }
    }

    val versions: List<String>
        get() = mForgeVersions ?: emptyList()
}
