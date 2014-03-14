package com.anypresence.library.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.anypresence.APSetup;
import com.anypresence.library.AnyPresenceActivity;
import com.anypresence.sdk.config.Config;
import com.anypresence.sdk.open_clove_demo.models.Room;

public class MainActivity extends AnyPresenceActivity {
    // Keeps track of setting up the cache db. Should only be called once per
    // session, thus the static variable.
    static boolean ORM_SETUP = false;

    // Easy boolean to switch into debug mode.
    protected static final boolean DEBUG = true;

    /**
     * onCreate is where the UI is created in Android. This is called when the
     * activity is first opened, when the phone is rotated, or when the system
     * has cached the app because it's been in the background for a long time.
     * savedInstanceState is null unless the UI has been cached.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout from /res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        // Run the setup code for the AnyPresence SDK. The SDK won't work until
        // this has been called.
        if(!ORM_SETUP) {

            // Sets up the cache database.
            APSetup.setupOrm(getContext());
            ORM_SETUP = true;

            // Sets up internal variables.
            APSetup.setup();

            // Update the server and version number. Has to be run after setup()
            // because that also sets the server/version.
            String server = "https://calm-garden-7313.herokuapp.com";
            String version = "/api/v2";
            Config.getInstance().setAppUrl(server + version);
            Config.getInstance().setAuthUrl(server + "/auth/password/callback");
            Config.getInstance().setDeauthUrl(server + "/auth/signout");
            Config.getInstance().setStrictQueryFieldCheck(false);

            // Additional logging for debug mode.
            if(DEBUG) {
                APSetup.enableDebugMode();
                System.out.println(Config.getInstance().getAppUrl());
                System.out.println(Config.getInstance().getAuthUrl());
                System.out.println(Config.getInstance().getDeauthUrl());
            }
        }

        if(savedInstanceState == null) {
            // Creates and starts the fragment for the List
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, new RoomListFragment()).commit();
        }
    }

    /**
     * onOptionsItemSelected is called when an item in the ActionBar has been
     * clicked or, on older phones, when an item from the menu is clicked.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Iterate over the items, returning true if we handle the action.
        switch(item.getItemId()) {
        case R.id.add:
            // Add has been clicked.

            // Create a new intent that points to the NewRoomActivity
            Intent intent = new Intent(getContext(), NewRoomActivity.class);

            // Pass a new Room object
            intent.putExtra(NewRoomActivity.EXTRA_ITEM, new Room());

            // Launch the activity
            startActivity(intent);

            // Return true because we handled the action
            return true;
        default:
            // Do the default action
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * onCreateOptionsMenu is called when loading items for the ActionBar or, on
     * older phones, when the menu button has been clicked.
     * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Adds the "Add" button to the ActionBar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Return true because we have buttons
        return true;
    }
}
