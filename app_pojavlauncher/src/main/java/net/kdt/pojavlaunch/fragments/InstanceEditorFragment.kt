package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.multirt.MultiRTUtils
import net.kdt.pojavlaunch.multirt.RTSpinnerAdapter
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog
import net.kdt.pojavlaunch.utils.CropperUtils
import net.kdt.pojavlaunch.utils.RendererCompatUtil

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

class InstanceEditorFragment : Fragment(R.layout.fragment_instance_editor), CropperUtils.CropperReceiver {
    companion object {
        const val TAG = "InstanceEditorFragment"
    }

    private var mInstance: Instance? = null
    private var mSelectedControlLayout: String? = null
    private var mSaveButton: Button? = null
    private var mDeleteButton: Button? = null
    private var mControlSelectButton: Button? = null
    private var mVersionSelectButton: Button? = null
    private var mDefaultRuntime: Spinner? = null
    private var mDefaultRenderer: Spinner? = null
    private var mDefaultName: EditText? = null
    private var mDefaultJvmArgument: EditText? = null
    private var mDefaultVersion: TextView? = null
    private var mDefaultControl: TextView? = null
    private var mInstanceIcon: ImageView? = null
    private var mSharedDataCheckbox: CheckBox? = null
    private var mRecommendedIconSize = 0
    private val mCropperLauncher: ActivityResultLauncher<*> = CropperUtils.registerCropper(this, this)

