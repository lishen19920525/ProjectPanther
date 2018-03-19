package io.panther.bundle;

import io.panther.callback.FindKeysCallback;


/**
 * Created by LiShen on 2017/11/30.
 */

public class FindKeysByPrefixBundle {
    public String prefix;
    public String[] keys;
    public FindKeysCallback callback;

    public FindKeysByPrefixBundle(String prefix, FindKeysCallback callback) {
        this.prefix = prefix;
        this.callback = callback;
    }
}