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

package io.panther;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.lang.ref.WeakReference;

import io.panther.bundle.BaseBundle;
import io.panther.bundle.DeleteBundle;
import io.panther.bundle.FindKeysByPrefixBundle;
import io.panther.bundle.MassDeleteBundle;
import io.panther.bundle.ReadBundle;
import io.panther.bundle.SaveBundle;
import io.panther.callback.DeleteCallback;
import io.panther.callback.FindKeysCallback;
import io.panther.callback.MassDeleteCallback;
import io.panther.callback.ReadCallback;
import io.panther.callback.SaveCallback;
import io.panther.constant.Constant;
import io.panther.memorycache.PantherArrayMap;
import io.panther.memorycache.PantherMemoryCacheMap;
import io.panther.util.ConfigurationParser;
import io.panther.util.GZIP;
import io.panther.util.JSON;

/**
 * Created by LiShen on 2016/7/8.
 * Panther
 */
@SuppressWarnings("unchecked")
public final class Panther {
    private static volatile Panther panther;

    private PantherConfiguration configuration;

    private static volatile DB database;
    private static volatile PantherArrayMap<String, BaseBundle> dataMedium;

    private PantherMemoryCacheMap memoryCacheMap;

    private MainHandler mainHandler;
    private HandlerThread workThread;
    private WorkHandler workHandler;

    private Panther(PantherConfiguration configuration) {
        // configuration
        this.configuration = configuration;
        log("========== Panther configuration =========="
                + "\ndatabase folder: " + configuration.databaseFolder.getAbsolutePath()
                + "\ndatabase name: " + configuration.databaseName
                + "\nmemory cache size: " + configuration.memoryCacheSize
                + "\n===========================================");
        // memory cache
        memoryCacheMap = new PantherMemoryCacheMap(configuration.memoryCacheSize);
        // open database when PANTHER init
        openDatabase();
    }

    /**
     * Get a PANTHER single instance
     *
     * @param context context
     * @return panther
     */
    public static Panther get(Context context) {
        if (panther == null) {
            synchronized (Panther.class) {
                if (panther == null) {
                    ConfigurationParser parser = new ConfigurationParser(context.getApplicationContext());
                    panther = new Panther(parser.parse());
                }
            }
        }
        return panther;
    }

    /**
     * Close the database
     */
    public void closeDatabase() {
        if (database != null) {
            try {
                synchronized (database) {
                    database.close();
                }
            } catch (Exception e) {
                logError("close database failed", e);
            }
        }
    }

    /**
     * Open database
     */
    public void openDatabase() {
        try {
            if (database != null && database.isOpen()) {
                log("database: " + configuration.databaseName + " already open, no need to open again");
                return;
            }
            // read and save cache
            synchronized (this) {
                dataMedium = new PantherArrayMap();
            }
            synchronized (DB.class) {
                database = DBFactory.open(configuration.databaseFolder.getAbsolutePath(),
                        configuration.databaseName);
            }
            log("database: " + configuration.databaseName + " open success");
        } catch (Exception e) {
            logError("database: " + configuration.databaseName + " open failed", e);
        }
    }

    /**
     * Whether database available
     *
     * @return available
     */
    private boolean checkDatabaseAvailable() {
        boolean available = false;
        try {
            if (database != null && database.isOpen()) {
                available = true;
            }
        } catch (Exception ignore) {
        }
        return available;
    }

