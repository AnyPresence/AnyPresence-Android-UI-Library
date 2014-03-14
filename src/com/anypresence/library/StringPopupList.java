package com.anypresence.library;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Pass a list of Strings as the options for this spinner. A more manual version
 * of AnyPresenceSpinner.
 * */
public class StringPopupList extends GenericPopupList<String> {
    public StringPopupList(Context context) {
        super(context);
    }

    public StringPopupList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StringPopupList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getItemLabel(String item) {
        return item;
    }
}
