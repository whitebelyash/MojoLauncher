package net.kdt.pojavlaunch.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.utils.jre.DotnetAssets;

/**
 * First-run import flow for a Vintage Story (.NET) instance: selects the VSMobile
 * game data archive and unpacks it together with the bundled .NET runtime + fonts.
 */
public class VintageStoryInstallFragment extends Fragment {
    public static final String TAG = "VintageStoryInstallFragment";

    private static final String ARG_INSTANCE_NAME = "instance_name";

    private TextView mVersionText;
    private ProgressBar mProgressBar;
    private Button mSelectButton;

    private final ActivityResultLauncher<String[]> mArchiveLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), (data)->{
                if(mSelectButton != null) mSelectButton.setEnabled(true);
                if(data != null) importGame(data);
            });

    public static VintageStoryInstallFragment newInstance(String instanceName) {
        Bundle args = new Bundle(1);
        args.putString(ARG_INSTANCE_NAME, instanceName);
        VintageStoryInstallFragment fragment = new VintageStoryInstallFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public VintageStoryInstallFragment() {
        super(R.layout.fragment_vintagestory_install);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mVersionText = view.findViewById(R.id.vs_import_status);
        mProgressBar = view.findViewById(R.id.vs_import_progress);
        mSelectButton = view.findViewById(R.id.vs_import_select_button);

        String name = null;
        if(getArguments() != null) name = getArguments().getString(ARG_INSTANCE_NAME);
        if(name != null) mVersionText.setText(String.format(getString(R.string.vs_import_selecting_for), name));

        mSelectButton.setOnClickListener(v -> {
            mSelectButton.setEnabled(false);
            mArchiveLauncher.launch(new String[]{"*/*"});
        });
    }

    private void importGame(Uri gameUri) {
        mSelectButton.setEnabled(false);
        mVersionText.setText(R.string.vs_import_extracting);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);

        PojavApplication.sExecutorService.execute(() -> {
            // The instance is only created once the import succeeds; if it fails it is
            // removed again so no orphaned empty instance is left behind.
            Instance instance = null;
            try {
                instance = Instances.createInstance((i)-> {
                    i.type = Instance.TYPE_VINTAGE_STORY;
                    i.name = "Vintage Story";
                }, null);
                DotnetAssets.ensureRuntime(requireContext());
                DotnetAssets.ensureFontconfig(requireContext());
                if(!DotnetAssets.isInstalled()) {
                    DotnetAssets.installGameData(requireContext(), gameUri);
                }
                instance.write();
                Instances.setSelectedInstance(instance);
                requireActivity().runOnUiThread(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.vs_import_done, Toast.LENGTH_SHORT).show();
                    Tools.backToMainMenu(requireActivity());
                });
            } catch (Throwable t) {
                if(instance != null) {
                    try { Instances.removeInstance(instance); } catch (java.io.IOException ignored) {}
                }
                requireActivity().runOnUiThread(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    mSelectButton.setEnabled(true);
                    mVersionText.setText(R.string.vs_import_failed);
                    Tools.showError(requireContext(), t);
                });
            }
        });
    }
}
