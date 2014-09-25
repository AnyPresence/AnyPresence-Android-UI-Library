package com.anypresence.library;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

import com.anypresence.rails_droid.RemoteObject;
import com.anypresence.sdk.acl.IAuthenticatable;

public abstract class LoginActivity extends AnyPresenceActivity implements Auth.OnLoginListener {
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;
    private String mUsername;
    private String mPassword;
    
    /**
     * Attempts to log in with the provided username and password. Calls
     * onLoginSuccess or onLoginFailed depending on the result.
     * */
    protected void login(String username, String password) {
        mUsername = username;
        mPassword = password;
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
    
    @Override
    public void onLoginSuccess(IAuthenticatable user) {
        // Save the token to AuthManager so we don't have to log in again later.
        String accountType = getAccountType();
        AccountManager accountManager = AccountManager.get(this);
        Account account = new Account(mUsername, accountType);
        accountManager.addAccountExplicitly(account, mPassword, null);
        
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, stripXSessionId(user));
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private String stripXSessionId(IAuthenticatable user) {
        try {
            return (String) user.getClass().getMethod("getXSessionId").invoke(user);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected String getAccountType() {
        return getPackageName();
    }
    
    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }
    
    /**
     * Retreives the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param savedInstanceState the save instance data of this Activity, may be null
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
    }
    
    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }
}
