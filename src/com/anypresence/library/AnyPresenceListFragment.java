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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.anypresence.rails_droid.IAPFutureCallback;
import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.sdk.acl.IAuthenticatable;

/**
 * A class that abstracts away loading data into a list fragment.
 * */
public abstract class AnyPresenceListFragment<T extends RemoteObject> extends ListFragment {
    private List<T> mDebugItems;
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
    private View mNoResultsText;
    private View mUnauthorizedText;
    private View mServerUnreachableText;
    private List<String> mRoles;

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

    @SuppressWarnings("unchecked")
	@Override
    public void onStart() {
        super.onStart();

        // Load query
        final Bundle args = getArguments();
        if(args != null) {
            String scope = args.getString("scope");
            HashMap<String, String> params = (HashMap<String, String>) args.getSerializable("params");
            Integer limit = args.getInt("limit", -1) == -1 ? null : args.getInt("limit");
            Integer offset = args.getInt("offset", -1) == -1 ? null : args.getInt("offset");
            setQueryScope(scope, params, limit, offset);
        }
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
     * 
     * Displays an unauthorized message when needed
     * */
    public void notifyDataChanged() {
        if(!mIsActivityCreated) return;
        if(isAuthorized()) {
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
            enableAuthorizedView();
        }
    }
    
    private void disableViews() {
        if(mNoResultsText != null) {
            if(mNoResultsText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mNoResultsText.getParent();
                parent.removeView(mNoResultsText);
            }
        }
        if(mUnauthorizedText != null) {
            if(mUnauthorizedText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mUnauthorizedText.getParent();
                parent.removeView(mUnauthorizedText);
            }
        }
        if(mServerUnreachableText != null) {
            if(mServerUnreachableText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mServerUnreachableText.getParent();
                parent.removeView(mServerUnreachableText);
            }
        }
    }
    
    private void enableAuthorizedView() {
        try {
            disableViews();
            if(mUnauthorizedText == null) {
                mUnauthorizedText = createUnauthorizedView();
            }
            if(mUnauthorizedText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mUnauthorizedText.getParent();
                parent.removeView(mUnauthorizedText);
            }
            ((ViewGroup) getListView().getParent()).addView(mUnauthorizedText);
            getListView().setEmptyView(mUnauthorizedText);
        }
        catch(IllegalStateException e) {}
    }
    
    private void enableEmptyView() {
        try {
            disableViews();
            if(mNoResultsText == null) {
                mNoResultsText = createNoResultsView();
            }
            if(mNoResultsText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mNoResultsText.getParent();
                parent.removeView(mNoResultsText);
            }
            ((ViewGroup) getListView().getParent()).addView(mNoResultsText);
            getListView().setEmptyView(mNoResultsText);
        }
        catch(IllegalStateException e) {}
    }
    
    private void enableServerUnreachableView() {
        try {
            disableViews();
            if(mServerUnreachableText == null) {
                mServerUnreachableText = createServerUnreachableView();
            }
            if(mServerUnreachableText.getParent() != null) {
                ViewGroup parent = (ViewGroup) mServerUnreachableText.getParent();
                parent.removeView(mServerUnreachableText);
            }
            ((ViewGroup) getListView().getParent()).addView(mServerUnreachableText);
            getListView().setEmptyView(mServerUnreachableText);
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
                    mAdapter.updateAdapter(mUnfilteredData);
                }
                else {
                    mAdapter.updateAdapter(applyFilter(mUnfilteredData));
                }
                setListShown(true);

                // Add a "no data" message
                enableEmptyView();

                if(mOnLoadListener != null) mOnLoadListener.onLoad(getList());
            }

            @Override
            public void failure(Throwable ex) {
                super.failure(ex);

                if(!activeQuery.equals(mQuery)) return;

                if(!mDoesCacheExist) {
                    setListShown(true);
                    enableServerUnreachableView();
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
        setQueryScope(new Query(queryScope, params, limit, offset));
    }

    /**
     * Set the query scope. Immediately loads items.
     * */
    public void setQueryScope(Query query) {
        mQuery = query;
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
     * Create a view to be displayed when not authorized.
     * 
     * Override this method to provide a custom view
     * */

    protected View createUnauthorizedView() {
        TextView emptyView = new TextView(getContext());
        emptyView.setText(R.string.ap_unauthorized);
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
            if(mFilter != null) mAdapter.updateAdapter(applyFilter(mUnfilteredData));
            else mAdapter.updateAdapter(mUnfilteredData);
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
    
    /**
     * Set roles that require the user to be authenticated
     * */
    public void setRoles(List<String> roles) {
        mRoles = roles;
        notifyDataChanged();
    }
    
    /**
     * Get roles that require the user to be authenticated
     * */
    public List<String> getRoles() {
        return mRoles;
    }

    private boolean isAuthorized() {
        if(mRoles == null || mRoles.size() == 0) return true;
        IAuthenticatable user = Auth.getCurrentUser();
        if(user == null) return false;
        for(String role : mRoles) {
            if(user.getRoles().containsKey(role)){
                return true;
            }
        }
        return false;
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
     * Create an adapter for the UI of the ListView.
     * */
    protected abstract AnyPresenceAdapter<T> createAdapter(List<T> items);

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

    public static class Query {
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

        public Integer getLimit() {
            return mLimit;
        }

        public Integer getOffset() {
            return mOffset;
        }
    }
}
