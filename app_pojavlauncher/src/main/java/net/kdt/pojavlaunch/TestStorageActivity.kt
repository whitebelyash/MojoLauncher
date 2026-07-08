package net.kdt.pojavlaunch

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.tasks.AsyncAssetManager

import git.artdeell.mojo.R

class TestStorageActivity : Activity() {
    private val REQUEST_STORAGE_REQUEST_CODE = 1
    private var mPermissionRequestDialog: AlertDialog? = null
    private var mPermsRequired = false
    private var mPermsDialogShown = false

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPermsDialogShown = false
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) {
            mPermsRequired = true
        } else exit()
    }

    override fun onResume() {
        super.onResume()
        if (!mPermsRequired) return
        if (!mPermsDialogShown) requestStoragePermission()
        else showRerequestDialog()
    }

    override fun onPause() {
        super.onPause()
        mPermissionRequestDialog?.dismiss()
    }

    private fun showRerequestDialog() {
        mPermissionRequestDialog?.dismiss()
        mPermissionRequestDialog = AlertDialog.Builder(this)
            .setTitle(R.string.global_error)
            .setMessage(R.string.toast_permission_denied)
            .setPositiveButton(android.R.string.ok) { _, _ -> requestStoragePermission() }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermsRequired = false
                exit()
            } else {
                mPermsDialogShown = true
                showRerequestDialog()
            }
        }
    }

    companion object {
        fun isStorageAllowed(context: Context): Boolean {
            val result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val result2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            return result1 == PackageManager.PERMISSION_GRANTED &&
                    result2 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_REQUEST_CODE)
    }

    private fun exit() {
        if (!Tools.checkStorageRoot(this)) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            return
        }
        LauncherPreferences.loadPreferences(this)
        AsyncAssetManager.unpackComponents(this)
        AsyncAssetManager.unpackSingleFiles(this)

        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
        finish()
    }
}
