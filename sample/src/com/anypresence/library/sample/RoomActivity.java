package com.anypresence.library.sample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.anypresence.library.AnyPresenceItemActivity;
import com.anypresence.sdk.open_clove_demo.models.Room;

public class RoomActivity extends AnyPresenceItemActivity<Room> {
    // Keep track of if the password has been entered
    private boolean mIsAuthenticated = false;

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
        setContentView(R.layout.activity_main);

        // Update the action bar to show the home button in the top left
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Update the action bar to show the Room's name as the title
        getSupportActionBar().setTitle(getItem().getName());
    }

    /**
     * onResume is called a lot. To be more specific, it's called after
     * onCreate, when the screen turns off/on, when the user presses the home
     * button and comes back, and when the user goes to another Activity and
     * presses the back button.
     * */
    @Override
    protected void onResume() {
        super.onResume();

        // Check if they've already logged in or not
        if(!mIsAuthenticated) {
            if(getItem().getPassword() == null || getItem().getPassword().isEmpty()) {
                // No password, let them pass
                mIsAuthenticated = true;
            }
            else {
                // Prompt the user to log in (assuming there is a password)
                requestPasswordPopup();
            }
        }
    }

    /**
     * Creates a lil popup and checks it against the password on the Room.
     * */
    private void requestPasswordPopup() {
        final EditText passwordBox = new EditText(getContext());
        passwordBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(passwordBox);
        builder.setTitle("Enter Password");
        builder.setPositiveButton("Ok", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(getItem().getPassword().equals(passwordBox.getText().toString())) {
                    mIsAuthenticated = true;
                }
                else {
                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    requestPasswordPopup();
                }
            }
        });
        builder.setNegativeButton("Cancel", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    /**
     * Manages the home button in the top left
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpTo(this, new Intent(getContext(), MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
