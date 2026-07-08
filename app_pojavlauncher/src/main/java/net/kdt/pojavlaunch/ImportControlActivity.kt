package net.kdt.pojavlaunch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast

import androidx.annotation.Nullable

import net.kdt.pojavlaunch.customcontrols.LayoutBitmaps
import net.kdt.pojavlaunch.utils.FileUtils

import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import git.artdeell.mojo.R

@Suppress("IOStreamConstructor")
class ImportControlActivity : Activity() {

    private var mUriData: Uri? = null
    private var mHasIntentChanged = true
    private var mIsFileVerified = false

    private var mEditText: EditText? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Tools.checkStorageInteractive(this)) {
            Tools.initStorageConstants(applicationContext)
        } else {
            return
        }

        setContentView(R.layout.activity_import_control)
        mEditText = findViewById(R.id.editText_import_control_file_name)
    }

    override fun onNewIntent(intent: Intent) {
        if (intent != null) setIntent(intent)
        mHasIntentChanged = true
    }

    override fun onPostResume() {
        super.onPostResume()
        if (!Tools.checkStorageInteractive(this)) {
            return
        }
        if (!mHasIntentChanged) return
        mIsFileVerified = false
        getUriData()
        if (mUriData == null) {
            finishAndRemoveTask()
            return
        }
        mEditText!!.setText(trimFileName(Tools.getFileName(this, mUriData!!)))
        mHasIntentChanged = false

        Thread {
            importControlFile()
            if (verify()) mIsFileVerified = true
            else runOnUiThread {
                Toast.makeText(
                    this@ImportControlActivity,
                    getText(R.string.import_control_invalid_file),
                    Toast.LENGTH_SHORT
                ).show()
                finishAndRemoveTask()
            }
        }.start()

        Tools.MAIN_HANDLER.postDelayed({
            val imm = getApplicationContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
            mEditText!!.setSelection(mEditText!!.text.length)
        }, 100)
    }

    fun startImport(view: View) {
        val fileName = trimFileName(mEditText!!.text.toString())
        if (!isFileNameValid(fileName)) {
            Toast.makeText(this, getText(R.string.import_control_invalid_name), Toast.LENGTH_SHORT).show()
            return
        }
        if (!mIsFileVerified) {
            Toast.makeText(this, getText(R.string.import_control_verifying_file), Toast.LENGTH_LONG).show()
            return
        }

        File(Tools.CTRLMAP_PATH + "/TMP_IMPORT_FILE.json").renameTo(File(Tools.CTRLMAP_PATH + "/" + fileName + ".json"))
        Toast.makeText(applicationContext, getText(R.string.import_control_done), Toast.LENGTH_SHORT).show()
        finishAndRemoveTask()
    }

    private fun importControlFile() {
        try {
            val `is` = contentResolver.openInputStream(mUriData!!)
            val os: OutputStream = FileOutputStream(Tools.CTRLMAP_PATH + "/" + "TMP_IMPORT_FILE" + ".json")
            IOUtils.copy(`is`, os)
            os.close()
            `is`!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private fun isFileNameValid(fileName: String): Boolean {
            val trimmed = trimFileName(fileName)
            if (trimmed.isEmpty()) return false
            return !FileUtils.exists(Tools.CTRLMAP_PATH + "/" + trimmed + ".json")
        }

        private fun trimFileName(fileName: String): String {
            return fileName
                .replace(".json", "")
                .replace("%..", "/")
                .replace("/", "")
                .replace("\\", "")
                .trim { it <= ' ' }
        }
    }

    private fun getUriData() {
        mUriData = intent.data
        if (mUriData != null) return
        try {
            mUriData = intent.clipData!!.getItemAt(0).uri
        } catch (_: Exception) {
        }
    }

    private fun verify(): Boolean {
        return try {
            val layout = LayoutBitmaps.load(File(Tools.CTRLMAP_PATH, "TMP_IMPORT_FILE.json"))
            val layoutJobj = JSONObject(layout.mControlsJson)
            layoutJobj.has("version") && layoutJobj.has("mControlDataList")
        } catch (e: IOException) {
            Log.w("ImportControlActivity", "Failed to validate layout", e)
            false
        } catch (e: JSONException) {
            Log.w("ImportControlActivity", "Failed to validate layout", e)
            false
        }
    }
}
