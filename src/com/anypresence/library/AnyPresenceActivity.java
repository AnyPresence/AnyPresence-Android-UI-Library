package com.anypresence.library;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;

public class AnyPresenceActivity extends ActionBarActivity {
    protected static final String TAG = "ANYPRESENCE";

    /**
     * Returns a base context. This context will die when the activity dies, so
     * don't hold on to it.
     * */
    protected Context getContext() {
        return this;
    }
}
