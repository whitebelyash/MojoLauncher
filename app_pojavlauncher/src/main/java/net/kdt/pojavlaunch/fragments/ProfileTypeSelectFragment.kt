package net.kdt.pojavlaunch.fragments

import android.os.Bundle
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment

import git.artdeell.mojo.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances

import java.io.IOException

class ProfileTypeSelectFragment : Fragment(R.layout.fragment_profile_type) {
    companion object {
        const val TAG = "ProfileTypeSelectFragment"
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.vanilla_profile).setOnClickListener {
            try {
                val instance = Instances.createDefaultInstance()
                Instances.setSelectedInstance(instance)
                Tools.swapFragment(requireActivity(), InstanceEditorFragment::class.java,
                    InstanceEditorFragment.TAG, Bundle(1))
            } catch (e: IOException) {
                Tools.showError(view.context, e)
            }
        }

        view.findViewById<View>(R.id.optifine_profile).setOnClickListener {
            Tools.swapFragment(requireActivity(), OptiFineInstallFragment::class.java,
                OptiFineInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_fabric).setOnClickListener {
            Tools.swapFragment(requireActivity(), FabricInstallFragment::class.java, FabricInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_forge).setOnClickListener {
            Tools.swapFragment(requireActivity(), ForgeInstallFragment::class.java, ForgeInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_modpack).setOnClickListener {
            Tools.swapFragment(requireActivity(), SearchModFragment::class.java, SearchModFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_quilt).setOnClickListener {
            Tools.swapFragment(requireActivity(), QuiltInstallFragment::class.java, QuiltInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_bta).setOnClickListener {
            Tools.swapFragment(requireActivity(), BTAInstallFragment::class.java, BTAInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_neoforge).setOnClickListener {
            Tools.swapFragment(requireActivity(), NeoforgeInstallFragment::class.java, NeoforgeInstallFragment.TAG, null)
        }
        view.findViewById<View>(R.id.modded_profile_legacy_fabric).setOnClickListener {
            Tools.swapFragment(requireActivity(), LegacyFabricInstallFragment::class.java, LegacyFabricInstallFragment.TAG, null)
        }
    }
}
