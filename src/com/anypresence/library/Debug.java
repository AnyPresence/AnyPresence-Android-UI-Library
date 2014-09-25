package com.anypresence.library;

/**
 * Created by will on 3/27/14.
 */
public class Debug {
	private static boolean DEBUG_ENABLED = false;

    /**
     * Enable additional logging from AnyPresenceLibrary
     * */
    public static void enableDebug() {
    	DEBUG_ENABLED = true;
    }

    /**
     * Disable logging from AnyPresenceLibrary
     * */
    public static void disableDebug() {
    	DEBUG_ENABLED = false;
    }

    /**
     * Returns if debug is enabled
     * */
    public static boolean isEnabled() {
    	return DEBUG_ENABLED;
    }
}
