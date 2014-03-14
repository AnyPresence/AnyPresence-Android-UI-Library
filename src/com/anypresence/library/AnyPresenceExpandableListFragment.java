package com.anypresence.library;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;

import com.anypresence.rails_droid.IAPFutureCallback;
import com.anypresence.rails_droid.RemoteObject;

public abstract class AnyPresenceExpandableListFragment<T extends RemoteObject> extends AnyPresenceFragment {
    private ExpandableListView mExpandableListView;
    private AnyPresenceExpandableAdapter<T> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true);
        mExpandableListView = new ExpandableListView(inflater.getContext());
        return mExpandableListView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = createAdapter(new ArrayList<T>());
        getListView().setAdapter(mAdapter);
        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                onItemSelected(mAdapter.getItem(position));
            }
        });

        mAdapter.updateAdapter(loadCache());
        loadServer(new AnyPresenceCallback<List<T>>() {
            @Override
            public void success(List<T> object) {
                mAdapter.updateAdapter(object);
            }
        });
    }

    public ExpandableListView getListView() {
        return mExpandableListView;
    }

    public abstract List<T> loadCache();

    public abstract void loadServer(IAPFutureCallback<List<T>> callback);

    protected abstract AnyPresenceExpandableAdapter<T> createAdapter(List<T> items);

    protected abstract void onItemSelected(T item);
}
