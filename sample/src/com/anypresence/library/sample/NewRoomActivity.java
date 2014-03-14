package com.anypresence.library.sample;

import java.util.Date;
import java.util.UUID;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.TextView;

import com.anypresence.library.EditActivity;
import com.anypresence.sdk.open_clove_demo.models.Room;

/**
 * EditActivity requires an object from the AnyPresence SDK. In this case, we're
 * using Room. This Activity will handle saving and deleting (via the
 * ActionBar).
 * */
public class NewRoomActivity extends EditActivity<Room> {

    /**
     * onCreate is where the UI is created in Android. This is called when the
     * activity is first opened, when the phone is rotated, or when the system
     * has cached the app because it's been in the background for a long time.
     * savedInstanceState is null unless the UI has been cached.
     * */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout from /res/layout/activity_new_room.xml
        setContentView(R.layout.activity_new_room);

        // Update the action bar to show the home button in the top left
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * updateItem is specific to EditActivity. It's called right before save().
     * You should update the item with any user changes now.
     * */
    @Override
    public void updateItem() {
        // Set a random group id
        getItem().setGroupId(UUID.randomUUID().toString().replaceAll("-", ""));

        // Set a user-defined name
        getItem().setName(((TextView) findViewById(R.id.name)).getText().toString());

        // Set a user-defined description
        getItem().setDesc(((TextView) findViewById(R.id.description)).getText().toString());

        // Set a user-defined password
        getItem().setPassword(((TextView) findViewById(R.id.password)).getText().toString());

        // Set today's date
        getItem().setDate(new Date());
    }

    /**
     * onOptionsItemSelected is called when an item in the ActionBar has been
     * clicked or, on older phones, when an item from the menu is clicked.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case android.R.id.home:
            // Go home when the home button is clicked
            NavUtils.navigateUpTo(this, new Intent(getContext(), MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
