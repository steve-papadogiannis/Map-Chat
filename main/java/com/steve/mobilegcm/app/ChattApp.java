package com.steve.mobilegcm.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseUser;

/**
 * The Class ChattApp is the Main Application class of this app. The onCreate
 * method of this class initializes the Parse.
 */
public class ChattApp extends Application {

    // Key for saving the search distance preference
    private static final String KEY_SEARCH_DISTANCE = "searchDistance";

    private static final float DEFAULT_SEARCH_DISTANCE = 150.0f;

    private static SharedPreferences preferences;

	@Override
	public void onCreate() {
		super.onCreate();
		Parse.initialize(this, "", "");
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.saveInBackground();
        preferences = getSharedPreferences("com.steve.mobilegcm", Context.MODE_PRIVATE);
	}

    public static float getSearchDistance() {
        return preferences.getFloat(KEY_SEARCH_DISTANCE, DEFAULT_SEARCH_DISTANCE);
    }

    public static void setSearchDistance( float value ) {
        preferences.edit().putFloat( KEY_SEARCH_DISTANCE, value ).apply();
    }

}
