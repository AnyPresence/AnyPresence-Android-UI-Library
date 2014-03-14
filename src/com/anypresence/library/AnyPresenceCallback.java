package com.anypresence.library;

import com.anypresence.sdk.callbacks.APCallback;

/**
 * A simplified callback that only requires action if there was a success.
 * */
public abstract class AnyPresenceCallback<T> extends APCallback<T> {
    @Override
    public void finished(T object, Throwable ex) {
        if(ex != null) {
            failure(ex);
        }
        else if(object != null) {
            success(object);
        }
        else {
            failure(new Throwable("Object was null"));
        }
    }

    /**
     * The callback threw an exception. Fail gracefully.
     * */
    public void failure(Throwable ex) {
        ex.printStackTrace();
    }

    /**
     * The callback got a result. Do something.
     * */
    public abstract void success(T object);
}
