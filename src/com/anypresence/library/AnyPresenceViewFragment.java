package com.anypresence.library;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.anypresence.rails_droid.IAPFutureCallback;
import com.anypresence.rails_droid.RemoteObject;

/**
 * A class that abstracts away loading data into a fragment.
 * */
public abstract class AnyPresenceViewFragment<T extends RemoteObject> extends AnyPresenceRoleFragment {
    private List<T> mDebugItems;
    private List<T> mItems;
    private Comparator<T> mComparator;
    private Filter<T> mFilter;
    private List<T> mUnfilteredData;
    private boolean mDoesCacheExist;
    private OnLoadListener<T> mOnLoadListener;
    private OnItemSelectedListener<T> mOnItemSelectedListener;
    private Query mQuery = new Query("all");
    private View mNoResultsText;
    private View mServerUnreachableText;

    @SuppressWarnings("unchecked")
	@Override
    public void onStart() {
        super.onStart();

        // Load query
        final Bundle args = getArguments();
        if(args != null && args.containsKey("scope")) {
            String scope = args.getString("scope");
            HashMap<String, String> params = args.containsKey("params") ? (HashMap<String, String>) args.getSerializable("params") : null;
            Integer limit = args.containsKey("limit") ? args.getInt("limit") : null;
            Integer offset = args.containsKey("offset") ? args.getInt("offset") : null;
            setQueryScope(scope, params, limit, offset);
        }
    }

    /**
     * Grab data from the server. Will also grab from cache.
     * 
     * Displays an unauthorized message when needed
     * */
    @Override
    public void notifyDataChanged() {
        if(!mIsActivityCreated) return;
        if(Auth.isAuthorized(mRoles)) {
            // User is authorized to see page
            IAPFutureCallback<List<T>> callback = createCallback();
            if(Debug.isEnabled() && mDebugItems != null) {
            	callback.onSuccess(mDebugItems);
            }
            else {
                loadCache(callback);
                loadServer(callback);
            }
        }
        else {
            // User is not authorized to see page
            enableUnauthorizedView();
        }
    }
    
    @Override
    void disableViews() {
        super.disableViews();
        mNoResultsText.setVisibility(View.GONE);
        mServerUnreachableText.setVisibility(View.GONE);
    }

    protected void enableEmptyView() {
        try {
            disableViews();
            mNoResultsText.setVisibility(View.VISIBLE);
        }
        catch(IllegalStateException e) {}
    }

    protected void enableServerUnreachableView() {
        try {
            disableViews();
            mServerUnreachableText.setVisibility(View.VISIBLE);
        }
        catch(IllegalStateException e) {}
    }

    private IAPFutureCallback<List<T>> createCallback() {
        // Keep a reference so older queries won't finish slowly and overwrite a
        // newer one
        final Query activeQuery = mQuery;
        return new AnyPresenceCallback<List<T>>() {
            @Override
            public void success(List<T> object) {
                if(!activeQuery.equals(mQuery)) return;

                mUnfilteredData = new ArrayList<T>(object);
                if(mComparator != null) Collections.sort(mUnfilteredData, mComparator);
                if(mFilter == null) {
                    setItems(mUnfilteredData);
                }
                else {
                    setItems(applyFilter(mUnfilteredData));
                }
                setViewShown(true);

                if(mItems.isEmpty()) {
                	// Add a "no data" message
                	enableEmptyView();
                }
                else {
                	enableAuthorizedView();
                }
                update(mItems);

                if(mOnLoadListener != null) mOnLoadListener.onLoad(getList());
            }

            @Override
            public void failure(Throwable ex) {
                super.failure(ex);

                if(!activeQuery.equals(mQuery)) return;

                if(!mDoesCacheExist) {
                    setViewShown(true);
                    enableServerUnreachableView();
                }
            }
        };
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
        setQueryScope(new Query(queryScope, params, limit, offset));
    }

    /**
     * Set the query scope. Immediately loads items.
     * */
    public void setQueryScope(Query query) {
        mQuery = query;
        setItems(new ArrayList<T>());
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
                    Method cacheMethod = getClazz().getMethod("fetchInCacheWithLatestAPCachedRequestPredicate", String.class, Map.class, Integer.class,
                            Integer.class);
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
                    setViewShown(false);
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

    /**
     * Create a view to be displayed when no data is found.
     * 
     * Override this method to provide a custom view
     * */
    protected View createNoResultsView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_no_data);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        return emptyView;
    }

    /**
     * Create a view to be displayed when server isn't reachable.
     * 
     * Override this method to provide a custom view
     * */

    protected View createServerUnreachableView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_loading_failed);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        emptyView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyView.setGravity(Gravity.CENTER);
        return emptyView;
    }

    /**
     * Returns the current list of items.
     * */
    protected List<T> getList() {
        return mItems;
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
        if(getList() != null && mComparator != null) {
            Collections.sort(getList(), mComparator);
            notifyDataChanged();
        }
    }

    /**
     * Call to filter the data.
     * */
    public void setFilter(Filter<T> filter) {
        mFilter = filter;
        if(getList() != null) {
            if(mFilter != null) setItems(applyFilter(mUnfilteredData));
            else setItems(mUnfilteredData);
        }
    }

    private List<T> applyFilter(List<T> items) {
        List<T> filtered = new ArrayList<T>();
        if(items != null) {
            for (T t : items) {
                if (mFilter.inFilter(t)) {
                    filtered.add(t);
                }
            }
        }
        return filtered;
    }
    
    private void setItems(List<T> items) {
    	mItems = items;
    }

    private static final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;

	@Override
	protected View inflateView(Bundle savedInstanceState) {
		View root = super.inflateView(savedInstanceState);

	    mNoResultsText = createNoResultsView();
	    mNoResultsText.setVisibility(View.GONE);
		mRoot.addView(mNoResultsText, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		mServerUnreachableText = createServerUnreachableView();
		mServerUnreachableText.setVisibility(View.GONE);
		mRoot.addView(mServerUnreachableText, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		return root;
	}
    
    /**
     * Set hard-coded items to show in the list.
     * Debug.enableDebug() must be called for the items to show up.
     * If debug items are null, the ListFragment will work normally.
     * */
    protected void setDebugItems(List<T> items) {
    	mDebugItems = items;
    	notifyDataChanged();
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
     * Update the UI with the supplied items
     * */
    protected abstract void update(List<T> items);

    /**
     * Return a copy of the objects class. Used internally for loading from
     * query scopes.
     * */
    @SuppressWarnings("unchecked")
	protected Class<T> getClazz() {
        return ((Class<T>)((ParameterizedType) getClass().
				       getGenericSuperclass()).getActualTypeArguments()[0]);
    }

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
}
