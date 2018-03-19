package io.panther;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Created by LiShen on 2017/11/29.
 * NetworkConfiguration parser
 */

public class ConfigurationParser {
    private final Context context;

    public ConfigurationParser(Context context) {
        this.context = context;
    }

    public PantherConfiguration parse() {
        PantherConfiguration configuration = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo != null && appInfo.metaData != null) {
                String className = appInfo.metaData.getString(Constant.PANTHER_MODULE_NAME, "");
                configuration = parseModule(className).applyConfiguration(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (configuration == null) {
            configuration = new PantherConfiguration.Builder(context).build();
        }
        return configuration;
    }

    private static PantherModule parseModule(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find PantherModule implementation", e);
        }
        Object module;
        try {
            module = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate PantherModule implementation for " + clazz, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate PantherModule implementation for " + clazz, e);
        }
        if (!(module instanceof PantherModule)) {
            throw new RuntimeException("Expected instanceof PantherModule, but found: " + module);
        }
        return (PantherModule) module;
    }
}