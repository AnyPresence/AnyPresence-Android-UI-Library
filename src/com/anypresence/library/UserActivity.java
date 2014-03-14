package com.anypresence.library;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.anypresence.rails_droid.RemoteRequestException;
import com.anypresence.sdk.acl.AuthManagerFactory;
import com.anypresence.sdk.acl.IAuthManager;
import com.anypresence.sdk.acl.IAuthenticatable;
import com.google.common.util.concurrent.FutureCallback;

public abstract class UserActivity extends AnyPresenceActivity {

    /**
     * Copy of the currently logged in user
     * */
    public IAuthenticatable user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IAuthManager manager = AuthManagerFactory.getInstance();
        user = manager.getAuthenticatableObject();
    }

    /**
     * Logs out the user. Callback is called when the server confirms the user
     * has unauthenticated.
     * */
    public void logout(FutureCallback<String> callback) {
        IAuthManager manager = AuthManagerFactory.getInstance();
        try {
            manager.deauthenticateUser(callback);
        }
        catch(RemoteRequestException e) {
            callback.onFailure(e);
        }
    }

    /**
     * Returns whether or not the user is signed in.
     * */
    public boolean isAuthenticated() {
        final IAuthManager manager = AuthManagerFactory.getInstance();
        return manager.getIsAuthenticated();
    }

    /**
     * Returns whether or not there is an internet connection. Requires
     * android.permission.ACCESS_NETWORK_STATE
     * */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    protected IAuthenticatable getUser() {
        return user;
    }
}
