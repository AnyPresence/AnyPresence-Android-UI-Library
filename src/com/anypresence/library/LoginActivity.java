package com.anypresence.library;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.rails_droid.RemoteRequestException;
import com.anypresence.sdk.acl.AuthManagerFactory;
import com.anypresence.sdk.acl.IAuthManager;
import com.anypresence.sdk.acl.IAuthenticatable;
import com.anypresence.sdk.callbacks.APCallback;

public abstract class LoginActivity extends AnyPresenceActivity implements Auth.OnLoginListener {

    /**
     * Attempts to log in with the provided username and password. Calls
     * onLoginSuccess or onLoginFailed depending on the result.
     * */
    protected void login(String username, String password) {
        Auth.login(username, password, this);
    }

    /**
     * Attempts to log in with the provided User object. Calls onLoginSuccess or
     * onLoginFailed depending on the result.
     * */
    protected void login(RemoteObject user) {
        Auth.login(user, this);
    }

    /**
     * Returns whether or not there is a user already logged in.
     * */
    public boolean isLoggedIn() {
        return Auth.isLoggedIn();
    }
}