    /**
     * Check the key and database before the database operation
     *
     * @param key key
     * @throws Exception exception
     */
    private void databaseOperationPreCheck(String key) throws Exception {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("KEY or PREFIX can not be null !");
        }
        if (!checkDatabaseAvailable()) {
            throw new IllegalStateException("DATABASE is unavailable");
        }
    }

    /**
     * Save in database synchronously, core method
     * Not recommended to call for storing large data in the main thread
     * Large data use {@link #writeInDatabaseAsync(String, Object, SaveCallback)}
     *
     * @param key  key
     * @param data data
     * @return result
     */
    public boolean writeInDatabase(String key, Object data) {
        try {
            databaseOperationPreCheck(key);
            // to Json string
            String dataJson = JSON.toJSONString(data);
            // compress
            String dataBundleJsonCompressed = GZIP.compress(dataJson);
            synchronized (database) {
                database.put(key, dataBundleJsonCompressed);
            }
            log("key = " + key + " value = " + dataJson + " saved in database finished");
            return true;
        } catch (Exception e) {
            logError("key = " + key + " value = " + String.valueOf(data) + " save in database failed", e);
            return false;
        }
    }

    /**
     * Save in database asynchronously
     *
     * @param key      key
     * @param data     data
     * @param callback callback
     */
    public void writeInDatabaseAsync(String key, Object data, SaveCallback callback) {
        SaveBundle saveBundle = new SaveBundle(key, data, callback);
        dataMedium.put(Constant.SAVE_KEY_PREFIX + key, saveBundle);
        callOnWorkHandler(Constant.MSG_WORK_SAVE, key);
    }


    /**
     * Read from database synchronously, core method.
     * Not recommended to call for read large data in the main thread.
     * Read large data use {@link #readFromDatabaseAsync(String, Class, ReadCallback)}
     *
     * @param key       key
     * @param dataClass class of data
     * @return data
     */
    public <T> Object readFromDatabase(String key, Class<T> dataClass) {
        String dataJson = null;
        T data = null;
        try {
            databaseOperationPreCheck(key);
            // read data json string compressed
            synchronized (database) {
                dataJson = database.get(key);
            }
        } catch (Exception e) {
            logError("read { key = " + key + " } from database failed", e);
        }
        if (!TextUtils.isEmpty(dataJson)) {
            // decompress
            dataJson = GZIP.decompress(dataJson);
            // data parse
            if (dataClass == String.class) {
                // String.class
                data = (T) dataJson;
            } else {
                if (dataJson.startsWith(Constant.JSON_ARRAY_PREFIX)) {
                    // parse to array
                    try {
                        data = (T) JSON.parseArray(dataJson, dataClass);
                    } catch (Exception e) {
                        logError("read { key = " + key + " } from database parse failed", e);
                    }
                } else {
                    // parse to object
                    try {
                        data = JSON.parseObject(dataJson, dataClass);
                    } catch (Exception e) {
                        logError("read { key = " + key + " } from database parse failed", e);
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(dataJson) && data != null) {
            log("key = " + key + " value = " + dataJson + " read from database finished");
        }
        return data;
    }

    /**
     * Read data from database asynchronously
     *
     * @param key       key
     * @param dataClass class of data
     * @param callback  callback
     */
    public void readFromDatabaseAsync(String key, Class dataClass, ReadCallback callback) {
        ReadBundle readBundle = new ReadBundle(key, dataClass, callback);
        dataMedium.put(Constant.READ_KEY_PREFIX + key, readBundle);
        callOnWorkHandler(Constant.MSG_WORK_READ, key);
    }

    /**
     * Read String from database synchronously
     *
     * @param key key
     * @return data
     */
    public String readStringFromDatabase(String key) {
        String data = (String) readFromDatabase(key, String.class);
        if (data == null) {
            data = "";
        }
        return data;
    }

    /**
     * Read String from database synchronously
     *
     * @param key          key
     * @param defaultValue default value
     * @return data
     */
    public String readStringFromDatabase(String key, String defaultValue) {
        String data = (String) readFromDatabase(key, String.class);
        if (TextUtils.isEmpty(data)) {
            data = defaultValue;
        }
        return data;
    }

    /**
     * Read Integer from database synchronously
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return data
     */
    public int readIntFromDatabase(String key, int defaultValue) {
        Integer data = (Integer) readFromDatabase(key, Integer.class);
        if (data == null) {
            return defaultValue;
        } else {
            return data;
        }
    }

    /**
     * Read Long from database synchronously
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return data
     */
    public long readLongFromDatabase(String key, long defaultValue) {
        Long data = (Long) readFromDatabase(key, Long.class);
        if (data == null) {
            return defaultValue;
        } else {
            return data;
        }
    }

    /**
     * Read Double from database synchronously
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return data
     */
    public double readDoubleFromDatabase(String key, double defaultValue) {
        Double data = (Double) readFromDatabase(key, Double.class);
        if (data == null) {
            return defaultValue;
        } else {
            return data;
        }
    }

    /**
     * Read Boolean from database synchronously
     *
     * @param key          key
     * @param defaultValue defaultValue
     * @return data
     */
    public boolean readBooleanFromDatabase(String key, boolean defaultValue) {
        Boolean data = (Boolean) readFromDatabase(key, Boolean.class);
        if (data == null) {
            return defaultValue;
        } else {
            return data;
        }
    }

    /**
     * Delete data from database, core method, synchronously
     *
     * @param key key
     */
    public boolean deleteFromDatabase(String key) {
        try {
            databaseOperationPreCheck(key);
            synchronized (database) {
                database.del(key);
            }
            log("{ key = " + key + " } delete from database finished");
            return true;
        } catch (Exception e) {
            logError(" { key = " + key + " } delete from database failed", e);
            return false;
        }
    }

    /**
     * Delete data from database, asynchronously
     *
     * @param key      key
     * @param callback callback
     */
    public void deleteFromDatabaseAsync(String key, DeleteCallback callback) {
        DeleteBundle deleteBundle = new DeleteBundle(key, callback);
        dataMedium.put(Constant.DELETE_KEY_PREFIX + key, deleteBundle);
        callOnWorkHandler(Constant.MSG_WORK_DELETE, key);
    }

    /**
     * Mass delete from database, asynchronously
     *
     * @param keys     keys
     * @param callback callback
     */
    public void massDeleteFromDatabaseAsync(String[] keys, MassDeleteCallback callback) {
        if (keys != null && keys.length > 0) {
            String key = String.valueOf(System.currentTimeMillis());
            MassDeleteBundle massDeleteBundle = new MassDeleteBundle(keys, callback);
            dataMedium.put(Constant.MASS_DELETE_KEY_PREFIX + key, massDeleteBundle);
            callOnWorkHandler(Constant.MSG_WORK_MASS_DELETE, key);
        } else {
            if (callback != null) {
                callback.onResult(false);
            }
        }
    }


    /**
     * Return whether key exist in database
     *
     * @param key key
     * @return exist
     */
    public boolean keyExist(String key) {
        boolean exist = false;
        try {
            databaseOperationPreCheck(key);
            synchronized (database) {
                exist = database.exists(key);
            }
        } catch (Exception ignore) {
        }
        log("{ key = " + key + " } exist = " + exist);
        return exist;
    }

    /**
     * Return keys with same prefix from database, synchronously
     *
     * @param prefix prefix
     * @return keys
     */
    public String[] findKeysByPrefix(String prefix) {
        String[] keys = null;
        try {
            databaseOperationPreCheck(prefix);
            synchronized (database) {
                keys = database.findKeys(prefix);
            }
        } catch (Exception ignore) {
        }
        if (keys == null) {
            keys = new String[]{};
        }
        log("{ prefix = " + prefix + " } has " + keys.length + " keys");
        return keys;
    }

    /**
     * Return keys with same prefix from database, asynchronously
     *
     * @param prefix   prefix
     * @param callback callback
     */
    public void findKeysByPrefix(String prefix, FindKeysCallback callback) {
        String key = String.valueOf(System.currentTimeMillis());
        FindKeysByPrefixBundle findKeysByPrefixBundle = new FindKeysByPrefixBundle(prefix, callback);
        dataMedium.put(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key, findKeysByPrefixBundle);
        callOnWorkHandler(Constant.MSG_WORK_FIND_KEYS_BY_PREFIX, key);
    }


    /**
     * Save data in memory cache, default will be weak reference mode
     *
     * @param key      key
     * @param data     data
     * @param strongly strongly or weak reference
     */
    public void writeInMemory(String key, Object data, boolean strongly) {
        if (strongly) {
            memoryCacheMap.put(key, data);
        } else {
            memoryCacheMap.put(key, new WeakReference<>(data));
        }
        log("{ key = " + key + " data = " + data + " } save in memory finished");
        log("memory cache size: " + memoryCacheMap.size());
    }

    /**
     * Save data in memory cache
     *
     * @param key  key
     * @param data data
     */
    public void writeInMemory(String key, Object data) {
        writeInMemory(key, data, false);
    }

    /**
     * Read data from memory cache, default will be weak reference mode
     *
     * @param key      key
     * @param strongly strongly or weak reference
     * @return data
     */
    public Object readFromMemory(String key, boolean strongly) {
        Object data = null;
        if (strongly) {
            data = memoryCacheMap.get(key);
            if (data instanceof WeakReference) {
                data = null;
                try {
                    throw new IllegalArgumentException("You may have chosen the wrong reference type");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        } else {
            WeakReference<Object> dataReference = null;
            try {
                dataReference = (WeakReference<Object>) memoryCacheMap.get(key);
            } catch (Exception ignore) {
                try {
                    throw new IllegalArgumentException("You may have chosen the wrong reference type");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            if (dataReference != null) {
                data = dataReference.get();
            }
        }
        log("{ key = " + key + " data = " + data + " } read from memory finished");
        return data;
    }

    /**
     * Read data from memory cache
     *
     * @param key key
     * @return data
     */
    public Object readFromMemory(String key) {
        return readFromMemory(key, false);
    }

    /**
     * Delete data from memory cache
     *
     * @param key key
     */
    public void deleteFromMemory(String key) {
        memoryCacheMap.delete(key);
        log("{ key = " + key + " } delete from memory finished");
        log("memory cache size: " + memoryCacheMap.size());
    }

    /**
     * Memory cache key array
     *
     * @return key array
     */
    public String[] memoryCacheKeys() {
        return memoryCacheMap.keySet();
    }

    /**
     * Clear memory cache
     */
    public void clearMemoryCache() {
        memoryCacheMap.clear();
        log("memory cache clear finish");
    }

    /**
     * Send message to work handler
     *
     * @param message message
     * @param key     key with event prefix
     */
    private void callOnWorkHandler(int message, String key) {
        Message msg = getWorkHandler().obtainMessage(message);
        msg.obj = key;
        getWorkHandler().sendMessage(msg);
    }

    /**
     * Handle job in main thread
     *
     * @param msg msg
     */
    private void handleMainMessage(Message msg) {
        String key = (String) msg.obj;
        switch (msg.what) {
            case Constant.MSG_MAIN_SAVE_CALLBACK:
                SaveBundle saveBundle = (SaveBundle) dataMedium.get(Constant.SAVE_KEY_PREFIX + key);
                if (saveBundle != null && saveBundle.callback != null) {
                    dataMedium.remove(Constant.SAVE_KEY_PREFIX + key);
                    saveBundle.callback.onResult(saveBundle.success);
                }
                break;
            case Constant.MSG_MAIN_READ_CALLBACK:
                ReadBundle readBundle = (ReadBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                if (readBundle != null && readBundle.callback != null) {
                    dataMedium.remove(Constant.READ_KEY_PREFIX + key);
                    boolean success = readBundle.data != null;
                    readBundle.callback.onResult(success, readBundle.data);
                }
                break;
            case Constant.MSG_MAIN_DELETE_CALLBACK:
                DeleteBundle deleteBundle = (DeleteBundle) dataMedium.get(Constant.DELETE_KEY_PREFIX + key);
                if (deleteBundle != null && deleteBundle.callback != null) {
                    dataMedium.remove(Constant.DELETE_KEY_PREFIX + key);
                    deleteBundle.callback.onResult(deleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_MASS_DELETE_CALLBACK:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) dataMedium.get(Constant.MASS_DELETE_KEY_PREFIX + key);
                if (massDeleteBundle != null && massDeleteBundle.callback != null) {
                    dataMedium.remove(Constant.MASS_DELETE_KEY_PREFIX + key);
                    massDeleteBundle.callback.onResult(massDeleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) dataMedium.get(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                if (findKeysByPrefixBundle != null && findKeysByPrefixBundle.callback != null) {
                    dataMedium.remove(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                    findKeysByPrefixBundle.callback.onResult(findKeysByPrefixBundle.keys);
                }
                break;
        }
    }

    /**
     * Send message to work handler
     *
     * @param message message
     * @param key     key with event prefix
     */
    private void callOnMainHandler(int message, String key) {
        Message msg = getMainHandler().obtainMessage(message);
        msg.obj = key;
        getMainHandler().sendMessage(msg);
    }

    /**
     * Handle job in work thread
     *
     * @param msg msg
     */
    private void handleWorkMessage(Message msg) {
        String key = (String) msg.obj;
        switch (msg.what) {
            case Constant.MSG_WORK_SAVE:
                SaveBundle saveBundle = (SaveBundle) dataMedium.get(Constant.SAVE_KEY_PREFIX + key);
                saveBundle.success = writeInDatabase(saveBundle.key, saveBundle.data);
                callOnMainHandler(Constant.MSG_MAIN_SAVE_CALLBACK, key);
                break;
            case Constant.MSG_WORK_READ:
                ReadBundle readBundle = (ReadBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                readBundle.data = readFromDatabase(readBundle.key, readBundle.dataClass);
                callOnMainHandler(Constant.MSG_MAIN_READ_CALLBACK, key);
                break;
            case Constant.MSG_WORK_DELETE:
                DeleteBundle deleteBundle = (DeleteBundle) dataMedium.get(Constant.DELETE_KEY_PREFIX + key);
                deleteBundle.success = deleteFromDatabase(deleteBundle.key);
                callOnMainHandler(Constant.MSG_MAIN_DELETE_CALLBACK, key);
                break;
            case Constant.MSG_WORK_MASS_DELETE:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) dataMedium.get(Constant.MASS_DELETE_KEY_PREFIX + key);
                for (String k : massDeleteBundle.keys) {
                    if (deleteFromDatabase(k)) {
                        massDeleteBundle.success = true;
                    }
                }
                callOnMainHandler(Constant.MSG_MAIN_MASS_DELETE_CALLBACK, key);
                break;
            case Constant.MSG_WORK_FIND_KEYS_BY_PREFIX:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) dataMedium.get(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                findKeysByPrefixBundle.keys = findKeysByPrefix(findKeysByPrefixBundle.prefix);
                callOnMainHandler(Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK, key);
                break;
            default:
                return;
        }
    }

    private MainHandler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new MainHandler(this, Looper.getMainLooper());
        }
        return mainHandler;
    }

    private WorkHandler getWorkHandler() {
        if (workThread == null || !workThread.isAlive() || workHandler == null) {
            workHandler = new WorkHandler(this, getWorkThread().getLooper());
        }
        return workHandler;
    }

    private HandlerThread getWorkThread() {
        if (workThread == null || !workThread.isAlive()) {
            workThread = new HandlerThread("panther", Process.THREAD_PRIORITY_BACKGROUND);
            workThread.start();
        }
        return workThread;
    }

    private void log(String content) {
        if (configuration.logEnabled)
            Log.d("Panther", content);
    }

    private void logError(String content, Throwable error) {
        if (configuration.logEnabled)
            Log.e("Panther", content, error);
    }

    private static class MainHandler extends Handler {
        private final WeakReference<Panther> weakReference;

        private MainHandler(Panther panther, Looper Looper) {
            super(Looper);
            weakReference = new WeakReference<>(panther);
        }

        @Override
        public void handleMessage(Message msg) {
            Panther panther = weakReference.get();
            if (panther != null && msg != null) {
                panther.handleMainMessage(msg);
            }
        }
    }

    private static class WorkHandler extends Handler {
        private final WeakReference<Panther> weakReference;

        private WorkHandler(Panther panther, Looper Looper) {
            super(Looper);
            weakReference = new WeakReference<>(panther);
        }

        @Override
        public void handleMessage(Message msg) {
            Panther panther = weakReference.get();
            if (panther != null && msg != null) {
                panther.handleWorkMessage(msg);
            }
        }
    }
}