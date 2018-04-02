package io.panther.bundle;

import io.panther.callback.ReadArrayCallback;

/**
 * Created by LiShen on 2018/4/2.
 * ProjectPanther
 */

public final class ReadArrayBundle<T> extends BaseBundle {
    public String key;
    public T[] data;
    public Class<T> dataClass;
    public ReadArrayCallback callback;

    public ReadArrayBundle(String key, Class<T> dataClass, ReadArrayCallback callback) {
        this.key = key;
        this.dataClass = dataClass;
        this.callback = callback;
    }
}