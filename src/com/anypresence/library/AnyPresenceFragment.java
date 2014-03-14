package com.anypresence.library;

import android.content.Context;
import android.support.v4.app.Fragment;

public class AnyPresenceFragment extends Fragment {
    /**
     * Returns a base context. This context will die when the activity dies, so
     * don't hold on to it.
     * */
    protected Context getContext() {
        return getActivity();
    }
}
