package com.anypresence.library;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;

import com.anypresence.rails_droid.IAPFutureCallback;
import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.sdk.callbacks.APCallback;

/**
 * An abstract class for loading objects into a spinner. Pass the name of a
 * field as the label (eg: title) and pass a query scope (and optionally query
 * params) to load data.
 * */
public abstract class AnyPresencePopupList<T extends RemoteObject> extends GenericPopupList<T> {
    private String mQueryScope;
    private Map<String, String> mQueryParams;
    private String mLabel;
    private OnLoadListener<T> mOnLoadListener;

    public AnyPresencePopupList(Context context) {
        super(context);
    }

    public AnyPresencePopupList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnyPresencePopupList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Set the currently selected item via object id.
     * */
    public void setItem(String id) {
        try {
            Method m = getClazz().getMethod("fetchInBackground", String.class, IAPFutureCallback.class);
            m.invoke(null, id, new APCallback<T>() {
                @Override
                public void finished(T arg0, Throwable ex) {
                    if(ex == null) {
                        setItem(arg0);
                        loadItems();
                    }
                    else {
                        ex.printStackTrace();
                    }
                }
            });
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

    /**
     * Loads items from query scope and params.
     * */
    @SuppressWarnings("unchecked")
    private void loadItems() {
        try {
            APCallback<List<T>> callback = new APCallback<List<T>>() {
                @Override
                public void finished(final List<T> arg0, Throwable ex) {
                    if(ex != null) {
                        ex.printStackTrace();
                        return;
                    }

                    setItems(arg0);
                    if(mOnLoadListener != null) mOnLoadListener.onLoad(arg0);
                }
            };
            Method cacheMethod = getClazz().getMethod("fetchInCacheWithParameterPredicate", String.class, Map.class);
            Object list = cacheMethod.invoke(null, mQueryScope == null ? "all" : mQueryScope, mQueryParams);
            if(list != null && ((List<T>) list).size() != 0) {
                callback.onSuccess((List<T>) list);
            }
            if(mQueryParams == null) {
                Method m = getClazz().getMethod("queryInBackground", String.class, IAPFutureCallback.class);
                m.invoke(null, mQueryScope == null ? "all" : mQueryScope, callback);
            }
            else {
                Method m = getClazz().getMethod("queryInBackground", String.class, Map.class, Integer.class, Integer.class, IAPFutureCallback.class);
                m.invoke(null, mQueryScope == null ? "all" : mQueryScope, mQueryParams, null, null, callback);
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

    /**
     * Returns the current query scope.
     * */
    public String getQueryScope() {
        return mQueryScope;
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
        this.mQueryScope = queryScope;
        this.mQueryParams = params;
        loadItems();
    }

    /**
     * Set the field used for labels.
     * */
    protected void setLabel(String label) {
        this.mLabel = label;
    }

    /**
     * Return a copy of the objects class. Used internally for loading from
     * query scopes.
     * */
    protected abstract Class<T> getClazz();

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

    @Override
    protected String getItemLabel(T item) {
        if(item == null) return "";
        try {
            Method m = item.getClass().getMethod("get" + Character.toUpperCase(mLabel.charAt(0)) + mLabel.substring(1));
            Object o = m.invoke(item);
            if(o == null) o = "";
            return o.toString();
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
        return null;
    }

    /**
     * A listener for when data is received from the server.
     * */
    public static interface OnLoadListener<T> {
        public void onLoad(List<T> items);
    }
}
