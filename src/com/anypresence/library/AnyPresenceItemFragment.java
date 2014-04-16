package com.anypresence.library;

import android.os.Bundle;

/**
 * A fragment for pages dedicated to a single object. Handles passing the object
 * and persisting it on rotation.
 * */
public abstract class AnyPresenceItemFragment<T> extends AnyPresenceFragment {
    public static final String EXTRA_ITEM = "item";
    private T mItem;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments().containsKey(EXTRA_ITEM)) {
            mItem = (T) getArguments().getSerializable(EXTRA_ITEM);
        }
    }

    public T getItem() {
        return mItem;
    }
}
