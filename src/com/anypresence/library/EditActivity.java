package com.anypresence.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.rails_droid.Utilities;
import com.anypresence.sdk.callbacks.APCallback;

/**
 * An activity for editing an object. Includes methods for passing an object,
 * updating, and deleting an object. Includes several methods for saving,
 * including save on back, save in actionbar, return result (doesn't save to
 * server), and manual saving.
 * */
public abstract class EditActivity<T extends RemoteObject> extends UserActivity {
    /**
     * Extra for the item. Must send an item, even if it's new and empty.
     * */
    public static final String EXTRA_ITEM = "item";

    /**
     * Extra for returning the item as a result. Pass a boolean to (de)activate.
     * Default is false.
     * */
    public static final String EXTRA_FOR_RESULT = "for_result";

    /**
     * Extra for saving when the back button is pressed. Pass a boolean to
     * (de)activate. Default is false.
     * */
    public static final String EXTRA_SAVE_ON_BACK = "save_on_back";

    protected T mItem;
    private T mClone;
    private boolean mIsForResult;
    private boolean mIsSaveOnBack;
    private boolean mIsDoingAction = false;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ITEM)) {
            mItem = (T) savedInstanceState.get(EXTRA_ITEM);
        }
        else {
            Intent intent = getIntent();
            if(intent.getExtras() == null) {
                finish();
                return;
            }
            mItem = (T) intent.getExtras().get(EXTRA_ITEM);
        }

        if(mItem == null) {
            finish();
            return;
        }

        try {
            mClone = (T) mItem.getClass().getConstructor().newInstance();
            Utilities.shallowCopyObjects(mClone, mItem);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        mIsForResult = getIntent().getExtras().getBoolean(EXTRA_FOR_RESULT, false);
        mIsSaveOnBack = getIntent().getExtras().getBoolean(EXTRA_FOR_RESULT, false);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        updateItem();
        savedInstanceState.putSerializable(EXTRA_ITEM, mItem);
    }

    /**
     * Called before save. Expectation is that any changes the user made in the
     * form should be applied to the object now.
     * */
    public abstract void updateItem();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(mItem.getObjectId() != null) inflater.inflate(R.menu.delete_menu, menu);
        if(!mIsSaveOnBack) inflater.inflate(R.menu.save_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.ap_save) save();
        else if(item.getItemId() == R.id.ap_delete) delete();
        else super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mIsSaveOnBack) {
            if(keyCode == KeyEvent.KEYCODE_BACK) {
                save();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Will attempt to save the object to the server. If successful, will close
     * the current activity. In the case of for result, will simply return the
     * object to the previous activity without hitting the server.
     * */
    public void save() {
        if(!saveVerification()) return;
        if(!mIsDoingAction) {
            mIsDoingAction = true;
            updateItem();
            if(mIsForResult) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_ITEM, mItem);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
            else {
                if(mItem.equals(mClone)) {
                    finish();
                    return;
                }
                mItem.saveInBackground(new APCallback<T>() {
                    @Override
                    public void finished(final T arg0, Throwable ex) {
                        mIsDoingAction = false;
                        if(ex != null) {
                            ex.printStackTrace();
                            Toast.makeText(getContext(), R.string.ap_edit_msg_failed, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            if(arg0 != null) {
                                // Using a post, so update item
                                mItem = arg0;
                            }
                            finish();
                        }
                    }
                });
            }
        }
    }

    /**
     * Will attempt to delete the object to the server. If successful, will
     * close the current activity.
     * */
    public void delete() {
        if(!mIsDoingAction) {
            mIsDoingAction = true;
            mItem.deleteInBackground(new APCallback<T>() {
                @Override
                public void finished(final T arg0, Throwable ex) {
                    mIsDoingAction = false;
                    if(ex != null) {
                        ex.printStackTrace();
                        Toast.makeText(getContext(), R.string.ap_edit_msg_failed, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        finish();
                    }
                }
            });
        }
    }

    /**
     * Called before save. Overwrite this method to do local form verification.
     * */
    protected boolean saveVerification() {
        return true;
    }

    /**
     * Returns the current item.
     * */
    public T getItem() {
        return mItem;
    }
}
