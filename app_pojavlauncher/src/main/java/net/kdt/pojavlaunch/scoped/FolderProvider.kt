package net.kdt.pojavlaunch.scoped

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.Nullable
import git.artdeell.mojo.BuildConfig
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class FolderProvider : DocumentsProvider() {
    private var BASE_DIR: File? = null
    private var mContentResolver: ContentResolver? = null
    private var mStorageProviderAuthortiy: String? = null

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val applicationName = context!!.getString(R.string.app_short_name)

        var summary = BuildConfig.VERSION_NAME
        if (BuildConfig.DEBUG) {
            summary = "(" + context!!.getString(R.string.generic_debug) + ") $summary"
        }

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR!!))
        row.add(Root.COLUMN_SUMMARY, summary)
        row.add(
            Root.COLUMN_FLAGS,
            Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD
        )
        row.add(Root.COLUMN_TITLE, applicationName)
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR!!.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (!Tools.checkFileValidness(this, null)) {
            result.setNotificationUri(mContentResolver, createUriForDocId(documentId))
            includeFile(result, documentId, null)
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        if (!Tools.checkFileValidness(this, null)) {
            val children = parent.listFiles()
                ?: throw FileNotFoundException("Unable to list files in " + parent.absolutePath)
            for (file in children) {
                includeFile(result, null, file)
            }
            result.setNotificationUri(mContentResolver, createUriForDocId(parentDocumentId))
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun onCreate(): Boolean {
        if (Tools.checkStorageRoot(context!!)) {
            Tools.initStorageConstants(context!!)
        } else {
            return false
        }
        BASE_DIR = File(Tools.DIR_GAME_HOME)
        mContentResolver = context!!.contentResolver
        mStorageProviderAuthortiy = context!!.getString(R.string.storageProviderAuthorities)
        return true
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        var newFile = File(parentDocumentId, displayName)
        var noConflictId = 2
        while (Tools.checkFileValidness(this, newFile)) {
            newFile = File(parentDocumentId, "$displayName (${noConflictId++})")
        }
        Tools.checkFileValidness(this, null)
        try {
            val succeeded = if (Document.MIME_TYPE_DIR == mimeType) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id " + newFile.path)
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with id " + newFile.path)
        }
        notifyChange(createUriForDocId(parentDocumentId))
        return newFile.path
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        val sourceFile = getFileForDocId(documentId)
        val sourceParent = sourceFile.parentFile
            ?: throw FileNotFoundException("Cannot rename root")
        val targetFile = File(getDocIdForFile(sourceParent) + "/$displayName")
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Couldn't rename the document with id$documentId")
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String
    ): String {
        val sourceFile = getFileForDocId(sourceParentDocumentId + sourceDocumentId)
        val targetFile = File(targetParentDocumentId + sourceDocumentId)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Failed to move the document with id " + sourceFile.path)
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun removeDocument(documentId: String, parentDocumentId: String) {
        deleteDocument("$parentDocumentId/$documentId")
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (file.isDirectory) {
            try {
                FileUtils.deleteDirectory(file)
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        } else {
            if (!file.delete()) {
                throw FileNotFoundException("Failed to delete document with id $documentId")
            }
        }
        notifyChange(createUriForFile(file.parentFile!!))
    }

    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        Log.i("FolderPRovider", "getDocumentType($documentId)")
        val file = getFileForDocId(documentId)
        return getMimeType(file)
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(rootId)
        val pending = LinkedList<File>()
        pending.add(parent)

        val MAX_SEARCH_RESULTS = 50
        while (pending.isNotEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            val isInsideHome = try {
                file.canonicalPath.startsWith(Tools.DIR_GAME_HOME)
            } catch (_: IOException) {
                true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    val listing = file.listFiles()
                    if (listing != null) Collections.addAll(pending, *listing)
                } else {
                    if (file.name.lowercase().contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }
        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    private fun getDocIdForFile(file: File): String = file.absolutePath

    @Throws(FileNotFoundException::class)
    private fun getFileForDocId(docId: String): File {
        val f = File(docId)
        if (!f.exists()) throw FileNotFoundException("${f.absolutePath} not found")
        return f
    }

    private fun getMimeType(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        if (lastDot >= 0) {
            val extension = name.substring(lastDot + 1).lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return mime
        }
        return "application/octet-stream"
    }

    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        var resolvedDocId = docId
        var resolvedFile = file
        if (resolvedDocId == null) {
            resolvedDocId = getDocIdForFile(resolvedFile!!)
        } else {
            resolvedFile = getFileForDocId(resolvedDocId)
        }

        var flags = 0
        if (resolvedFile!!.isDirectory) {
            if (resolvedFile.canWrite()) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (resolvedFile.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        val parent = resolvedFile.parentFile
        if (parent != null) {
            if (parent.canWrite()) flags = flags or Document.FLAG_SUPPORTS_DELETE
        }

        val displayName = resolvedFile.name
        val mimeType = getMimeType(resolvedFile)
        if (mimeType.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, resolvedDocId)
        row.add(Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(Document.COLUMN_SIZE, resolvedFile.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, resolvedFile.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }

    @TargetApi(26)
    @Throws(FileNotFoundException::class)
    override fun findDocumentPath(
        @Nullable parentDocumentId: String?,
        childDocumentId: String
    ): DocumentsContract.Path {
        var source = BASE_DIR!!
        if (parentDocumentId != null) source = getFileForDocId(parentDocumentId)
        var destination = getFileForDocId(childDocumentId)
        val pathIds = ArrayList<String>()
        while (source != destination && destination != null) {
            pathIds.add(getDocIdForFile(destination))
            destination = destination.parentFile
        }
        pathIds.add(getDocIdForFile(source))
        Collections.reverse(pathIds)
        Log.i("FolderProvider", pathIds.toString())
        return DocumentsContract.Path(getDocIdForFile(source), pathIds)
    }

    @Throws(FileNotFoundException::class)
    private fun createUriForDocId(documentId: String): Uri {
        return createUriForFile(getFileForDocId(documentId))
    }

    private fun createUriForFile(file: File): Uri {
        return DocumentsContract.buildDocumentUri(mStorageProviderAuthortiy, file.absolutePath)
    }

    private fun notifyChange(uri: Uri) {
        mContentResolver!!.notifyChange(uri, null)
    }

    companion object {
        private val BLOCKED_PACKAGES = listOf("com.dnamobile.modlymodmanager")
        private const val ALL_MIME_TYPES = "*/*"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )
    }
}
