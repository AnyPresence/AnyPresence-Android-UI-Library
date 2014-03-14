package com.anypresence.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;

import com.anypresence.rails_droid.RemoteObject;

public abstract class AnyPresenceExpandableAdapter<T extends RemoteObject> implements ExpandableListAdapter {
    public static abstract class Header<T> {
        private final List<T> items = new ArrayList<T>();

        public abstract boolean contains(T item);

        public abstract String getName();
    }

    // Tracks loading of bitmaps (and kills them when no longer needed)
    private final Map<View, BitmapTask> asynctasks = new HashMap<View, BitmapTask>();

    // Keeps a list of headers and items
    private final List<Header<T>> headers;
    private final ArrayList<T> list;

    // Have to manually track observers
    private final List<DataSetObserver> observers = new ArrayList<DataSetObserver>();

    public AnyPresenceExpandableAdapter(Context context, List<Header<T>> headers, List<T> objects) {
        super();

        this.headers = headers;

        if(ArrayList.class.isAssignableFrom(objects.getClass())) {
            list = (ArrayList<T>) objects;
        }
        else {
            list = new ArrayList<T>(objects);
        }

        for(T t : list) {
            for(Header<T> h : headers) {
                if(h.contains(t)) {
                    h.items.add(t);
                    break;
                }
            }
        }
    }

    public ArrayList<T> getList() {
        return list;
    }

    public void notifyDataSetChanged() {
        for(DataSetObserver d : observers) {
            d.onChanged();
        }
    }

    public T getItem(int position) {
        return list.get(position);
    }

    protected void grabImage(View convertView, ImageView iv, String url) {
        // Kill the previous async tasks
        BitmapTask previousTask = asynctasks.get(convertView);
        if(previousTask != null) {
            previousTask.cancel(true);
        }
        iv.setImageDrawable(null);
        BitmapTask newTask = new BitmapTask(iv, url);
        newTask.execute();
        asynctasks.put(convertView, newTask);
    }

    public abstract View inflateView();

    public abstract View inflateHeader();

    public abstract void updateView(View convertView, T object);

    public abstract void updateHeader(View convertView, Header<T> object);

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return headers.get(groupPosition).items.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflateView();
        }
        updateView(convertView, headers.get(groupPosition).items.get(childPosition));

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return headers.get(groupPosition).items.size();
    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return groupId * 10000L + childId;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return groupId * 10000L;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return headers.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflateHeader();
        }
        updateHeader(convertView, headers.get(groupPosition));

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return list == null ? true : list.isEmpty();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {}

    @Override
    public void onGroupExpanded(int groupPosition) {}

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    /**
     * Updates the data in the adapter.
     * */
    public void updateAdapter(List<T> data) {
        // Cache can be null
        if(data == null) return;

        List<T> adapterList = getList();
        adapterList.clear();
        for(T t : data) {
            adapterList.add(t);
        }
        notifyDataSetChanged();
    }
}