    private var mRenderNames: MutableList<String>? = null

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        val value = ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR) as String?
        if (value != null) {
            mSelectedControlLayout = value
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        bindViews(view)

        val renderersList = RendererCompatUtil.getCompatibleRenderers(view.context)
        mRenderNames = ArrayList(renderersList.rendererIds.toList())
        val renderList = ArrayList<String>(renderersList.rendererDisplayNames.size + 1)
        renderList.addAll(renderersList.rendererDisplayNames.toList())
        renderList.add(view.context.getString(R.string.global_default))
        mDefaultRenderer!!.adapter = ArrayAdapter(view.context, R.layout.item_simple_list_1, renderList)

        mSaveButton!!.setOnClickListener {
            InstanceIconProvider.dropIcon(mInstance!!)
            save()
            Tools.backToMainMenu(requireActivity())
        }

        mDeleteButton!!.setOnClickListener {
            val dialogFragment = DeleteConfirmDialogFragment()
            dialogFragment.show(childFragmentManager, "delete_dialog_confirm")
        }

        val controlSelectListener = getControlSelectListener()
        mControlSelectButton!!.setOnClickListener(controlSelectListener)
        mDefaultControl!!.setOnClickListener(controlSelectListener)

        val versionSelectListener = getVersionSelectListener()
        mVersionSelectButton!!.setOnClickListener(versionSelectListener)
        mDefaultVersion!!.setOnClickListener(versionSelectListener)

        mInstanceIcon!!.setOnClickListener { v ->
            mRecommendedIconSize = Math.max(v.width, v.height)
            CropperUtils.startCropper(mCropperLauncher)
        }

        mSharedDataCheckbox!!.setOnCheckedChangeListener { _, checked ->
            mInstance!!.sharedData = checked
            val text = if (checked) R.string.instance_shared_data_on else R.string.instance_shared_data_off
            mSharedDataCheckbox!!.setText(text)
        }

        val selectedInstance = Instances.loadSelectedInstance()
        val context = view.context
        if (selectedInstance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        } else {
            loadValues(selectedInstance, context)
        }
    }

    private fun getControlSelectListener(): View.OnClickListener {
        return View.OnClickListener {
            val bundle = Bundle(3)
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false)
            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH)

            Tools.swapFragment(requireActivity(),
                FileSelectorFragment::class.java, FileSelectorFragment.TAG, bundle)
        }
    }

    private fun getVersionSelectListener(): View.OnClickListener {
        return View.OnClickListener { v ->
            VersionSelectorDialog.open(v.context, false) { id, _ -> mDefaultVersion!!.text = id }
        }
    }

    private fun nullToEmpty(`in`: String?): String {
        return `in` ?: ""
    }

    private fun loadValues(@NonNull instance: Instance, @NonNull context: Context) {
        mInstance = instance
        mInstanceIcon!!.setImageDrawable(
            InstanceIconProvider.fetchIcon(resources, instance)
        )

        val runtimes = MultiRTUtils.getRuntimes()
        var jvmIndex = -1
        if (instance.selectedRuntime != null) {
            jvmIndex = runtimes.indexOf(Runtime(instance.selectedRuntime))
        }
        mDefaultRuntime!!.adapter = RTSpinnerAdapter(context, runtimes)
        if (jvmIndex == -1) jvmIndex = runtimes.size - 1
        mDefaultRuntime!!.setSelection(jvmIndex)

        val rendererIndex = mRenderNames!!.indexOf(instance.launchRenderer)
        if (rendererIndex == -1) {
            mDefaultRenderer!!.setSelection(mDefaultRenderer!!.adapter!!.count - 1)
        } else {
            mDefaultRenderer!!.setSelection(rendererIndex)
        }

        mDefaultVersion!!.text = instance.versionId
        mDefaultJvmArgument!!.setText(nullToEmpty(instance.jvmArgs))
        mDefaultName!!.setText(nullToEmpty(instance.name))
        mDefaultControl!!.text = if (mSelectedControlLayout == null) nullToEmpty(instance.controlLayout) else mSelectedControlLayout
        mSharedDataCheckbox!!.isChecked = instance.sharedData
    }

    private fun bindViews(@NonNull view: View) {
        mDefaultControl = view.findViewById(R.id.vprof_editor_ctrl_spinner)
        mDefaultRuntime = view.findViewById(R.id.vprof_editor_spinner_runtime)
        mDefaultRenderer = view.findViewById(R.id.vprof_editor_instance_renderer)
        mDefaultVersion = view.findViewById(R.id.vprof_editor_version_spinner)

        mDefaultName = view.findViewById(R.id.vprof_editor_instance_name)
        mDefaultJvmArgument = view.findViewById(R.id.vprof_editor_jre_args)

        mSaveButton = view.findViewById(R.id.vprof_editor_save_button)
        mDeleteButton = view.findViewById(R.id.vprof_editor_delete_button)
        mControlSelectButton = view.findViewById(R.id.vprof_editor_ctrl_button)
        mVersionSelectButton = view.findViewById(R.id.vprof_editor_version_button)
        mInstanceIcon = view.findViewById(R.id.vprof_editor_instance_icon)
        mSharedDataCheckbox = view.findViewById(R.id.vprof_editor_data_checkbox_container)
    }

    private fun save() {
        mInstance!!.versionId = mDefaultVersion!!.text.toString()
        mInstance!!.controlLayout = mDefaultControl!!.text.toString()
        mInstance!!.name = mDefaultName!!.text.toString()
        mInstance!!.jvmArgs = mDefaultJvmArgument!!.text.toString()

        if (mInstance!!.controlLayout!!.isEmpty()) mInstance!!.controlLayout = null
        if (mInstance!!.jvmArgs!!.isEmpty()) mInstance!!.jvmArgs = null

        val selectedRuntime = mDefaultRuntime!!.selectedItem as Runtime
        mInstance!!.selectedRuntime = if (selectedRuntime.name == "<Default>" || selectedRuntime.versionString == null)
            null else selectedRuntime.name

        if (mDefaultRenderer!!.selectedItemPosition == mRenderNames!!.size) mInstance!!.renderer = null
        else mInstance!!.renderer = mRenderNames!![mDefaultRenderer!!.selectedItemPosition]

        try {
            mInstance!!.write()
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    override fun getAspectRatio(): Float = 1f

    override fun getTargetMaxSide(): Int = mRecommendedIconSize

    override fun onCropped(contentBitmap: Bitmap) {
        mInstanceIcon!!.setImageBitmap(contentBitmap)
        Log.i("bitmap", "w=" + contentBitmap.width + " h=" + contentBitmap.height)
        try {
            mInstance!!.encodeNewIcon(contentBitmap)
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    override fun onFailed(exception: Exception) {
        Tools.showErrorRemote(exception)
    }
}
