package net.kdt.pojavlaunch.fragments

import android.content.Context

import androidx.annotation.NonNull

import git.artdeell.mojo.R

import net.kdt.pojavlaunch.modloaders.ForgelikeUtils

class ForgeInstallFragment : ForgelikeInstallFragment(ForgelikeUtils.FORGE_UTILS, TAG) {
    companion object {
        const val TAG = "ForgeInstallFragment"
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
    }

    override fun getTitleText(): Int = R.string.forge_dl_select_version

    override fun getNoDataMsg(): Int = R.string.forge_dl_no_installer
}
