package net.kdt.pojavlaunch.fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import com.kdt.pickafile.FileListView
import com.kdt.pickafile.FileSelectedListener

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore

import java.io.File

class FileSelectorFragment : Fragment(R.layout.fragment_file_selector) {
    companion object {
        const val TAG = "FileSelectorFragment"
        const val BUNDLE_SELECT_FOLDER = "select_folder"
        const val BUNDLE_SELECT_FILE = "select_file"
        const val BUNDLE_SHOW_FILE = "show_file"
        const val BUNDLE_SHOW_FOLDER = "show_folder"
        const val BUNDLE_ROOT_PATH = "root_path"
    }

    private var mSelectFolderButton: Button? = null
    private var mCreateFolderButton: Button? = null
    private var mFileListView: FileListView? = null
    private var mFilePathView: TextView? = null

    private var mSelectFolder = true
    private var mShowFiles = true
    private var mShowFolders = true
    private var mRootPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        Tools.DIR_GAME_NEW
    else
        Environment.getExternalStorageDirectory().absolutePath

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        bindViews(view)
        parseBundle()
        if (!mSelectFolder) mSelectFolderButton!!.visibility = View.GONE
        else mSelectFolderButton!!.visibility = View.VISIBLE

        mFileListView!!.showFiles = mShowFiles
        mFileListView!!.showFolders = mShowFolders
        mFileListView!!.lockPathAt(File(mRootPath))
        mFileListView!!.setDialogTitleListener { title -> mFilePathView!!.text = removeLockPath(title) }
        mFileListView!!.refreshPath()

        mCreateFolderButton!!.setOnClickListener { v ->
            val editText = EditText(context)
            AlertDialog.Builder(context!!)
                .setTitle(R.string.folder_dialog_insert_name)
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.folder_dialog_create) { _, _ ->
                    val folder = File(mFileListView!!.fullPath, editText.text.toString())
                    val success = folder.mkdir()
                    if (success) {
                        mFileListView!!.listFileAt(File(mFileListView!!.fullPath, editText.text.toString()))
                    } else {
                        mFileListView!!.refreshPath()
                    }
                }.show()
        }

        mSelectFolderButton!!.setOnClickListener {
            ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(mFileListView!!.fullPath.absolutePath))
            Tools.removeCurrentFragment(requireActivity())
        }

        mFileListView!!.setFileSelectedListener(object : FileSelectedListener {
            override fun onFileSelected(file: File, path: String) {
                ExtraCore.setValue(ExtraConstants.FILE_SELECTOR, removeLockPath(path))
                Tools.removeCurrentFragment(requireActivity())
            }
        })
    }

    private fun removeLockPath(path: String): String {
        return path.replace(mRootPath, ".")
    }

    private fun parseBundle() {
        val bundle = arguments
        if (bundle == null) return
        mSelectFolder = bundle.getBoolean(BUNDLE_SELECT_FOLDER, mSelectFolder)
        mShowFiles = bundle.getBoolean(BUNDLE_SHOW_FILE, mShowFiles)
        mShowFolders = bundle.getBoolean(BUNDLE_SHOW_FOLDER, mShowFolders)
        mRootPath = bundle.getString(BUNDLE_ROOT_PATH, mRootPath)!!
    }

    private fun bindViews(@NonNull view: View) {
        mSelectFolderButton = view.findViewById(R.id.file_selector_select_folder)
        mCreateFolderButton = view.findViewById(R.id.file_selector_create_folder)
        mFileListView = view.findViewById(R.id.file_selector)
        mFilePathView = view.findViewById(R.id.file_selector_current_path)
    }
}
