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

import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by LiShen on 2017/11/21.
 * Lru cache map
 */

@SuppressWarnings("unchecked")
public class PantherMemoryCache {
    private static final String PREFIX_STRONG = "strong:";
    private static final String PREFIX_WEAK = "weak:";

    private volatile LruCache<String, Object> CACHE;

    public PantherMemoryCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        CACHE = new LruCache<>(maxSize);
    }

    public synchronized void put(String key, Object value) {
        put(key, value, false);
    }

    public synchronized void put(String key, Object value, boolean strong) {
        if (strong) {
            CACHE.put(PREFIX_STRONG + key, value);
        } else {
            CACHE.put(PREFIX_WEAK + key, new WeakReference<>(value));
        }
    }

    @Nullable
    public synchronized <T> T get(String key) {
        return get(key, false);
    }

    @Nullable
    public synchronized <T> T get(String key, boolean strong) {
        T value = null;
        if (strong) {
            try {
                value = (T) CACHE.get(PREFIX_STRONG + key);
                if (value instanceof WeakReference) {
                    throw new RuntimeException("Maybe use wrong STRONG type");
                }
            } catch (Exception e) {
                e.printStackTrace();
                value = null;
            }
        } else {
            try {
                WeakReference<Object> valueRef = (WeakReference<Object>) CACHE.get(PREFIX_WEAK + key);
                if (valueRef != null) {
                    value = (T) valueRef.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    public synchronized void delete(String key) {
        CACHE.remove(PREFIX_STRONG + key);
        CACHE.remove(PREFIX_WEAK + key);
    }

    public synchronized void clear() {
        CACHE.evictAll();
    }

    public synchronized int size() {
        return CACHE.size();
    }

    public synchronized String[] keySet() {
        Set<String> keySet = CACHE.snapshot().keySet();
        return keySet.toArray(new String[0]);
    }
}