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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.panther.bundle.VisitTimeBundle;

/**
 * Created by LiShen on 2017/11/21.
 * Lru cache map
 */

public class PantherMemoryCacheMap {
    private int maxSize;
    private static volatile PantherArrayMap<String, Object> cacheMap = new PantherArrayMap<>();
    private static volatile List<VisitTimeBundle> visitTimeList = new ArrayList<>();
    private Comparator<VisitTimeBundle> visitTimeComparator;

    public PantherMemoryCacheMap(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        visitTimeComparator = new Comparator<VisitTimeBundle>() {
            @Override
            public int compare(VisitTimeBundle v1, VisitTimeBundle v2) {
                return v1.visitTime.compareTo(v2.visitTime);
            }
        };
    }

    public synchronized void put(String key, Object value) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        if (cacheMap.containsKey(key)) {
            updateVisitTime(key, System.currentTimeMillis());
        } else {
            visitTimeList.add(new VisitTimeBundle(key, System.currentTimeMillis()));
        }
        cacheMap.put(key, value);
        if (cacheMap.size() > maxSize) {
            Collections.sort(visitTimeList, visitTimeComparator);
            delete(visitTimeList.get(0).key);
        }
    }

    public synchronized void delete(String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        cacheMap.remove(key);
        int flag = -1;
        for (int i = 0; i < visitTimeList.size(); i++) {
            if (visitTimeList.get(i).key.equals(key)) {
                flag = i;
            }
        }
        if (flag >= 0) {
            visitTimeList.remove(flag);
        }
    }

    public synchronized Object get(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        updateVisitTime(key, System.currentTimeMillis());
        return cacheMap.get(key);
    }

    public synchronized void clear() {
        cacheMap.clear();
        visitTimeList.clear();
    }

    public synchronized int size() {
        return cacheMap.size();
    }

    public synchronized String[] keySet() {
        Set<String> keySet = cacheMap.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }

    private void updateVisitTime(String key, Long visitTime) {
        for (VisitTimeBundle v : visitTimeList) {
            if (v.key.equals(key)) {
                v.visitTime = visitTime;
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "=== Panther Memory Cache Map ===\nmax size: " + maxSize + "\n" + cacheMap.toString();
    }
}