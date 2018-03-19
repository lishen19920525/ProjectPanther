package io.panther.demo;

import android.app.Application;

/**
 * Created by LiShen on 2018/3/19.
 * ProjectPanther
 */

public class PantherDemoApplication extends Application {
    private static PantherDemoApplication application;

    @Override
    public void onCreate() {
        application = this;
        super.onCreate();
    }

    public static PantherDemoApplication get() {
        return application;
    }
}