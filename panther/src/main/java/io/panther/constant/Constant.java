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

package io.panther.constant;

/**
 * Created by LiShen on 2017/11/20.
 * Constants
 */

public class Constant {
    public static final int DEFAULT_MEMORY_CACHE_SIZE = 128;

    public static final int MSG_MAIN_SAVE_CALLBACK = 1;
    public static final int MSG_MAIN_READ_CALLBACK = 2;
    public static final int MSG_MAIN_DELETE_CALLBACK = 3;
    public static final int MSG_MAIN_MASS_DELETE_CALLBACK = 4;
    public static final int MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK = 5;

    public static final int MSG_WORK_SAVE = -1;
    public static final int MSG_WORK_READ = -2;
    public static final int MSG_WORK_DELETE = -4;
    public static final int MSG_WORK_MASS_DELETE = -5;
    public static final int MSG_WORK_FIND_KEYS_BY_PREFIX = -6;

    public static final int MSG_SUBTYPE_NORMAL = 0;
    public static final int MSG_SUBTYPE_READ_ARRAY = 1;

    public static final String PANTHER_MODULE_NAME = "io.panther.PantherModule";

    public static final String SAVE_KEY_PREFIX = "save:";
    public static final String READ_KEY_PREFIX = "read:";
    public static final String DELETE_KEY_PREFIX = "delete:";
    public static final String MASS_DELETE_KEY_PREFIX = "mass_delete:";
    public static final String FIND_KEYS_BY_PREFIX_KEY_PREFIX = "find_keys_by_prefix:";
}