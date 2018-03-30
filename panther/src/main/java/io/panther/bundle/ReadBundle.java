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

import io.panther.callback.ReadCallback;

/**
 * Created by LiShen on 2017/1/11.
 * Read data bundle
 */

public final class ReadBundle {
    public String key;
    public Object data;
    public Class dataClass;
    public ReadCallback callback;

    public ReadBundle(String key, Class dataClass, ReadCallback callback) {
        this.key = key;
        this.dataClass = dataClass;
        this.callback = callback;
    }
}