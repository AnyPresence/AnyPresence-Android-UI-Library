
package com.anypresence.library;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

import com.anypresence.library.AnyPresenceFragment;
import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.sdk.APObject;


import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public abstract class SimpleEmbeddedListFragment<T extends APObject> extends com.anypresence.library.AnyPresenceViewFragment<T> {

    private static final String TAG = "SimpleEmbeddedListFragment";

    private Context mContext;

    static class ViewHolder {
        ViewGroup layout;
    }

    ViewHolder holder = new ViewHolder();

    @Override
    protected View createAuthorizedView() {
        View root = new View(getContext());
        return root;
    }

    public SimpleEmbeddedListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup)super.onCreateView(inflater, container, savedInstanceState);
        View embeddedListLayout = View.inflate(getContext(), R.layout.fragment_simple_embedded_list, root);
        return embeddedListLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * Returns a base context. This context will die when the activity dies, so
     * don't hold on to it.
     * */
    protected Context getContext() {
        return mContext;
    }

    protected void update(List<T> objects) {
        if (holder.layout == null) {
            if (getView() == null) return;

            holder.layout = (ViewGroup)getView().findViewById(R.id.embedded_list);
        }
        holder.layout.removeAllViews();

        for (T object : objects) {
            addRow(holder.layout, object);
        }

        holder.layout.postInvalidate();
    }

    protected abstract void addRow(ViewGroup layout, T object);

}