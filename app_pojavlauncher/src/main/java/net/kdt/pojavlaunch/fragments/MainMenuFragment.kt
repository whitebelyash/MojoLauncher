package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import com.kdt.mcgui.mcVersionSpinner

import net.kdt.pojavlaunch.CustomControlsActivity
import git.artdeell.mojo.R

import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.openPath
import net.kdt.pojavlaunch.Tools.shareLog
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.utils.FileUtils

import java.io.File

class MainMenuFragment : Fragment(R.layout.fragment_launcher) {
    companion object {
        const val TAG = "MainMenuFragment"
    }

    private var mVersionSpinner: mcVersionSpinner? = null

    private val mModInstallerLauncher: ActivityResultLauncher<Any> =
        registerForActivityResult(OpenDocumentWithExtension("jar")) { data ->
            if (data != null) Tools.launchModInstaller(requireContext(), data)
        }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        val mNewsButton: Button = view.findViewById(R.id.news_button)
        val mDiscordButton: Button = view.findViewById(R.id.social_media_button)
        val mCustomControlButton: Button = view.findViewById(R.id.custom_control_button)
        val mInstallJarButton: Button = view.findViewById(R.id.install_jar_button)
        val mShareLogsButton: Button = view.findViewById(R.id.share_logs_button)
        val mOpenDirectoryButton: Button = view.findViewById(R.id.open_files_button)

        val mEditProfileButton: ImageButton = view.findViewById(R.id.edit_profile_button)
        val mPlayButton: Button = view.findViewById(R.id.play_button)
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner)

        mNewsButton.setOnClickListener { Tools.openURL(requireActivity(), Tools.URL_HOME) }
        mDiscordButton.setOnClickListener { Tools.openURL(requireActivity(), getString(R.string.social_media_invite)) }
        mCustomControlButton.setOnClickListener { startActivity(Intent(requireContext(), CustomControlsActivity::class.java)) }
        mInstallJarButton.setOnClickListener { runInstallerWithConfirmation() }
        mEditProfileButton.setOnClickListener { mVersionSpinner!!.openProfileEditor(requireActivity()) }

        mPlayButton.setOnClickListener { ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true) }

        mShareLogsButton.setOnClickListener { shareLog(requireContext()) }

        mOpenDirectoryButton.setOnClickListener { openGameDirectory(it.context) }

        mNewsButton.setOnLongClickListener {
            Tools.swapFragment(requireActivity(), GamepadMapperFragment::class.java, GamepadMapperFragment.TAG, null)
            true
        }
    }

    private fun openGameDirectory(context: Context) {
        val instance = Instances.loadSelectedInstance()
        if (instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show()
            return
        }
        val gameDirectory = instance.gameDirectory
        if (FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false)
        } else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
    }

    private fun runInstallerWithConfirmation() {
        if (ProgressKeeper.taskCount == 0) {
            mModInstallerLauncher.launch(null)
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show()
    }
}
