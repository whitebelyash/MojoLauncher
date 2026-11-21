package com.kdt.mcgui;

import mojui.content.Context;
import mojui.graphics.BlendMode;
import mojui.graphics.Canvas;
import mojui.graphics.Color;
import mojui.graphics.Paint;
import mojui.graphics.drawable.ColorDrawable;
import mojui.graphics.drawable.Drawable;
import mojui.util.AttributeSet;
import mojui.widget.ProgressBar;

import androidx.annotation.StringRes;
import mojui.core.content.res.ResourcesCompat;

import git.artdeell.mojo.R;

public class TextProgressBar extends ProgressBar {

    private int mTextPadding = 0;
    public TextProgressBar(Context context) {super(context, null, mojui.R.attr.progressBarStyleHorizontal); init();}

    public TextProgressBar(Context context, AttributeSet attrs) {super(context, attrs, mojui.R.attr.progressBarStyleHorizontal); init();}
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, mojui.R.attr.progressBarStyleHorizontal);
        init();
    }
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, mojui.R.attr.progressBarStyleHorizontal, defStyleRes);
        init();
    }

    private Paint mTextPaint;
    private String mText = "";

    private void init(){
        setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.view_text_progressbar, null));
        setProgress(35);
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        mTextPaint.setAntiAlias(true);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mTextPaint.setTextSize((float) ((getHeight()- getPaddingBottom() - getPaddingTop()) * 0.55));
        int xPos = (int) Math.max(Math.min((getProgress() * getWidth() / getMax()) + mTextPadding, getWidth() - mTextPaint.measureText(mText) - mTextPadding) , mTextPadding);
        int yPos = (int) ((getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;

        canvas.drawText(mText, xPos, yPos, mTextPaint);
    }


    public final void setText(@StringRes int resid) {
        setText(getContext().getResources().getText(resid).toString());
    }

    public final void setText(String text){
        mText = text;
        invalidate();
    }

    public final void setTextPadding(int padding){
        mTextPadding = padding;
    }
}
