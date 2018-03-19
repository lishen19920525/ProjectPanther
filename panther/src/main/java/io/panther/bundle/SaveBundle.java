/*
 * Copyright 2015 Glow Geniuses Studio
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

package io.panther.bundle;

import io.panther.callback.SaveCallback;

/**
 * Created by LiShen on 2017/1/11.
 * Save data bundle
 */

public final class SaveBundle {
    public String key;
    public boolean success;
    public Object data;
    public SaveCallback callback;

    public SaveBundle(String key, Object data, SaveCallback callback) {
        this.key = key;
        this.data = data;
        this.callback = callback;
    }
}