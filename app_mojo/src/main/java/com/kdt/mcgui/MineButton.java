package com.kdt.mcgui;

import mojui.content.*;
import mojui.graphics.*;
import mojui.util.*;

import mojui.core.content.res.ResourcesCompat;

import git.artdeell.mojo.R;

public class MineButton extends mojui.appcompat.widget.AppCompatButton {
	
	public MineButton(Context ctx) {
		this(ctx, null);
	}
	
	public MineButton(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
		setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.mine_button_background, null));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._13ssp));
	}

}
