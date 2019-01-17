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

package io.panther.memorycache;

import android.text.TextUtils;

import java.util.Set;

/**
 * Created by LiShen on 2017/11/21.
 * Lru cache map
 */

public class PantherMemoryCacheMap {
    private int maxSize;
    private static volatile LruCache<String, Object> cacheMap;

    public PantherMemoryCacheMap(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        cacheMap = new LruCache<>(maxSize);
    }

    public synchronized void put(String key, Object value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        if (value == null) {
            cacheMap.remove(key);
            return;
        }
        cacheMap.put(key, value);
    }

    public synchronized void delete(String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        cacheMap.remove(key);
    }

    public synchronized Object get(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        return cacheMap.get(key);
    }

    public synchronized void clear() {
        cacheMap.evictAll();
    }

    public synchronized int size() {
        return cacheMap.size();
    }

    public synchronized String[] keySet() {
        Set<String> keySet = cacheMap.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }

    @Override
    public String toString() {
        return "=== Panther Memory Cache Map ===\nmax size: " + maxSize + "\n" + cacheMap.toString();
    }
}