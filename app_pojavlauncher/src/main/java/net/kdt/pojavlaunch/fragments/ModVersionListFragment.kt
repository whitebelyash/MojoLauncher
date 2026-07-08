package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

import java.io.File
import java.io.IOException

abstract class ModVersionListFragment<T> : Fragment(R.layout.fragment_mod_version_list),
    Runnable, View.OnClickListener, ExpandableListView.OnChildClickListener, ModloaderDownloadListener {
    private val mExtraTag: String
    private var mExpandableListView: ExpandableListView? = null
    private var mProgressBar: ProgressBar? = null
    private var mInflater: LayoutInflater? = null
    private var mRetryView: View? = null

    constructor(mFragmentTag: String) : super(R.layout.fragment_mod_version_list) {
        this.mExtraTag = mFragmentTag + "_proxy"
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
        this.mInflater = LayoutInflater.from(context)
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById<View>(R.id.title_textview) as TextView).text = getTitleText()
        mProgressBar = view.findViewById(R.id.mod_dl_list_progress)
        mExpandableListView = view.findViewById(R.id.mod_dl_expandable_version_list)
        mExpandableListView!!.setOnChildClickListener(this)
        mRetryView = view.findViewById(R.id.mod_dl_retry_layout)
        view.findViewById<View>(R.id.forge_installer_retry_button).setOnClickListener(this)
        val taskProxy = getTaskProxy()
        if (taskProxy != null) {
            mExpandableListView!!.isEnabled = false
            taskProxy.attachListener(this)
        }
        Thread(this).start()
    }

    override fun onStop() {
        val taskProxy = getTaskProxy()
        if (taskProxy != null) taskProxy.detachListener()
        super.onStop()
    }

    override fun run() {
        try {
            val versions = loadVersionList()
            Tools.runOnUiThread {
                if (versions != null) {
                    mExpandableListView!!.setAdapter(createAdapter(versions, mInflater!!))
                } else {
                    mRetryView!!.visibility = View.VISIBLE
                }
                mProgressBar!!.visibility = View.GONE
            }
        } catch (e: IOException) {
            Tools.runOnUiThread {
                if (context != null) {
                    Tools.showError(context!!, e)
                    mRetryView!!.visibility = View.VISIBLE
                    mProgressBar!!.visibility = View.GONE
                }
            }
        }
    }

    override fun onClick(view: View) {
        mRetryView!!.visibility = View.GONE
        mProgressBar!!.visibility = View.VISIBLE
        Thread(this).start()
    }

    override fun onChildClick(expandableListView: ExpandableListView, view: View, i: Int, i1: Int, l: Long): Boolean {
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(expandableListView.context, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return true
        }
        val forgeVersion = expandableListView.expandableListAdapter.getChild(i, i1)
        val taskProxy = ModloaderListenerProxy()
        val downloadTask = createDownloadTask(forgeVersion, taskProxy)
        setTaskProxy(taskProxy)
        taskProxy.attachListener(this)
        mExpandableListView!!.isEnabled = false
        Thread(downloadTask).start()
        return true
    }

    override fun onDownloadFinished(downloadedFile: File?) {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()!!.detachListener()
            setTaskProxy(null)
            mExpandableListView!!.isEnabled = true
            parentFragmentManager.popBackStackImmediate()
            onDownloadFinished(context, downloadedFile!!)
        }
    }

    override fun onDataNotAvailable() {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()!!.detachListener()
            setTaskProxy(null)
            mExpandableListView!!.isEnabled = true
            Tools.dialog(context,
                context.getString(R.string.global_error),
                context.getString(getNoDataMsg()))
        }
    }

    override fun onDownloadError(e: Exception) {
        Tools.runOnUiThread {
            val context = requireContext()
            getTaskProxy()!!.detachListener()
            setTaskProxy(null)
            mExpandableListView!!.isEnabled = true
            Tools.showError(context, e)
        }
    }

    private fun setTaskProxy(proxy: ModloaderListenerProxy?) {
        ExtraCore.setValue(mExtraTag, proxy)
    }

    private fun getTaskProxy(): ModloaderListenerProxy? {
        return ExtraCore.getValue(mExtraTag) as ModloaderListenerProxy?
    }

    abstract fun getTitleText(): Int
    abstract fun getNoDataMsg(): Int

    @Throws(IOException::class)
    abstract fun loadVersionList(): T

    abstract fun createAdapter(versionList: T, layoutInflater: LayoutInflater): ExpandableListAdapter
    abstract fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable
    abstract fun onDownloadFinished(context: Context, downloadedFile: File)
}
