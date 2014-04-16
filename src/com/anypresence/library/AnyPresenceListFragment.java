package com.anypresence.library;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.anypresence.rails_droid.IAPFutureCallback;
import com.anypresence.rails_droid.RemoteObject;

/**
 * A class that abstracts away loading data into a list fragment.
 * */
public abstract class AnyPresenceListFragment<T extends RemoteObject> extends ListFragment {
    private Context mContext;
    private AnyPresenceAdapter<T> mAdapter;
    private Comparator<T> mComparator;
    private Filter<T> mFilter;
    private List<T> mUnfilteredData;
    private boolean mIsActivityCreated = false;
    private boolean mDoesCacheExist;
    private OnLoadListener<T> mOnLoadListener;
    private OnItemSelectedListener<T> mOnItemSelectedListener;
    private Query mQuery = new Query("all");
    private TextView mNoResultsText;

    public AnyPresenceListFragment() {
        super();
        try {
            // Fragments inside fragments can't do this
            setRetainInstance(true);
        }
        catch(IllegalStateException e) {}
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity;
    }

    /**
     * Returns a base context. This context will die when the activity dies, so
     * don't hold on to it.
     * */
    protected Context getContext() {
        return mContext;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = createAdapter(new ArrayList<T>());
        setListAdapter(null);
        getListView().setAdapter(mAdapter);
        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                onItemSelected(mAdapter.getItem(position));
            }
        });
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            getListView().setCacheColorHint(Color.TRANSPARENT);
        }
        mIsActivityCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyDataChanged();
    }

    /**
     * Grab data from the server. Will also grab from cache.
     * */
    public void notifyDataChanged() {
        if(!mIsActivityCreated) return;
        IAPFutureCallback<List<T>> callback = createCallback();
        loadCache(callback);
        loadServer(callback);
    }

    private IAPFutureCallback<List<T>> createCallback() {
        // Keep a reference so older queries won't finish slowly and overwrite a
        // newer one
        final Query activeQuery = mQuery;
        return new AnyPresenceCallback<List<T>>() {
            @Override
            public void success(List<T> object) {
                if(!activeQuery.equals(mQuery)) return;

                mUnfilteredData = object;
                if(mFilter == null) {
                    mAdapter.updateAdapter(object);
                }
                else {
                    List<T> filtered = applyFilter(mUnfilteredData);
                    mAdapter.updateAdapter(filtered);
                }
                setListShown(true);
                if(mComparator != null) Collections.sort(mAdapter.getList(), mComparator);

                // Add a "no data" message. Will crash if listview isn't set up
                // yet.
                try {
                    if(getListView().getEmptyView() == null) {
                        if(mNoResultsText == null) {
                            mNoResultsText = createNoResultsTextView();
                        }
                        if(mNoResultsText.getParent() != null) {
                            ViewGroup parent = (ViewGroup) mNoResultsText.getParent();
                            parent.removeView(mNoResultsText);
                        }
                        ((ViewGroup) getListView().getParent()).addView(mNoResultsText);
                        getListView().setEmptyView(mNoResultsText);
                    }
                }
                catch(IllegalStateException e) {}

                if(mOnLoadListener != null) mOnLoadListener.onLoad(getList());
            }

            @Override
            public void failure(Throwable ex) {
                super.failure(ex);

                if(!activeQuery.equals(mQuery)) return;

                if(!mDoesCacheExist) {
                    setListShown(true);
                    Toast.makeText(getContext(), R.string.ap_loading_failed, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void setListShown(boolean shown) {
        try {
            super.setListShown(shown);
        }
        catch(IllegalStateException e) {}
    }

    /**
     * Returns the current query scope.
     * */
    public String getQueryScope() {
        return mQuery == null ? null : mQuery.getScope();
    }

    /**
     * Set the query scope. Immediately loads items.
     * */
    public void setQueryScope(String queryScope) {
        setQueryScope(queryScope, null);
    }

    /**
     * Set the query scope. Immediately loads items.
     * */
    public void setQueryScope(String queryScope, Map<String, String> params) {
        setQueryScope(queryScope, params, null, null);
    }

    /**
     * Set the query scope. Immediately loads items.
     * */
    public void setQueryScope(String queryScope, Map<String, String> params, Integer limit, Integer offset) {
        mQuery = new Query(queryScope, params, limit, offset);
        if(mAdapter != null) mAdapter.updateAdapter(new ArrayList<T>());
        try {
            getListView().setEmptyView(null);
            if(mNoResultsText != null && mNoResultsText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mNoResultsText.getParent();
                parent.removeView(mNoResultsText);
            }
        }
        catch(IllegalStateException e) {}
        notifyDataChanged();
    }

    /**
     * Load data from cache.
     * */
    @SuppressWarnings("unchecked")
    private void loadCache(IAPFutureCallback<List<T>> callback) {
        mDoesCacheExist = false;
        if(mQuery != null && mQuery.getScope() != null) {
            try {
                Object list;
                if(mQuery.getLimit() != null || mQuery.getOffset() != null) {
                    // Query cache w/ limit and offset
                    Method cacheMethod = getClazz().getMethod("fetchInCacheWithLatestAPCachedRequestPredicate", String.class, Map.class, Integer.class, Integer.class);
                    list = cacheMethod.invoke(null, mQuery.getScope(), mQuery.getParams(), mQuery.getOffset(), mQuery.getLimit());
                }
                else {
                    // Query cache w/o limit and offset
                    Method cacheMethod = getClazz().getMethod("fetchInCacheWithParameterPredicate", String.class, Map.class);
                    list = cacheMethod.invoke(null, mQuery.getScope(), mQuery.getParams());
                }
                if(list != null && ((List<T>) list).size() != 0) {
                    mDoesCacheExist = true;
                    callback.onSuccess((List<T>) list);
                }
                else {
                    setListShown(false);
                }
            }
            catch(NoSuchMethodException e) {
                e.printStackTrace();
            }
            catch(IllegalAccessException e) {
                e.printStackTrace();
            }
            catch(InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load data from server.
     * */
    private void loadServer(IAPFutureCallback<List<T>> callback) {
        if(mQuery != null && mQuery.getScope() != null) {
            try {
                if(mQuery.getParams() == null) {
                    if(mQuery.getLimit() != null || mQuery.getOffset() != null) {
                        // Query cache w/ limit and offset
                        Method m = getClazz().getMethod("queryInBackground", String.class, Integer.class, Integer.class, IAPFutureCallback.class);
                        m.invoke(null, mQuery.getScope(), mQuery.getOffset(), mQuery.getLimit(), callback);
                    }
                    else {
                        // Query cache w/o limit and offset
                        Method m = getClazz().getMethod("queryInBackground", String.class, IAPFutureCallback.class);
                        m.invoke(null, mQuery.getScope(), callback);
                    }
                }
                else {
                    if(mQuery.getLimit() != null || mQuery.getOffset() != null) {
                        // Query cache w/ limit and offset
                        Method m = getClazz().getMethod("queryInBackground", String.class, Map.class, Integer.class, Integer.class, IAPFutureCallback.class);
                        m.invoke(null, mQuery.getScope(), mQuery.getParams(), mQuery.getOffset(), mQuery.getLimit(), callback);
                    }
                    else {
                        // Query cache w/o limit and offset
                        Method m = getClazz().getMethod("queryInBackground", String.class, Map.class, IAPFutureCallback.class);
                        m.invoke(null, mQuery.getScope(), mQuery.getParams(), callback);
                    }
                }
            }
            catch(NoSuchMethodException e) {
                e.printStackTrace();
            }
            catch(IllegalAccessException e) {
                e.printStackTrace();
            }
            catch(InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    protected TextView createNoResultsTextView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_no_data);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        return emptyView;
    }

    /**
     * Returns the current list of items.
     * */
    protected List<T> getList() {
        return mAdapter.getList();
    }

    /**
     * The item has been selected.
     * */
    protected void onItemSelected(T item) {
        if(mOnItemSelectedListener != null) mOnItemSelectedListener.onItemSelected(item);
    }

    /**
     * Call to sort the data.
     * */
    protected void setSort(Comparator<T> comparator) {
        mComparator = comparator;
        if(mAdapter != null && mAdapter.getList() != null && mComparator != null) {
            Collections.sort(mAdapter.getList(), mComparator);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Call to filter the data.
     * */
    public void setFilter(Filter<T> filter) {
        mFilter = filter;
        if(mAdapter != null) {
            if (mFilter != null) mAdapter.updateAdapter(applyFilter(mUnfilteredData));
            else mAdapter.updateAdapter(mUnfilteredData);
        }
    }

    private List<T> applyFilter(List<T> items) {
        List<T> filtered = new ArrayList<T>();
        for(T t : items) {
            if(mFilter.inFilter(t)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Get the active OnLoadListener
     * */
    public OnLoadListener<T> getOnLoadListener() {
        return mOnLoadListener;
    }

    /**
     * Set an OnLoadListener
     * */
    public void setOnLoadListener(OnLoadListener<T> onLoadListener) {
        this.mOnLoadListener = onLoadListener;
    }

    /**
     * Get the active OnItemSelectedListener
     * */
    public OnItemSelectedListener<T> getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    /**
     * Set an OnItemSelectedListener
     * */
    public void setOnItemSelectedListener(OnItemSelectedListener<T> onItemSelectedListener) {
        this.mOnItemSelectedListener = onItemSelectedListener;
    }

    /**
     * Create an adapter for the UI of the ListView.
     * */
    protected abstract AnyPresenceAdapter<T> createAdapter(List<T> items);

    /**
     * Return a copy of the objects class. Used internally for loading from
     * query scopes.
     * */
    protected abstract Class<T> getClazz();

    public static interface Filter<T extends RemoteObject> {
        public boolean inFilter(T item);
    }

    /**
     * A listener for when data is received from the server.
     * */
    public static interface OnLoadListener<T> {
        public void onLoad(List<T> items);
    }

    /**
     * A listener for when an item is clicked.
     * */
    public static interface OnItemSelectedListener<T> {
        public void onItemSelected(T item);
    }

    private static class Query {
        private final String mScope;
        private final Map<String, String> mParams;
        private final Integer mLimit;
        private final Integer mOffset;

        public Query(String scope) {
            mScope = scope;
            mParams = null;
            mLimit = null;
            mOffset = null;
        }

        public Query(String scope, Map<String, String> params) {
            mScope = scope;
            mParams = params;
            mLimit = null;
            mOffset = null;
        }

        public Query(String scope, Map<String, String> params, Integer limit, Integer offset) {
            mScope = scope;
            mParams = params;
            mLimit = limit;
            mOffset = offset;
        }

        public String getScope() {
            return mScope;
        }

        public Map<String, String> getParams() {
            return mParams;
        }

        public Integer getLimit() { return mLimit; }

        public Integer getOffset() { return mOffset; }
    }
}
