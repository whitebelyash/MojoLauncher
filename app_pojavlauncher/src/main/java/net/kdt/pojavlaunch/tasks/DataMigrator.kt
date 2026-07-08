package net.kdt.pojavlaunch.tasks

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import com.kdt.mcgui.ProgressLayout
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.PojavApplication.sExecutorService
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.IOException

class DataMigrator(private val activity: Activity, private val uri: Uri) {
    private var progress = 0.0

    private fun updateProgress(step: Double, entry: String) {
        progress += step
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, minOf(100, progress.toInt()), activity.getString(R.string.migration_progress_copying, entry))
    }

    private fun getFilesUri(uri: Uri): Uri {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val to = arrayOf("files")
        val cursor = activity.contentResolver.query(uri, projection, null, to, null)
            ?: throw IllegalArgumentException()
        cursor.moveToFirst()
        return DocumentsContract.buildChildDocumentsUriUsingTree(uri, cursor.getString(0))
    }

    private fun executeMigrate() {
        Log.i("DataMigration", "Begin data migration!")
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, 0)
        val root = File(Tools.DIR_GAME_HOME)
        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            copyFileTree(activity, getFilesUri(DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))), root, 100.0)
            Tools.runOnUiThread { Toast.makeText(activity, R.string.migration_progress_finish, Toast.LENGTH_LONG).show() }
        } catch (e: Exception) {
            Log.e("DataMigration", "Failed to import data to the launcher: ${e.message}")
            Tools.runOnUiThread { Toast.makeText(activity, R.string.migration_progress_failed, Toast.LENGTH_LONG).show() }
            Tools.showErrorRemote(e)
        }
        progress = 0.0
        Log.i("DataMigration", "End data migration!")
        ProgressLayout.clearProgress(ProgressLayout.DATA_MIGRATION)
    }

    fun migrateData() {
        val authority = uri.authority ?: return
        if (!authority.contains(activity.getString(R.string.group_id))) {
            Toast.makeText(activity, R.string.migration_progress_foreign, Toast.LENGTH_LONG).show()
            return
        }
        if (authority == activity.getString(R.string.storageProviderAuthorities)) {
            Toast.makeText(activity, R.string.migration_progress_self, Toast.LENGTH_LONG).show()
            return
        }
        sExecutorService.submit { executeMigrate() }
    }

    @Throws(IOException::class)
    private fun copyFileTree(activity: Activity, source: Uri, dest: File, progressPortion: Double) {
        val cr = activity.contentResolver
        cr.query(source, TREE_PROJECTION, null, null, null).use { cursor ->
            if (cursor == null) throw IllegalArgumentException()
            val count = cursor.count
            val step = progressPortion / count.toDouble()
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val file = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE))
                val child = DocumentsContract.buildChildDocumentsUriUsingTree(source, id)
                if (type == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val destDir = File(dest, file)
                    if (destDir.exists() && dest.name == "instances") continue
                    if (!destDir.exists()) destDir.mkdirs()
                    copyFileTree(activity, child, destDir, step)
                } else {
                    val destFile = File(dest, file)
                    if (destFile.length() == size) continue
                    Tools.write(cr.openInputStream(child), destFile)
                }
                updateProgress(step, file)
            }
        }
    }

    companion object {
        private val TREE_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }
}
