package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import com.kdt.mcgui.ProgressLayout

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper

class SelectAuthFragment : Fragment(R.layout.fragment_select_auth_method) {
    companion object {
        const val TAG = "AUTH_SELECT_FRAGMENT"
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        val mMicrosoftButton: Button = view.findViewById(R.id.button_microsoft_authentication)
        val mLocalButton: Button = view.findViewById(R.id.button_local_authentication)
        val mElyByButton: Button = view.findViewById(R.id.button_elyby_authentication)

        mMicrosoftButton.setOnClickListener { launchAuthFragment(MicrosoftLoginFragment::class.java, MicrosoftLoginFragment.TAG) }
        mLocalButton.setOnClickListener { launchAuthFragment(LocalLoginFragment::class.java, LocalLoginFragment.TAG) }
        mElyByButton.setOnClickListener { launchAuthFragment(ElyByLoginFragment::class.java, ElyByLoginFragment.TAG) }
    }

    private fun launchAuthFragment(fragmentClass: Class<out Fragment>, fragmentTag: String) {
        if (ProgressKeeper.hasProgressKey(ProgressLayout.AUTHENTICATE)) {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
            return
        }
        Tools.swapFragment(requireActivity(), fragmentClass, fragmentTag, null)
    }
}
