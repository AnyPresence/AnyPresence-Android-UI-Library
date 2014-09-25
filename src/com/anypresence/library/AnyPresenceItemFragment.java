package com.anypresence.library;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment for pages dedicated to a single object. Handles passing the object
 * and persisting it on rotation.
 * */
public abstract class AnyPresenceItemFragment<T extends Serializable> extends AnyPresenceFragment {
    public static final String EXTRA_ITEM = "item";
    T mItem;

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if(savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ITEM)) {
            mItem = (T) savedInstanceState.get(EXTRA_ITEM);
        }
    	else if(getArguments() != null && getArguments().containsKey(EXTRA_ITEM)) {
            mItem = (T) getArguments().getSerializable(EXTRA_ITEM);
        }
    	else {
            try {
                mItem = ((Class<T>)((ParameterizedType) getClass().
                           getGenericSuperclass()).getActualTypeArguments()[0]).newInstance();
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(EXTRA_ITEM, mItem);
    }

    public T getItem() {
        return mItem;
    }
}
