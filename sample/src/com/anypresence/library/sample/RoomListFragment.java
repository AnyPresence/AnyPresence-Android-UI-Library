package com.anypresence.library.sample;

import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.anypresence.library.AnyPresenceAdapter;
import com.anypresence.library.AnyPresenceListFragment;
import com.anypresence.sdk.open_clove_demo.models.Room;

/**
 * This list fragment handles loading data from cache and the server. It
 * defaults to All, but calling setQueryScope can change the scope.
 * */
public class RoomListFragment extends AnyPresenceListFragment<Room> {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setSort(new Comparator<Room>() {
            @Override
            public int compare(Room lhs, Room rhs) {
                if(lhs.getDate() == null && rhs.getDate() == null) return 0;
                if(rhs.getDate() == null) return 1;
                if(lhs.getDate() == null) return 1;
                if(lhs.getDate().before(rhs.getDate())) return 1;
                if(lhs.getDate().after(rhs.getDate())) return -1;
                return 0;
            }
        });
    }

    /**
     * Just return a copy of our adapter here
     * */
    @Override
    protected AnyPresenceAdapter<Room> createAdapter(List<Room> items) {
        return new InfoAdapter(getContext(), items);
    }

    /**
     * Overwrite this to do stuff when a list item is clicked
     * */
    @Override
    protected void onItemSelected(Room item) {
        // Open a room activity
        Intent intent = new Intent(getContext(), RoomActivity.class);
        intent.putExtra(RoomActivity.EXTRA_ITEM, item);
        startActivity(intent);
    }

    /**
     * An adapter for Room. This creates the UI for the list items.
     * */
    public class InfoAdapter extends AnyPresenceAdapter<Room> {
        public InfoAdapter(Context context, List<Room> objects) {
            super(context, objects);
        }

        /**
         * This is called when an item in the list has to be updated
         * */
        @Override
        public void updateView(View convertView, Room object) {
            // Grab and update the title
            TextView tv = (TextView) convertView.findViewById(R.id.title);
            tv.setText(object.getName());

            // Show the lock icon if the password is not null/empty
            ImageView iv = (ImageView) convertView.findViewById(R.id.locked);
            if(object.getPassword() == null || object.getPassword().isEmpty()) {
                iv.setVisibility(View.GONE);
            }
            else {
                iv.setVisibility(View.VISIBLE);
            }

            // If you want to use an image from a url instead, call loadImage().
            // It'll handle caching, loading the image, and halting the
            // connection if the user scrolls away.
        }

        /**
         * Pass a copy of the list item view here. This'll probably be called a
         * few times, once for each item visible on the screen.
         * */
        @Override
        public View inflateView() {
            return View.inflate(getContext(), R.layout.list_item_room, null);
        }
    }

    /**
     * Used so that the library can call queryInBackground and other such calls.
     * */
    @Override
    protected Class<Room> getClazz() {
        return Room.class;
    }
}
