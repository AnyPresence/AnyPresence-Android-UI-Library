package com.anypresence.library;

import java.lang.reflect.ParameterizedType;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.rails_droid.Utilities;
import com.anypresence.sdk.callbacks.APCallback;

/**
 * A fragment for editing an object. Includes methods for passing an object,
 * updating, and deleting an object. Save and delete buttons are included
 * in the actionbar.
 * */
public abstract class AnyPresenceEditFragment<T extends RemoteObject> extends AnyPresenceItemFragment<T> {
    private T mClone;
    private boolean mIsSaveOnBack;
    private boolean mIsDoingAction = false;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        
        if(savedInstanceState != null) {
            mItem = (T) savedInstanceState.getSerializable(EXTRA_ITEM);
        }
        
        if(mItem == null) {
        	try {
				mItem = getClazz().newInstance();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
        }
        
        try {
            mClone = (T) getItem().getClass().getConstructor().newInstance();
            Utilities.shallowCopyObjects(mClone, getItem());
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	protected Class<T> getClazz() {
        return ((Class<T>)((ParameterizedType) getClass().
				       getGenericSuperclass()).getActualTypeArguments()[0]);
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        updateItem(getItem());
        savedInstanceState.putSerializable(EXTRA_ITEM, getItem());
    }

    /**
     * Called before save. Expectation is that any changes the user made in the
     * form should be applied to the object now.
     * */
    public abstract void updateItem(T item);
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(getItem() != null && getItem().getObjectId() != null) inflater.inflate(R.menu.delete_menu, menu);
        if(!mIsSaveOnBack) inflater.inflate(R.menu.save_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.ap_save) save();
        else if(item.getItemId() == R.id.ap_delete) delete();
        else super.onOptionsItemSelected(item);
        return true;
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
            updateItem(getItem());
            if(getItem().equals(mClone)) {
                finish();
                return;
            }
            getItem().saveInBackground(new APCallback<T>() {
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
    
    private void finish() {
        getActivity().getSupportFragmentManager().popBackStack();
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
}
