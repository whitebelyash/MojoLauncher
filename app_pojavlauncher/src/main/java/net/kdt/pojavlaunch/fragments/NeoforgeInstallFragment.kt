package net.kdt.pojavlaunch.fragments

import android.content.Context

import androidx.annotation.NonNull

import net.kdt.pojavlaunch.modloaders.ForgelikeUtils

import git.artdeell.mojo.R

class NeoforgeInstallFragment : ForgelikeInstallFragment(ForgelikeUtils.NEOFORGE_UTILS, TAG) {
    companion object {
        const val TAG = "NeoforgeInstallFragment"
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
    }

    override fun getTitleText(): Int = R.string.neoforge_dl_select_version

    override fun getNoDataMsg(): Int = R.string.neoforge_dl_no_installer
}
