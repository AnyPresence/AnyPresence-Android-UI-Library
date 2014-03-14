package com.anypresence.library;

import java.io.Serializable;

import android.os.Bundle;

/**
 * An activity for pages dedicated to a single object. Handles passing the
 * object and persisting it on rotation.
 * */
public abstract class AnyPresenceItemActivity<T extends Serializable> extends AnyPresenceActivity {
    public static final String EXTRA_ITEM = "item";
    protected T mItem;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ITEM)) {
            mItem = (T) savedInstanceState.get(EXTRA_ITEM);
        }
        else {
            mItem = (T) getIntent().getSerializableExtra(EXTRA_ITEM);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(EXTRA_ITEM, mItem);
    }

    /**
     * Returns the current item.
     * */
    public T getItem() {
        return mItem;
    }
}
