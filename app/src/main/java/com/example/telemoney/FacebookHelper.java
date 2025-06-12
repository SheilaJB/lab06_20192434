package com.example.telemoney;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class FacebookHelper extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }
}
