package com.anypresence.library;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.rails_droid.RemoteRequestException;
import com.anypresence.sdk.acl.AuthManagerFactory;
import com.anypresence.sdk.acl.IAuthManager;
import com.anypresence.sdk.acl.IAuthenticatable;
import com.anypresence.sdk.callbacks.APCallback;

/**
 * Created by will on 3/27/14.
 */
public class Auth {
    /**
     * Attempts to log in with the provided username and password. Calls
     * onLoginSuccess or onLoginFailed depending on the result.
     * */
    public static void login(String username, String password, final OnLoginListener listener) {
        try {
            final IAuthManager manager = AuthManagerFactory.getInstance();
            manager.authenticateUser(username, password, new APCallback<String>() {
                @Override
                public void finished(String result, Throwable ex) {
                    if(ex != null) {
                        ex.printStackTrace();
                        listener.onLoginFailed();
                        return;
                    }

                    IAuthManager manager = AuthManagerFactory.getInstance();
                    IAuthenticatable user = manager.getAuthenticatableObject();
                    listener.onLoginSuccess(user);
                }
            });
        }
        catch(RemoteRequestException e) {
            e.printStackTrace();
            listener.onLoginFailed();
        }
    }

    /**
     * Attempts to log in with the provided User object. Calls onLoginSuccess or
     * onLoginFailed depending on the result.
     * */
    public static void login(RemoteObject user, final OnLoginListener listener) {
        final IAuthManager manager = AuthManagerFactory.getInstance();
        try {
            manager.authenticateUser(user, new APCallback<String>() {
                @Override
                public void finished(String result, Throwable ex) {
                    if(ex != null) {
                        ex.printStackTrace();
                        listener.onLoginFailed();
                        return;
                    }

                    IAuthManager manager = AuthManagerFactory.getInstance();
                    IAuthenticatable user = manager.getAuthenticatableObject();
                    listener.onLoginSuccess(user);
                }
            });
        }
        catch(RemoteRequestException e) {
            e.printStackTrace();
            listener.onLoginFailed();
        }
    }

    /**
     * Returns whether or not there is a user already logged in.
     * */
    public static boolean isLoggedIn() {
        return AuthManagerFactory.getInstance().getIsAuthenticated();
    }

    public static interface OnLoginListener {
        /**
         * Called on a successful login.
         * */
        public void onLoginSuccess(IAuthenticatable user);

        /**
         * Called on a failed login.
         * */
        public void onLoginFailed();
    }
}
