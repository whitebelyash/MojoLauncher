package com.kdt.pickafile

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast

import com.ipaulpro.afilechooser.FileListAdapter

import java.io.File
import java.util.Arrays

import net.kdt.pojavlaunch.Tools
import android.os.Environment

class FileListView : LinearLayout {
    private var fullPath: File? = null
    private var mainLv: ListView? = null
    private var context: Context? = null

    private var fileSelectedListener: FileSelectedListener? = null
    private var dialogTitleListener: DialogTitleListener? = null
    private var lockPath = File("/")

    private val fileSuffixes: Array<String>
    private var showFiles = true
    private var showFolders = true

    constructor(build: AlertDialog) : this(build.context, null, emptyArray()) {
        dialogToTitleListener(build)
    }

    constructor(build: AlertDialog, fileSuffix: String) : this(build.context, null, arrayOf(fileSuffix)) {
        dialogToTitleListener(build)
    }

    constructor(build: AlertDialog, fileSuffixes: Array<String>) : this(build.context, null, fileSuffixes) {
        dialogToTitleListener(build)
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, emptyArray())

    constructor(context: Context, attrs: AttributeSet?, fileSuffixes: Array<String>) : this(context, attrs, 0, fileSuffixes)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int, fileSuffixes: Array<String>) : super(context, attrs, defStyle) {
        this.fileSuffixes = fileSuffixes
        init(context)
    }

    private fun dialogToTitleListener(dialog: AlertDialog) {
        if (dialog != null) dialogTitleListener = object : DialogTitleListener {
            override fun onChangeDialogTitle(newTitle: String) {
                dialog.setTitle(newTitle)
            }
        }
    }

    fun init(context: Context) {
        this.context = context

        val layParam = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        orientation = VERTICAL

        mainLv = ListView(context)
        mainLv!!.onItemClickListener = AdapterView.OnItemClickListener { p1, _, p3, _ ->
            val mainFile = File(p1.getItemAtPosition(p3).toString())
            if (p3 == 0 && lockPath != fullPath) {
                parentDir()
            } else {
                listFileAt(mainFile)
            }
        }

        mainLv!!.onItemLongClickListener = AdapterView.OnItemLongClickListener { p1, _, p3, _ ->
            val mainFile = File(p1.getItemAtPosition(p3).toString())
            if (mainFile.isFile) {
                fileSelectedListener?.onFileLongClick(mainFile, mainFile.absolutePath)
                return@OnItemLongClickListener true
            }
            false
        }
        addView(mainLv, layParam)

        try {
            listFileAt(Environment.getExternalStorageDirectory())
        } catch (_: NullPointerException) {
        }
    }

    fun setFileSelectedListener(listener: FileSelectedListener) {
        fileSelectedListener = listener
    }

    fun setDialogTitleListener(listener: DialogTitleListener) {
        dialogTitleListener = listener
    }

    fun listFileAt(path: File) {
        try {
            if (path.exists()) {
                if (path.isDirectory) {
                    fullPath = path

                    val listFile = path.listFiles()
                    val fileAdapter = FileListAdapter(context!!)
                    if (path != lockPath) {
                        fileAdapter.add(File(path, ".."))
                    }

                    if (listFile != null && listFile.isNotEmpty()) {
                        Arrays.sort(listFile, SortFileName())

                        for (file in listFile) {
                            if (file.isDirectory) {
                                if (showFolders && (!file.name.startsWith(".") || file.name == ".minecraft"))
                                    fileAdapter.add(file)
                                continue
                            }

                            if (showFiles) {
                                if (fileSuffixes.isNotEmpty()) {
                                    for (suffix in fileSuffixes) {
                                        if (file.name.endsWith(".$suffix")) {
                                            fileAdapter.add(file)
                                            break
                                        }
                                    }
                                } else {
                                    fileAdapter.add(file)
                                }
                            }
                        }
                    }
                    mainLv!!.adapter = fileAdapter
                    dialogTitleListener?.onChangeDialogTitle(path.absolutePath)
                } else {
                    fileSelectedListener?.onFileSelected(path, path.absolutePath)
                }
            } else {
                Toast.makeText(context, "This folder (or file) doesn't exist", Toast.LENGTH_SHORT).show()
                refreshPath()
            }
        } catch (e: Exception) {
            Tools.showError(context!!, e)
        }
    }

    fun getFullPath(): File? {
        return fullPath
    }

    fun refreshPath() {
        listFileAt(fullPath!!)
    }

    fun parentDir() {
        if (fullPath!!.absolutePath != "/") {
            listFileAt(fullPath!!.parentFile!!)
        }
    }

    fun lockPathAt(path: File) {
        lockPath = path
        listFileAt(path)
    }

    fun setShowFiles(showFiles: Boolean) {
        this.showFiles = showFiles
    }

    fun setShowFolders(showFolders: Boolean) {
        this.showFolders = showFolders
    }
}
