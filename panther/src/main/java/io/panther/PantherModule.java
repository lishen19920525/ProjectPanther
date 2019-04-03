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
import android.support.annotation.NonNull;

/**
 * Created by LiShen on 2017/11/28.
 * Panther init module
 * <p>
 * To use this interface:
 * <p>
 * Implement the PantherModule interface in a class with public visibility
 * <p>
 * public class YourPantherModule implements PantherModule {
 * {@literal @}Override
 * public PantherConfiguration applyConfiguration(Context context) {
 * ....
 * }
 * }
 * <p>
 * Add your implementation to your list of keeps in your proguard.cfg file:
 * <p>
 * {@code
 * -keepnames class * com.xxx.xxx.xxx.xxx.YourPantherModule
 * }
 * <p>
 * Add a metadata tag to your AndroidManifest.xml with your PantherModule implementation's fully qualified
 * classname as the value, and {@code io.panther.PantherModule} as the key:
 * <p>
 * <p>
 * {@code
 * <meta-data
 * android:name="io.panther.PantherModule"
 * android:value="com.xxx.xxx.xxx.xxx.YourPantherModule" />
 * }
 */

public interface PantherModule {
    /**
     * Get a configuration for Panther init, do not be null
     *
     * @return configuration
     */
    @NonNull
    PantherConfiguration applyConfiguration(Context context);
}