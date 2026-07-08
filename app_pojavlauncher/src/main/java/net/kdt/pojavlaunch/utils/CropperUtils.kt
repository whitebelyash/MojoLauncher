package net.kdt.pojavlaunch.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import net.kdt.pojavlaunch.PojavApplication
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.imgcropper.BitmapCropBehaviour
import net.kdt.pojavlaunch.imgcropper.CropperBehaviour
import net.kdt.pojavlaunch.imgcropper.CropperView
import net.kdt.pojavlaunch.imgcropper.RegionDecoderCropBehaviour
import java.io.IOException

object CropperUtils {
    fun registerCropper(activity: AppCompatActivity, cropperReceiver: CropperReceiver): ActivityResultLauncher<*> {
        return registerCropper(ActivityContextProvider(activity), cropperReceiver)
    }

    fun registerCropper(fragment: Fragment, cropperReceiver: CropperReceiver): ActivityResultLauncher<*> {
        return registerCropper(FragmentContextProvider(fragment), cropperReceiver)
    }

    private fun registerCropper(contextProvider: ContextProvider, cropperReceiver: CropperReceiver): ActivityResultLauncher<*> {
        return contextProvider.resultCaller.registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            val context = contextProvider.context
            if (context == null) return@registerForActivityResult
            if (result == null) {
                Toast.makeText(context, R.string.cropper_select_cancelled, Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            openCropperDialog(context, result, cropperReceiver)
        }
    }

    private fun openCropperDialog(context: Context, selectedUri: Uri, cropperReceiver: CropperReceiver) {
        val contentResolver = context.contentResolver
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.cropper_title)
            .setView(R.layout.dialog_cropper)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        val cropImageView = dialog.findViewById<CropperView>(R.id.crop_dialog_view)!!
        val finishProgressBar = dialog.findViewById<View>(R.id.crop_dialog_progressbar)!!
        bindViews(dialog, cropImageView)
        cropImageView.setAspectRatio(cropperReceiver.aspectRatio)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            cropperReceiver.onCropped(cropImageView.crop(cropperReceiver.targetMaxSide))
        }
        PojavApplication.sExecutorService.execute {
            var cropperBehaviour: CropperBehaviour? = null
            try {
                cropperBehaviour = createBehaviour(cropImageView, contentResolver, selectedUri)
            } catch (e: Exception) {
                cropperReceiver.onFailed(e)
            }
            val finalBehaviour = cropperBehaviour
            Tools.runOnUiThread {
                finishSetup(dialog, finishProgressBar, cropImageView, finalBehaviour)
            }
        }
    }

    private fun fixDialogHeight(dialog: AlertDialog) {
        val dialogWindow = dialog.window
        dialogWindow?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun finishSetup(dialog: AlertDialog, progressBar: View, cropImageView: CropperView, cropperBehaviour: CropperBehaviour?) {
        if (cropperBehaviour == null) {
            dialog.dismiss()
            return
        }
        progressBar.visibility = View.GONE
        cropImageView.setCropperBehaviour(cropperBehaviour)
        cropperBehaviour.applyImage()
        cropImageView.post {
            fixDialogHeight(dialog)
            cropImageView.requestLayout()
        }
    }

    @Throws(Exception::class)
    private fun createBehaviour(cropImageView: CropperView, contentResolver: ContentResolver, selectedUri: Uri): CropperBehaviour? {
        contentResolver.openInputStream(selectedUri)?.use { inputStream ->
            try {
                val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
                val cropBehaviour = RegionDecoderCropBehaviour(cropImageView)
                cropBehaviour.setRegionDecoder(regionDecoder)
                return cropBehaviour
            } catch (e: IOException) {
                Log.w("CropperUtils", "Failed to load image into BitmapRegionDecoder", e)
            }
        }
        contentResolver.openInputStream(selectedUri)?.use { inputStream ->
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            if (originalBitmap == null) throw IOException("Image format not supported")
            val cropBehaviour = BitmapCropBehaviour(cropImageView)
            cropBehaviour.setBitmap(originalBitmap)
            return cropBehaviour
        }
    }

    private fun bindViews(alertDialog: AlertDialog, imageCropperView: CropperView) {
        val horizontalLock = alertDialog.findViewById<ToggleButton>(R.id.crop_dialog_hlock)!!
        val verticalLock = alertDialog.findViewById<ToggleButton>(R.id.crop_dialog_vlock)!!
        val reset = alertDialog.findViewById<View>(R.id.crop_dialog_reset)!!
        horizontalLock.setOnClickListener {
            imageCropperView.horizontalLock = horizontalLock.isChecked
        }
        verticalLock.setOnClickListener {
            imageCropperView.verticalLock = verticalLock.isChecked
        }
        reset.setOnClickListener {
            imageCropperView.resetTransforms()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun startCropper(resultLauncher: ActivityResultLauncher<*>) {
        val realResultLauncher = resultLauncher as ActivityResultLauncher<Array<String>>
        realResultLauncher.launch(arrayOf("image/*"))
    }

    interface CropperReceiver {
        val aspectRatio: Float
        val targetMaxSide: Int
        fun onCropped(contentBitmap: Bitmap?)
        fun onFailed(exception: Exception)
    }

    private interface ContextProvider {
        val context: Context?
        val resultCaller: ActivityResultCaller
    }

    private class FragmentContextProvider(private val mFragment: Fragment) : ContextProvider {
        override val context: Context? get() = mFragment.context
        override val resultCaller: ActivityResultCaller get() = mFragment
    }

    private class ActivityContextProvider(private val mActivity: AppCompatActivity) : ContextProvider {
        override val context: Context?
            get() = if (mActivity.isDestroyed || mActivity.isFinishing) null else mActivity
        override val resultCaller: ActivityResultCaller get() = mActivity
    }
}
