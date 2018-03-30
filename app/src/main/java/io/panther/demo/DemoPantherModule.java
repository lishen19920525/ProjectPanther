package io.panther.demo;

import android.content.Context;
import android.os.Build;

import java.io.File;

import io.panther.PantherConfiguration;
import io.panther.PantherModule;

/**
 * Created by LiShen on 2018/3/19.
 * ProjectPanther
 */

public class DemoPantherModule implements PantherModule {
    @Override
    public PantherConfiguration applyConfiguration(Context context) {
        File databaseFolder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            databaseFolder = context.getExternalFilesDirs(null)[0];
        } else {
            databaseFolder = context.getExternalFilesDir(null);
        }
        if (databaseFolder != null && databaseFolder.exists()) {
            String path = databaseFolder.getAbsolutePath();
            if (!path.endsWith(File.separator)) {
                path = path + File.separator;
            }
            path = path + "database/";
            databaseFolder = new File(path);
        }
        return new PantherConfiguration.Builder(context)
                .databaseFolder(databaseFolder)
                .logEnabled(true)
                .databaseName("PantherDemo")
                .build();
    }
}