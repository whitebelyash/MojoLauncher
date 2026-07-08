package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter

import com.kdt.mcgui.ProgressLayout

import git.artdeell.mojo.R

import net.kdt.pojavlaunch.instances.InstanceInstaller
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask
import net.kdt.pojavlaunch.modloaders.OptiFineUtils
import net.kdt.pojavlaunch.modloaders.OptiFineVersionListAdapter

import java.io.File
import java.io.IOException

class OptiFineInstallFragment : ModVersionListFragment<OptiFineUtils.OptiFineVersions>(TAG) {
    companion object {
        const val TAG = "OptiFineInstallFragment"
    }

    override fun getTitleText(): Int = R.string.of_dl_select_version

    override fun getNoDataMsg(): Int = R.string.of_dl_failed_to_scrape

    @Throws(IOException::class)
    override fun loadVersionList(): OptiFineUtils.OptiFineVersions = OptiFineUtils.downloadOptiFineVersions()

    override fun createAdapter(versionList: OptiFineUtils.OptiFineVersions, layoutInflater: LayoutInflater): ExpandableListAdapter =
        OptiFineVersionListAdapter(versionList, layoutInflater)

    private fun createInstance(version: OptiFineUtils.OptiFineVersion, listenerProxy: ModloaderListenerProxy) {
        try {
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0)
            OptiFineDownloadTask(version).prepareForInstall()
            val instanceInstaller = OptiFineUtils.createInstaller(version)
            Instances.createInstance({ instance ->
                instance.name = "OptiFine"
                instance.installer = instanceInstaller
                instance.sharedData = true
            }, "OptiFine")
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
            instanceInstaller.start()
            listenerProxy.onDownloadFinished(null)
        } catch (e: Exception) {
            listenerProxy.onDownloadError(e)
        }
    }

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable =
        Runnable { createInstance(selectedVersion as OptiFineUtils.OptiFineVersion, listenerProxy) }

    override fun onDownloadFinished(context: Context, downloadedFile: File) {
    }
}
