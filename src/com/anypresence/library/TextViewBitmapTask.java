package com.anypresence.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

/**
 * Loads an image from a URL into the left drawable on the passed TextView.
 * */
public class TextViewBitmapTask extends BitmapTask {
    private final TextView mTextView;
    private final Side mSide;

    public static enum Side {
        TOP, BOTTOM, LEFT, RIGHT;
    }

    public TextViewBitmapTask(Context context, TextView textView, String url, Side side) {
        super(context, null, url);
        this.mTextView = textView;
        this.mSide = side;
    }

    @Override
    protected void onPreExecute() {
        Bitmap bitmap = loadCache();
        if(bitmap != null) {
            Drawable d = new BitmapDrawable(mTextView.getContext().getResources(), bitmap);
            switch(mSide) {
            case TOP:
                mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], d, mTextView.getCompoundDrawables()[2],
                        mTextView.getCompoundDrawables()[3]);
                break;
            case BOTTOM:
                mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], mTextView.getCompoundDrawables()[1],
                        mTextView.getCompoundDrawables()[2], d);
                break;
            case LEFT:
                mTextView.setCompoundDrawablesWithIntrinsicBounds(d, mTextView.getCompoundDrawables()[1], mTextView.getCompoundDrawables()[2],
                        mTextView.getCompoundDrawables()[3]);
                break;
            case RIGHT:
                mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], mTextView.getCompoundDrawables()[1], d,
                        mTextView.getCompoundDrawables()[3]);
                break;
            }

            Log.d(AnyPresenceActivity.TAG, "Saving bitmap to memory.");
            persist(bitmap);
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        Drawable d = new BitmapDrawable(mTextView.getContext().getResources(), result);
        switch(mSide) {
        case TOP:
            mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], d, mTextView.getCompoundDrawables()[2],
                    mTextView.getCompoundDrawables()[3]);
            break;
        case BOTTOM:
            mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], mTextView.getCompoundDrawables()[1],
                    mTextView.getCompoundDrawables()[2], d);
            break;
        case LEFT:
            mTextView.setCompoundDrawablesWithIntrinsicBounds(d, mTextView.getCompoundDrawables()[1], mTextView.getCompoundDrawables()[2],
                    mTextView.getCompoundDrawables()[3]);
            break;
        case RIGHT:
            mTextView.setCompoundDrawablesWithIntrinsicBounds(mTextView.getCompoundDrawables()[0], mTextView.getCompoundDrawables()[1], d,
                    mTextView.getCompoundDrawables()[3]);
            break;
        }
    }
}
