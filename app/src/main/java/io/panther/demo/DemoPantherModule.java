package io.panther.demo;

import android.content.Context;

import io.panther.PantherConfiguration;
import io.panther.PantherModule;

/**
 * Created by LiShen on 2018/3/19.
 * ProjectPanther
 */

public class DemoPantherModule implements PantherModule {
    @Override
    public PantherConfiguration applyConfiguration(Context context) {
        return new PantherConfiguration.Builder(context)
                .logEnabled(false)
                .databaseName("PantherDemo")
                .build();
    }
}