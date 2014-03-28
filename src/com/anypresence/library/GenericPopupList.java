package com.anypresence.library;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * Handles generic calls like saving state
 * */
public abstract class GenericPopupList<T extends Serializable> extends TextView {
    // Variables for items, currently selected item
    private T mItem;
    private List<T> mItems;
    private String mPrompt;

    // Listeners
    private OnChangeListener<T> mOnChangeListener;

    // Filtering
    private List<T> mUnfilteredData;
    private Filter<T> mFilter;

    // Sorting
    private Comparator<T> mComparator;

    public GenericPopupList(Context context) {
        super(context);
        setup();
    }

    public GenericPopupList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public GenericPopupList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    private void setup() {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getContext().getResources().getDisplayMetrics());
        setPadding(padding, padding, padding, padding);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = constructPopupMenu(GenericPopupList.this, getItems(), new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        setItem(getItem(item.getTitle().toString()));
                        return true;
                    }
                });
                if(menu != null) {
                    menu.show();
                }
            }
        });
    }

    private PopupMenu constructPopupMenu(View view, List<T> values, OnMenuItemClickListener listener) {
        final PopupMenu popupMenu = new PopupMenu(getContext(), view);
        final Menu menu = popupMenu.getMenu();
        if(values != null) {
            for(T t : values) {
                menu.add(getItemLabel(t));
            }
        }
        popupMenu.setOnMenuItemClickListener(listener);
        return popupMenu;
    }

    public void setPrompt(String prompt) {
        mPrompt = prompt;
        if(mItem == null) setText(mPrompt);
    }

    public String getPrompt() {
        return mPrompt;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState<T> s = new SavedState<T>(superState);
        s.mItem = mItem;
        return s;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        @SuppressWarnings("unchecked")
        SavedState<T> s = (SavedState<T>) state;
        super.onRestoreInstanceState(s.getSuperState());
        setItem(s.mItem);
    }

    /**
     * Set the currently selected item.
     * */
    public void setItem(T item) {
        this.mItem = item;
        if(item == null && mPrompt != null) setText(mPrompt);
        else setText(getItemLabel(item));
        if(mOnChangeListener != null) mOnChangeListener.onChange(item);
    }

    /**
     * Returns the currently selected item. May be null.
     * */
    public T getItem() {
        return mItem;
    }

    /**
     * Returns a list of all possible items.
     * */
    public List<T> getItems() {
        return mItems;
    }

    /**
     * Manually set available items.
     * */
    public void setItems(final List<T> items) {
        // Apply filter
        if(mFilter == null) {
            mItems = items;
        }
        else {
            mItems = applyFilter(mUnfilteredData);
        }

        // Sort list
        if(mComparator != null) Collections.sort(mItems, mComparator);

        if(mPrompt == null) {
            // Choose the first item
            if(mItems.size() > 0) setItem(mItems.get(0));
        }
        else {
            setItem(null);
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
     * Returns the text for that item.
     * */
    protected abstract String getItemLabel(T item);

    private boolean isEqual(String a, T b) {
        return getItemLabel(b).equals(a);
    }

    private T getItem(String name) {
        for(T t : mItems) {
            if(isEqual(name, t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Get the active OnChangeListener
     * */
    public OnChangeListener<T> getOnChangeListener() {
        return mOnChangeListener;
    }

    /**
     * Set an OnChangeListener
     * */
    public void setOnChangeListener(OnChangeListener<T> onChangeListener) {
        this.mOnChangeListener = onChangeListener;
    }

    /**
     * A class for persisting state.
     * */
    private static class SavedState<T extends Serializable> extends BaseSavedState {
        private T mItem;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unchecked")
        private SavedState(Parcel in) {
            super(in);
            mItem = (T) in.readSerializable();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            if(mItem != null) out.writeSerializable(mItem);
        }
    }

    /**
     * A listener for when the user changes the currently selected item.
     * */
    public static interface OnChangeListener<T> {
        public void onChange(T item);
    }

    public static interface Filter<T> {
        public boolean inFilter(T item);
    }
}
