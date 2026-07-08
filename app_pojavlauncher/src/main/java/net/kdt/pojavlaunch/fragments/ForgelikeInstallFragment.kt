package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter

import com.kdt.mcgui.ProgressLayout

import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import net.kdt.pojavlaunch.modloaders.ForgelikeVersionListAdapter
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy

import java.io.File
import java.io.IOException

abstract class ForgelikeInstallFragment : ModVersionListFragment<List<String>> {
    private val mUtils: ForgelikeUtils

    constructor(utils: ForgelikeUtils, mFragmentTag: String) : super(mFragmentTag) {
        this.mUtils = utils
    }

    @Throws(IOException::class)
    override fun loadVersionList(): List<String> = mUtils.downloadVersions()

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable =
        Runnable { createInstance(selectedVersion as String, listenerProxy) }

    override fun createAdapter(versionList: List<String>, layoutInflater: LayoutInflater): ExpandableListAdapter =
        ForgelikeVersionListAdapter(versionList, layoutInflater, mUtils)

    override fun onDownloadFinished(context: Context, downloadedFile: File) {
    }

    private fun createInstance(selectedVersion: String, listenerProxy: ModloaderListenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
            val instanceInstaller = mUtils.createInstaller(selectedVersion)
            Instances.createInstance({ instance ->
                instance.name = mUtils.name
                instance.icon = mUtils.iconName
                instance.installer = instanceInstaller
            }, selectedVersion)
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: IOException) {
            listenerProxy.onDownloadError(e)
        }
    }
}
