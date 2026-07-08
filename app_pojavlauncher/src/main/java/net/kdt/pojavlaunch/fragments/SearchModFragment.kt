package net.kdt.pojavlaunch.fragments

import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.math.MathUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.kdt.mcgui.ProgressLayout

import git.artdeell.mojo.R

import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener

import org.apache.commons.io.IOUtils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SearchModFragment : Fragment(R.layout.fragment_mod_search), ModItemAdapter.SearchResultCallback {
    companion object {
        const val TAG = "SearchModFragment"
    }

    private var mOverlay: View? = null
    private var mOverlayTopCache = 0f

    private val mOverlayPositionListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(@NonNull recyclerView: RecyclerView, dx: Int, dy: Int) {
            mOverlay!!.setY(MathUtils.clamp(mOverlay!!.getY() - dy, -mOverlay!!.getHeight().toFloat(), mOverlayTopCache))
        }
    }

    private var mSearchEditText: EditText? = null
    private var mFilterButton: ImageButton? = null
    private var mRecyclerview: RecyclerView? = null
    private var mModItemAdapter: ModItemAdapter? = null
    private var mSearchProgressBar: ProgressBar? = null
    private var mStatusTextView: TextView? = null
    private var mDefaultTextColor: ColorStateList? = null
    private var modpackApi: ModpackApi? = null

    private val mSearchFilters: SearchFilters

    private var mImportButton: Button? = null
    private var mTaskCountListener: TaskCountListener? = null

    private val mImportLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val context = context
        val contentResolver = requireContext().contentResolver
        PojavApplication.sExecutorService.execute {
            performLocalInstall(uri, context!!, contentResolver)
        }
    }

    fun performLocalInstall(uri: Uri, context: Context, contentResolver: ContentResolver) {
        val fileName = Tools.getFileName(context, uri) ?: return
        val outFile = File(Tools.DIR_CACHE, fileName + ".cf")
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, R.string.multirt_progress_caching)
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) return
            val outputStream: OutputStream = FileOutputStream(outFile)
            try {
                IOUtils.copy(inputStream, outputStream)
                outputStream.flush()
            } catch (e: IOException) {
                Tools.showErrorRemote("Error", e)
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                return
            } finally {
                inputStream.close()
                outputStream.close()
            }
        } catch (e: IOException) {
            Tools.showErrorRemote("Error", e)
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            return
        }
        try {
            modpackApi!!.installLocalModpack(fileName, outFile, null)
        } catch (e: IOException) {
            Tools.showErrorRemote("Error", e)
        } finally {
            outFile.delete()
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
        }
    }

    init {
        mSearchFilters = SearchFilters()
        mSearchFilters.isModpack = true
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
        modpackApi = CommonApi(context.getString(R.string.curseforge_api_key))
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        mModItemAdapter = ModItemAdapter(resources, modpackApi!!, this)
        ProgressKeeper.addTaskCountListener(mModItemAdapter!!)
        mOverlayTopCache = resources.getDimension(R.dimen.fragment_padding_medium)

        mOverlay = view.findViewById(R.id.search_mod_overlay)
        mSearchEditText = view.findViewById(R.id.search_mod_edittext)
        mSearchProgressBar = view.findViewById(R.id.search_mod_progressbar)
        mRecyclerview = view.findViewById(R.id.search_mod_list)
        mStatusTextView = view.findViewById(R.id.search_mod_status_text)
        mFilterButton = view.findViewById(R.id.search_mod_filter)

        mDefaultTextColor = mStatusTextView!!.textColors

        mRecyclerview!!.layoutManager = LinearLayoutManager(context)
        mRecyclerview!!.adapter = mModItemAdapter

        mRecyclerview!!.addOnScrollListener(mOverlayPositionListener)

        mSearchEditText!!.setOnEditorActionListener { _, _, _ ->
            searchMods(mSearchEditText!!.text.toString())
            mSearchEditText!!.clearFocus()
            false
        }

        mOverlay!!.post {
            val overlayHeight = mOverlay!!.height
            mRecyclerview!!.setPadding(mRecyclerview!!.paddingLeft,
                mRecyclerview!!.paddingTop + overlayHeight,
                mRecyclerview!!.paddingRight,
                mRecyclerview!!.paddingBottom)
        }
        mFilterButton!!.setOnClickListener { displayFilterDialog() }
        mImportButton = view.findViewById(R.id.mineButton_import_local_modpack)
        mImportButton!!.setOnClickListener {
            mImportLauncher.launch("*/*")
        }
        mTaskCountListener = TaskCountListener { taskCount ->
            Tools.runOnUiThread { mImportButton!!.isEnabled = taskCount == 0 }
            false
        }
        ProgressKeeper.addTaskCountListener(mTaskCountListener!!)

        searchMods(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ProgressKeeper.removeTaskCountListener(mModItemAdapter!!)
        mRecyclerview!!.removeOnScrollListener(mOverlayPositionListener)
        if (mTaskCountListener != null) {
            ProgressKeeper.removeTaskCountListener(mTaskCountListener!!)
        }
    }

    override fun onSearchFinished() {
        mSearchProgressBar!!.visibility = View.GONE
        mStatusTextView!!.visibility = View.GONE
    }

    override fun onSearchError(error: Int) {
        mSearchProgressBar!!.visibility = View.GONE
        mStatusTextView!!.visibility = View.VISIBLE
        when (error) {
            ModItemAdapter.ERROR_INTERNAL -> {
                mStatusTextView!!.setTextColor(Color.RED)
                mStatusTextView!!.setText(R.string.search_modpack_error)
            }
            ModItemAdapter.ERROR_NO_RESULTS -> {
                mStatusTextView!!.setTextColor(mDefaultTextColor!!)
                mStatusTextView!!.setText(R.string.search_modpack_no_result)
            }
        }
    }

    private fun searchMods(name: String?) {
        mSearchProgressBar!!.visibility = View.VISIBLE
        mSearchFilters.name = name ?: ""
        mModItemAdapter!!.performSearchQuery(mSearchFilters)
    }

    private fun displayFilterDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_mod_filters)
            .create()

        dialog.setOnShowListener { dialogInterface ->
            val mSelectedVersion = dialog.findViewById<TextView>(R.id.search_mod_selected_mc_version_textview)
            val mSelectVersionButton = dialog.findViewById<Button>(R.id.search_mod_mc_version_button)
            val mApplyButton = dialog.findViewById<Button>(R.id.search_mod_apply_filters)

            mSelectVersionButton!!.setOnClickListener { v -> VersionSelectorDialog.open(v.context, true) { id, _ -> mSelectedVersion!!.text = id } }

            mSelectedVersion!!.text = mSearchFilters.mcVersion

            mApplyButton!!.setOnClickListener {
                mSearchFilters.mcVersion = mSelectedVersion.text.toString()
                searchMods(mSearchEditText!!.text.toString())
                dialogInterface.dismiss()
            }
        }

        dialog.show()
    }
}
