package com.anypresence.library;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.rails_droid.RemoteRequestException;
import com.anypresence.sdk.acl.AuthManagerFactory;
import com.anypresence.sdk.acl.IAuthManager;
import com.anypresence.sdk.acl.IAuthenticatable;
import com.anypresence.sdk.callbacks.APCallback;

public abstract class LoginActivity extends AnyPresenceActivity {

    /**
     * Attempts to log in with the provided username and password. Calls
     * onLoginSuccess or onLoginFailed depending on the result.
     * */
    protected void login(String username, String password) {
        try {
            final IAuthManager manager = AuthManagerFactory.getInstance();
            manager.authenticateUser(username, password, new APCallback<String>() {
                @Override
                public void finished(String result, Throwable ex) {
                    if(ex != null) {
                        ex.printStackTrace();
                        onLoginFailed();
                        return;
                    }

                    IAuthManager manager = AuthManagerFactory.getInstance();
                    IAuthenticatable user = manager.getAuthenticatableObject();
                    onLoginSuccess(user);
                }
            });
        }
        catch(RemoteRequestException e) {
            e.printStackTrace();
            onLoginFailed();
        }
    }

    /**
     * Attempts to log in with the provided User object. Calls onLoginSuccess or
     * onLoginFailed depending on the result.
     * */
    protected void login(RemoteObject user) {
        final IAuthManager manager = AuthManagerFactory.getInstance();
        try {
            manager.authenticateUser(user, new APCallback<String>() {
                @Override
                public void finished(String result, Throwable ex) {
                    if(ex != null) {
                        ex.printStackTrace();
                        onLoginFailed();
                        return;
                    }

                    IAuthManager manager = AuthManagerFactory.getInstance();
                    IAuthenticatable user = manager.getAuthenticatableObject();
                    onLoginSuccess(user);
                }
            });
        }
        catch(RemoteRequestException e) {
            e.printStackTrace();
            onLoginFailed();
        }
    }

    /**
     * Returns whether or not there is a user already logged in.
     * */
    public boolean isLoggedIn() {
        return AuthManagerFactory.getInstance().getIsAuthenticated();
    }

    /**
     * Called on a successful login.
     * */
    public abstract void onLoginSuccess(IAuthenticatable user);

    /**
     * Called on a failed login.
     * */
    public abstract void onLoginFailed();

}
