package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import net.kdt.pojavlaunch.PojavApplication
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.modloaders.FabricVersion
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Future

abstract class FabriclikeInstallFragment : Fragment(R.layout.fragment_fabric_install), ModloaderDownloadListener, CompoundButton.OnCheckedChangeListener {
    private val mFabriclikeUtils: FabriclikeUtils
    private val mExtraTag: String
    private var mGameVersionSpinner: Spinner? = null
    private var mGameVersionArray: Array<FabricVersion>? = null
    private var mGameVersionFuture: Future<*>? = null
    private var mSelectedGameVersion: String? = null
    private var mLoaderVersionSpinner: Spinner? = null
    private var mLoaderVersionArray: Array<FabricVersion>? = null
    private var mLoaderVersionFuture: Future<*>? = null
    private var mSelectedLoaderVersion: String? = null
    private var mProgressBar: ProgressBar? = null
    private var mStartButton: Button? = null
    private var mRetryView: View? = null
    private var mOnlyStableCheckbox: CheckBox? = null

    protected constructor(mFabriclikeUtils: FabriclikeUtils, mFragmentTag: String) : super(R.layout.fragment_fabric_install) {
        this.mFabriclikeUtils = mFabriclikeUtils
        this.mExtraTag = mFragmentTag + "_proxy"
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mStartButton = view.findViewById(R.id.fabric_installer_start_button)
        mStartButton!!.setOnClickListener(this::onClickStart)
        mGameVersionSpinner = view.findViewById(R.id.fabric_installer_game_ver_spinner)
        mGameVersionSpinner!!.onItemSelectedListener = GameVersionSelectedListener()
        mLoaderVersionSpinner = view.findViewById(R.id.fabric_installer_loader_ver_spinner)
        mLoaderVersionSpinner!!.onItemSelectedListener = LoaderVersionSelectedListener()
        mProgressBar = view.findViewById(R.id.fabric_installer_progress_bar)
        mRetryView = view.findViewById(R.id.fabric_installer_retry_layout)
        mOnlyStableCheckbox = view.findViewById(R.id.fabric_installer_only_stable_checkbox)
        mOnlyStableCheckbox!!.setOnCheckedChangeListener(this)
        view.findViewById<View>(R.id.fabric_installer_retry_button).setOnClickListener(this::onClickRetry)
        (view.findViewById<View>(R.id.fabric_installer_label_loader_ver) as TextView).text = getString(R.string.fabric_dl_loader_version, mFabriclikeUtils.name)
        val proxy = getListenerProxy()
        if (proxy != null) {
            mStartButton!!.isEnabled = false
            proxy.attachListener(this)
        }
        updateGameVersions()
    }

    override fun onStop() {
        cancelFutureChecked(mGameVersionFuture)
        cancelFutureChecked(mLoaderVersionFuture)
        val proxy = getListenerProxy()
        if (proxy != null) {
            proxy.detachListener()
        }
        super.onStop()
    }

    private fun onClickStart(v: View) {
        if (ProgressKeeper.hasOngoingTasks()) {
            Toast.makeText(v.context, R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
            return
        }
        val proxy = ModloaderListenerProxy()
        proxy.attachListener(this)
        setListenerProxy(proxy)
        mStartButton!!.isEnabled = false
        PojavApplication.sExecutorService.execute(this::performInstallation)
    }

    private fun performInstallation() {
        try {
            val versionId = mFabriclikeUtils.install(mSelectedGameVersion, mSelectedLoaderVersion)
            if (versionId == null) {
                getListenerProxy()!!.onDataNotAvailable()
                return
            }
            Instances.createInstance({ i ->
                i.name = mFabriclikeUtils.name
                i.icon = mFabriclikeUtils.iconName
                i.versionId = versionId
            }, versionId)
            getListenerProxy()!!.onDownloadFinished(null)
        } catch (e: IOException) {
            Tools.showErrorRemote(e)
        }
    }

    @Suppress("unused")
    private fun onClickRetry(v: View) {
        mStartButton!!.isEnabled = false
        mRetryView!!.visibility = View.GONE
        mLoaderVersionSpinner!!.adapter = null
        if (mGameVersionArray == null) {
            mGameVersionSpinner!!.adapter = null
            updateGameVersions()
            return
        }
        updateLoaderVersions()
    }

    override fun onDownloadFinished(downloadedFile: File?) {
        Tools.runOnUiThread {
            getListenerProxy()!!.detachListener()
            setListenerProxy(null)
            mStartButton!!.isEnabled = true
            parentFragmentManager.popBackStackImmediate()
        }
    }

    override fun onDataNotAvailable() {
        Tools.runOnUiThread {
            val context = requireContext()
            getListenerProxy()!!.detachListener()
            setListenerProxy(null)
            mStartButton!!.isEnabled = true
            Tools.dialog(context,
                context.getString(R.string.global_error),
                context.getString(R.string.fabric_dl_cant_read_meta, mFabriclikeUtils.name))
        }
    }

    override fun onDownloadError(e: Exception) {
        Tools.runOnUiThread {
            val context = requireContext()
            getListenerProxy()!!.detachListener()
            setListenerProxy(null)
            mStartButton!!.isEnabled = true
            Tools.showError(context, e)
        }
    }

    private fun cancelFutureChecked(future: Future<*>?) {
        if (future != null && !future.isCancelled) future.cancel(true)
    }

    private fun startLoading() {
        mProgressBar!!.visibility = View.VISIBLE
        mStartButton!!.isEnabled = false
    }

    private fun stopLoading() {
        mProgressBar!!.visibility = View.GONE
    }

    private fun createAdapter(fabricVersions: Array<FabricVersion>, onlyStable: Boolean): ArrayAdapter<FabricVersion> {
        val filteredVersions = ArrayList<FabricVersion>(fabricVersions.size)
        for (fabricVersion in fabricVersions) {
            if (!onlyStable || fabricVersion.stable) filteredVersions.add(fabricVersion)
        }
        filteredVersions.trimToSize()
        return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filteredVersions)
    }

