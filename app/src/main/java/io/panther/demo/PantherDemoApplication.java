package io.panther.demo;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import io.panther.Panther;

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

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }

    public static PantherDemoApplication get() {
        return application;
    }

    public void closeDatabase() {
        Panther.get(this).closeDatabase();
    }
}