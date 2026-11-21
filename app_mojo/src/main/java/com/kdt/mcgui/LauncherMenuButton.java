package com.kdt.mcgui;

import mojui.content.Context;
import mojui.content.res.Resources;
import mojui.content.res.TypedArray;
import mojui.util.AttributeSet;
import mojui.util.TypedValue;
import mojui.view.Gravity;
import mojui.view.ViewGroup;
import mojui.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import mojui.core.content.res.ResourcesCompat;


import git.artdeell.mojo.R;

import fr.spse.extended_view.ExtendedButton;

public class LauncherMenuButton extends ExtendedButton {

    public LauncherMenuButton(@NonNull Context context) {
        super(context);
        setSettings();
    }
    public LauncherMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setSettings();
    }


    /** Set style stuff */
    private void setSettings(){
        Resources resources = getContext().getResources();

        int padding = resources.getDimensionPixelSize(R.dimen._22sdp);
        setCompoundDrawablePadding(padding);
        setPaddingRelative(padding, 0, 0, 0);
        setGravity(Gravity.CENTER_VERTICAL);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._12ssp));

        // Set drawable size
        int[] sizes = getExtendedViewData().getSizeCompounds();
        sizes[0] = resources.getDimensionPixelSize(R.dimen._30sdp);
        getExtendedViewData().setSizeCompounds(sizes);
        postProcessDrawables();
    }
}