    private fun onException(myFuture: Future<*>, e: Exception?) {
        Tools.runOnUiThread {
            if (myFuture.isCancelled) return@runOnUiThread
            stopLoading()
            if (e != null) Tools.showError(requireContext(), e)
            mRetryView!!.visibility = View.VISIBLE
        }
    }

    override fun onCheckedChanged(@NonNull compoundButton: CompoundButton, b: Boolean) {
        updateGameSpinner()
        updateLoaderSpinner()
    }

    inner class LoaderVersionSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
            mSelectedLoaderVersion = (adapterView.adapter.getItem(i) as FabricVersion).version
            mStartButton!!.isEnabled = mSelectedGameVersion != null
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {
            mSelectedLoaderVersion = null
            mStartButton!!.isEnabled = false
        }
    }

    inner class LoadLoaderVersionsTask : SelfReferencingFuture.FutureInterface {
        override fun run(myFuture: Future<*>) {
            Log.i("LoadLoaderVersions", "Starting...")
            try {
                mLoaderVersionArray = mFabriclikeUtils.downloadLoaderVersions(mSelectedGameVersion!!)
                if (mLoaderVersionArray != null) onFinished(myFuture)
                else onException(myFuture, null as Exception?)
            } catch (e: IOException) {
                onException(myFuture, e)
            }
        }

        private fun onFinished(myFuture: Future<*>) {
            Tools.runOnUiThread {
                if (myFuture.isCancelled) return@runOnUiThread
                stopLoading()
                updateLoaderSpinner()
            }
        }
    }

    private fun updateLoaderVersions() {
        startLoading()
        mLoaderVersionFuture = SelfReferencingFuture(LoadLoaderVersionsTask()).startOnExecutor(PojavApplication.sExecutorService)
    }

    private fun updateLoaderSpinner() {
        if (mLoaderVersionArray == null || isDetached) return
        mLoaderVersionSpinner!!.adapter = createAdapter(mLoaderVersionArray!!, mOnlyStableCheckbox!!.isChecked)
    }

    inner class GameVersionSelectedListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
            mSelectedGameVersion = (adapterView.adapter.getItem(i) as FabricVersion).version
            cancelFutureChecked(mLoaderVersionFuture)
            updateLoaderVersions()
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {
            mSelectedGameVersion = null
            if (mLoaderVersionFuture != null) mLoaderVersionFuture!!.cancel(true)
            adapterView.adapter = null
        }
    }

    inner class LoadGameVersionsTask : SelfReferencingFuture.FutureInterface {
        override fun run(myFuture: Future<*>) {
            try {
                mGameVersionArray = mFabriclikeUtils.downloadGameVersions()
                if (mGameVersionArray != null) onFinished(myFuture)
                else onException(myFuture, null as Exception?)
            } catch (e: IOException) {
                onException(myFuture, e)
            }
        }

        private fun onFinished(myFuture: Future<*>) {
            Tools.runOnUiThread {
                if (myFuture.isCancelled) return@runOnUiThread
                stopLoading()
                updateGameSpinner()
            }
        }
    }

    private fun updateGameVersions() {
        startLoading()
        mGameVersionFuture = SelfReferencingFuture(LoadGameVersionsTask()).startOnExecutor(PojavApplication.sExecutorService)
    }

    private fun updateGameSpinner() {
        if (mGameVersionArray == null || isDetached) return
        mGameVersionSpinner!!.adapter = createAdapter(mGameVersionArray!!, mOnlyStableCheckbox!!.isChecked)
    }

    private fun getListenerProxy(): ModloaderListenerProxy? {
        return ExtraCore.getValue(mExtraTag) as ModloaderListenerProxy?
    }

    private fun setListenerProxy(listenerProxy: ModloaderListenerProxy?) {
        ExtraCore.setValue(mExtraTag, listenerProxy)
    }
}
