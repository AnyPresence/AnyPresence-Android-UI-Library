package com.anypresence.library;

import com.anypresence.sdk.callbacks.APCallback;

/**
 * A simplified callback
 * */
public abstract class AnyPresenceVoidCallback<T> extends APCallback<T> {
    @Override
    public void finished(T object, Throwable ex) {
        if(ex != null) {
            failure(ex);
        } else {
            success(object);
        }
    }

    public abstract void success(T object);

    public abstract void failure(Throwable ex);

}
