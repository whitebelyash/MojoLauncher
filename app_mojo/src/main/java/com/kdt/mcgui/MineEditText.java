package com.kdt.mcgui;

import mojui.content.*;
import mojui.util.*;
import mojui.graphics.*;
import mojui.widget.EditText;

public class MineEditText extends mojui.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setBackgroundColor(Color.parseColor("#131313"));
		setPadding(5, 5, 5, 5);
	}
}
