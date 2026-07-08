package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.modloaders.BTADownloadTask
import net.kdt.pojavlaunch.modloaders.BTAUtils
import net.kdt.pojavlaunch.modloaders.BTAVersionListAdapter
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy

import java.io.File
import java.io.IOException

class BTAInstallFragment : ModVersionListFragment<BTAUtils.BTAVersionList>(TAG) {
    companion object {
        const val TAG = "BTAInstallFragment"
    }

    override fun getTitleText(): Int = R.string.select_bta_version

    override fun getNoDataMsg(): Int = R.string.modloader_dl_failed_to_load_list

    @Throws(IOException::class)
    override fun loadVersionList(): BTAUtils.BTAVersionList = BTAUtils.downloadVersionList()

    override fun createAdapter(versionList: BTAUtils.BTAVersionList, layoutInflater: LayoutInflater): ExpandableListAdapter =
        BTAVersionListAdapter(versionList, layoutInflater)

    override fun createDownloadTask(selectedVersion: Any, listenerProxy: ModloaderListenerProxy): Runnable =
        BTADownloadTask(listenerProxy, selectedVersion as BTAUtils.BTAVersion)

    override fun onDownloadFinished(context: Context, downloadedFile: File) {
    }
}
