package com.anypresence.library;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.List;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.sdk.acl.AuthManager;
import com.anypresence.sdk.acl.IAuthenticatable;
import com.anypresence.sdk.callbacks.APCallback;
import com.anypresence.sdk.config.Config;
import com.anypresence.sdk.http.PersistentCookieStore;

/**
 * Created by will on 3/27/14.
 */
public class Auth {
    private static String SESSION_COOKIE = "_session_id";
    private static boolean LOGGED_IN = false;
    
    /**
     * Detects if a user has an account for the app.
     * If so, checks if there's a user session stored in cookies.
     * If there isn't, uses the Account Manager to grab a new user token (which may prompt the user for a password)
     * */
    public static void setup(Activity activity, String accountType) {
        AccountManager manager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = manager.getAccountsByType(accountType);
        if(accounts.length == 0) {
            LOGGED_IN = false;
            
            // Remove any session id cookies.
            PersistentCookieStore cookieStore = new PersistentCookieStore(activity);
            List<HttpCookie> cookies = cookieStore.get(Config.getInstance().getAppUrl());
            for(HttpCookie c : cookies) {
                if(SESSION_COOKIE.equals(c.getName())) {
                    cookieStore.remove(Config.getInstance().getAppUrl(), c);
                }
            }
        }
        else {
            LOGGED_IN = true;
            
            // Update the auth manager
            AuthManager authManager = new AuthManager.Builder().build();
            authManager.setAuthenticated(true);
            
            // Load the default account
            Account account = accounts[0];
            
            // Override cookies to persist
            CookieHandler.setDefault(new CookieManager(new PersistentCookieStore(activity), CookiePolicy.ACCEPT_ALL));
            
            // Get the auth token from the cookie
            final PersistentCookieStore cookieStore = new PersistentCookieStore(activity);
            String cookieAuthToken = null;
            List<HttpCookie> cookies = cookieStore.get(Config.getInstance().getAppUrl());
            for(HttpCookie c : cookies) {
                if(SESSION_COOKIE.equals(c.getName())) {
                    cookieAuthToken = c.getValue();
                    break;
                }
            }
            
            // Get the auth token from the account manager
            String cachedAuthToken = manager.peekAuthToken(account, SESSION_COOKIE);
            
            // The cookie could have expired, or the active account could have changed. If so, we need to update the token.
            if(cookieAuthToken == null || !cookieAuthToken.equals(cachedAuthToken)) {
                Log.d("Cookie", "Using AccountManager to restore auth token");
                manager.invalidateAuthToken(accountType, cachedAuthToken);
                manager.getAuthToken(account, SESSION_COOKIE, null, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            // Grab the token
                            Bundle bundle = future.getResult();
                            String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            Log.d("Cookie", "Created a new token");
                            
                            // The act of creating a token will update the cookie on its own.
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, null);
            }
        }
    }
    
    /**
     * Attempts to log in with the provided username and password. Calls
     * onLoginSuccess or onLoginFailed depending on the result.
     * */
    public static void login(String username, String password, final OnLoginListener listener) {
        AuthManager manager = new AuthManager.Builder().useAnyPresenceAuth(username, password).build();

        manager.authenticate(new APCallback<IAuthenticatable>() {
            @Override
            public void finished(IAuthenticatable result, Throwable ex) {
                if(ex != null) {
                    ex.printStackTrace();
                    listener.onLoginFailed();
                    return;
                }
                
                // Update login status
                LOGGED_IN = true;
                
                // Call listener
                listener.onLoginSuccess(result);
            }
        });
    }
    
    /**
     * Attempts to log in with the provided User object. Calls onLoginSuccess or
     * onLoginFailed depending on the result.
     * */
    public static void login(RemoteObject user, final OnLoginListener listener) {
        AuthManager manager = new AuthManager.Builder(user.getClass()).useAnyPresenceAuth(user).build();
        manager.authenticate(new APCallback<IAuthenticatable>() {
            @Override
            public void finished(IAuthenticatable result, Throwable ex) {
                if(ex != null) {
                    ex.printStackTrace();
                    if(listener != null) listener.onLoginFailed();
                    return;
                }
                
                // Update login status
                LOGGED_IN = true;
                
                // Call listener
                if(listener != null) listener.onLoginSuccess(result);
            }
        });
    }
    
    /**
     * Attempts to log out of the current account.
     * */
    public static void logout(Context context, String accountType, final OnLogoutListener listener) {
        AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = manager.getAccountsByType(accountType);
        if(accounts.length != 0) {
            // Remove any session id cookies.
            PersistentCookieStore cookieStore = new PersistentCookieStore(context);
            List<HttpCookie> cookies = cookieStore.get(Config.getInstance().getAppUrl());
            for(HttpCookie c : cookies) {
                if(SESSION_COOKIE.equals(c.getName())) {
                    cookieStore.remove(Config.getInstance().getAppUrl(), c);
                }
            }
            
            // Remove the account from the device
            Account account = accounts[0];
            manager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        if (future.getResult()) {
                            Log.d("Auth", "Account removed");
                            listener.onLogoutSuccess();
                            
                            // Clear up the auth manager
                            AuthManager manager = new AuthManager.Builder().build();
                            manager.deauthenticate(new AnyPresenceVoidCallback<Void>() {
                                @Override
                                public void success(Void object) {}

                                @Override
                                public void failure(Throwable ex) {}
                            });
                        } else {
                            listener.onLogoutFailed(new Exception("Failed to remove account from Account Manager"));
                        }
                    } catch (Exception e) {
                        listener.onLogoutFailed(e);
                    }
                }
            }, null);
            
            // Update login status
            LOGGED_IN = false;
        }
    }
    
    /**
     * Manually update Auth's logged in state.
     * */
    public static void setLoggedIn(boolean loggedIn) {
        LOGGED_IN = loggedIn;
    }
    
    /**
     * Returns whether or not there is a user already logged in.
     * */
    public static boolean isLoggedIn() {
        return LOGGED_IN;
    }
    
    public static IAuthenticatable getCurrentUser() {
        return new AuthManager.Builder().build().getUser();
    }
    
    public static boolean isAuthorized(List<String> roles) {
        if(roles == null || roles.size() == 0) return true;
        IAuthenticatable user = Auth.getCurrentUser();
        if(user == null) return false;
        for(String role : roles) {
            if(user.getRoles().containsKey(role)){
                return true;
            }
        }
        return false;
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
    
    public static interface OnLogoutListener {
        /**
         * Called on a successful logout.
         * */
        public void onLogoutSuccess();
        
        /**
         * Called on a failed logout.
         * */
        public void onLogoutFailed(Exception e);
    }
}
