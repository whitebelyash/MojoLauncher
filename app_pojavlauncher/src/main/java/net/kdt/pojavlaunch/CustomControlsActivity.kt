package net.kdt.pojavlaunch

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView

import androidx.activity.result.ActivityResultLauncher
import androidx.drawerlayout.widget.DrawerLayout

import com.google.gson.JsonSyntaxException

import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData
import net.kdt.pojavlaunch.customcontrols.ControlLayout
import net.kdt.pojavlaunch.customcontrols.EditorExitable
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.CropperUtils

import java.io.IOException

import git.artdeell.mojo.R


class CustomControlsActivity : BaseActivity(), EditorExitable, CropperUtils.CropperReceiver {
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerNavigationView: ListView? = null
    private var mControlLayout: ControlLayout? = null
    private var mCropperReceiver: CropperUtils.CropperReceiver? = null
    private var mCropperLauncher: ActivityResultLauncher<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCropperLauncher = CropperUtils.registerCropper(this, this)

        setContentView(R.layout.activity_custom_controls)

        mControlLayout = findViewById(R.id.customctrl_controllayout)
        mDrawerLayout = findViewById(R.id.customctrl_drawerlayout)
        mDrawerNavigationView = findViewById(R.id.customctrl_navigation_view)
        val mPullDrawerButton = findViewById<View>(R.id.drawer_button)

        mPullDrawerButton.setOnClickListener { mDrawerLayout!!.openDrawer(mDrawerNavigationView!!) }
        mDrawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        mDrawerNavigationView!!.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.menu_customcontrol_customactivity))
        mDrawerNavigationView!!.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            when (position) {
                0 -> mControlLayout!!.addControlButton(ControlData("New"))
                1 -> mControlLayout!!.addDrawer(ControlDrawerData())
                2 -> mControlLayout!!.addJoystickButton(ControlJoystickData())
                3 -> mControlLayout!!.openLoadDialog()
                4 -> mControlLayout!!.openSaveDialog(this)
                5 -> mControlLayout!!.openSetDefaultDialog()
                6 -> {
                    try {
                        val contentUri = DocumentsContract.buildDocumentUri(getString(R.string.storageProviderAuthorities), mControlLayout!!.saveToDirectory(mControlLayout!!.mLayoutFileName!!))

                        val shareIntent = Intent()
                        shareIntent.action = Intent.ACTION_SEND
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        shareIntent.type = "application/json"
                        startActivity(shareIntent)

                        val sendIntent = Intent.createChooser(shareIntent, mControlLayout!!.mLayoutFileName)
                        startActivity(sendIntent)
                    } catch (e: Exception) {
                        Tools.showError(this, e)
                    }
                }
            }
            mDrawerLayout!!.closeDrawers()
        }
        mControlLayout!!.setModifiable(true)
    }

    override fun onAttachedToWindow() {
        mControlLayout!!.post {
            try {
                mControlLayout!!.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH)
            } catch (e: IOException) {
                Tools.showError(this, e)
            } catch (e: JsonSyntaxException) {
                Tools.showError(this, e)
            }
        }
    }

    fun startCropping(cropperReceiver: CropperUtils.CropperReceiver) {
        mCropperReceiver = cropperReceiver
        CropperUtils.startCropper(mCropperLauncher!!)
    }

    override fun onBackPressed() {
        mControlLayout!!.askToExit(this)
    }

    override fun exitEditor() {
        super.onBackPressed()
    }

    override val aspectRatio: Float
        get() = mCropperReceiver?.aspectRatio ?: 1f
    override val targetMaxSide: Int
        get() = mCropperReceiver?.targetMaxSide ?: 128

    override fun onCropped(contentBitmap: Bitmap?) {
        mCropperReceiver?.onCropped(contentBitmap)
    }

    override fun onFailed(exception: Exception) {
        mCropperReceiver?.onFailed(exception)
    }
}
