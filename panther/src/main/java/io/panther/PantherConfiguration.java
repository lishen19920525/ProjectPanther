/*
 * Copyright 2018 LISHEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.panther;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;

import io.panther.constant.Constant;

/**
 * Created by LiShen on 2017/11/21.
 * Panther init configuration
 */

public class PantherConfiguration {
    Context context;
    String databaseName;
    File databaseFolder;
    int memoryCacheSize;
    boolean logEnabled;

    private PantherConfiguration(Builder builder) {
        context = builder.context;
        databaseName = builder.databaseName;
        databaseFolder = builder.databaseFolder;
        memoryCacheSize = builder.memoryCacheSize;
        logEnabled = builder.logEnabled;

        // application context
        if (context == null) {
            throw new NullPointerException("Context can not be null!");
        }
        // default database name
        if (TextUtils.isEmpty(databaseName)) {
            databaseName = context.getPackageName();
        }
        // database folder
        boolean databaseFolderValid = false;
        if (databaseFolder != null) {
            boolean databaseFolderExist = false;
            try {
                databaseFolderExist = databaseFolder.exists();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!databaseFolderExist) {
                try {
                    databaseFolderValid = databaseFolder.mkdir();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                databaseFolderValid = true;
            }
        }
        if (!databaseFolderValid) {
            databaseFolder = context.getFilesDir();
        }
        if (databaseFolder == null) {
            throw new NullPointerException("Database folder can not be null!");
        }
        // memory cache max size
        if (memoryCacheSize <= 0) {
            memoryCacheSize = Constant.DEFAULT_MEMORY_CACHE_SIZE;
        }
    }

    public static final class Builder {
        private Context context;
        private String databaseName;
        private File databaseFolder;
        private int memoryCacheSize;
        private boolean logEnabled;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        /**
         * Database name
         *
         * @param val name
         * @return
         */
        public Builder databaseName(String val) {
            databaseName = val;
            return this;
        }

        /**
         * Database will in this folder
         *
         * @param val folder
         * @return
         */
        public Builder databaseFolder(File val) {
            databaseFolder = val;
            return this;
        }

        /**
         * Memory cache max size
         *
         * @param val size
         * @return
         */
        public Builder memoryCacheSize(int val) {
            memoryCacheSize = val;
            return this;
        }

        /**
         * Log
         *
         * @param val enabled
         * @return
         */
        public Builder logEnabled(boolean val) {
            logEnabled = val;
            return this;
        }

        public PantherConfiguration build() {
            return new PantherConfiguration(this);
        }
    }
}