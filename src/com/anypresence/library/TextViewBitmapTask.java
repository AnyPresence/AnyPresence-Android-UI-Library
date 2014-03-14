package com.anypresence.library;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

/**
 * Loads an image from a URL into the left drawable on the passed TextView.
 * */
public class TextViewBitmapTask extends BitmapTask {
    private final TextView mTextView;

    public TextViewBitmapTask(TextView textView, String url) {
        super(null, url);
        this.mTextView = textView;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        Drawable d = new BitmapDrawable(mTextView.getContext().getResources(), result);
        mTextView.setCompoundDrawablesWithIntrinsicBounds(d, mTextView.getCompoundDrawables()[1], mTextView.getCompoundDrawables()[2],
                mTextView.getCompoundDrawables()[3]);
    }
}
