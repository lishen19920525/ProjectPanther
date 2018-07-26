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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Hashtable;

import io.panther.bundle.BaseBundle;
import io.panther.bundle.DeleteBundle;
import io.panther.bundle.FindKeysByPrefixBundle;
import io.panther.bundle.MassDeleteBundle;
import io.panther.bundle.ReadArrayBundle;
import io.panther.bundle.ReadBundle;
import io.panther.bundle.SaveBundle;
import io.panther.callback.DeleteCallback;
import io.panther.callback.FindKeysCallback;
import io.panther.callback.MassDeleteCallback;
import io.panther.callback.ReadArrayCallback;
import io.panther.callback.ReadCallback;
import io.panther.callback.WriteCallback;
import io.panther.constant.Constant;
import io.panther.memorycache.PantherMemoryCacheMap;
import io.panther.util.ConfigurationParser;
import io.panther.util.GZIP;
import io.panther.util.JSON;

/**
 * Created by LiShen on 2016/7/8.
 * Panther
 */
public final class Panther {
    private static volatile Panther panther;

    private PantherConfiguration configuration;

    private static volatile DB database;
    private Hashtable<String, BaseBundle> dataMedium;

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
        try {
            openDatabase();
        } catch (Exception e) {
            logError(e.toString(), new IllegalStateException("DATABASE is unavailable"));
        }
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
    private void openDatabase() throws Exception {
        if (database != null && database.isOpen()) {
            log("database: " + configuration.databaseName + " already open, no need to open again");
            return;
        }
        // read and save cache
        synchronized (this) {
            dataMedium = new Hashtable<>();
        }
        synchronized (DB.class) {
            database = DBFactory.open(configuration.databaseFolder.getAbsolutePath(),
                    configuration.databaseName);
        }
        log("database: " + configuration.databaseName + " open success");
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
            openDatabase();
        }
    }

    /**
     * Save in database synchronously, core method
     * Not recommended to call for storing large data in the main thread
     * Large data use {@link #writeInDatabaseAsync(String, Object, WriteCallback)}
     *
     * @param key  key
     * @param data data
     * @return result
     */
    public boolean writeInDatabase(String key, Object data) {
        try {
            databaseOperationPreCheck(key);
            // to Json string
            String dataJson;
            if (data instanceof String) {
                dataJson = (String) data;
            } else {
                dataJson = JSON.toJSONString(data);
            }
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
    public void writeInDatabaseAsync(String key, Object data, WriteCallback callback) {
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
    public <T> T readFromDatabase(String key, Class<T> dataClass) {
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
                // parse to object
                try {
                    data = JSON.parseObject(dataJson, dataClass);
                } catch (Exception e) {
                    logError("read { key = " + key + " } from database parse failed", e);
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
    public <T> void readFromDatabaseAsync(String key, Class<T> dataClass, ReadCallback<T> callback) {
        ReadBundle<T> readBundle = new ReadBundle(key, dataClass, callback);
        dataMedium.put(Constant.READ_KEY_PREFIX + key, readBundle);
        callOnWorkHandler(Constant.MSG_WORK_READ, key);
    }

    /**
     * Read array data from database synchronously, core method.
     * Not recommended to call for read large data in the main thread.
     * Read large data use {@link #readArrayFromDatabaseAsync(String, Class, ReadArrayCallback)}
     *
     * @param key       key
     * @param dataClass class of data
     * @return array data
     */
    public <T> T[] readArrayFromDatabase(String key, Class<T> dataClass) {
        String dataJson = null;
        T[] data = null;
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
            try {
                data = JSON.parseArray(dataJson, dataClass);
            } catch (Exception e) {
                logError("read { key = " + key + " } from database parse failed", e);
            }
        }
        if (!TextUtils.isEmpty(dataJson) && data != null) {
            log("key = " + key + " value = " + dataJson + " read from database finished");
        }
        return data;
    }

    /**
     * Read array data from database asynchronously
     *
     * @param key       key
     * @param dataClass class of data
     * @param callback  callback
     */
    public <T> void readArrayFromDatabaseAsync(String key, Class<T> dataClass, ReadArrayCallback<T> callback) {
        ReadArrayBundle<T> readBundle = new ReadArrayBundle(key, dataClass, callback);
        dataMedium.put(Constant.READ_KEY_PREFIX + key, readBundle);
        callOnWorkHandler(Constant.MSG_WORK_READ, key, Constant.MSG_SUBTYPE_READ_ARRAY);
    }


    /**
     * Read String from database synchronously
     *
     * @param key key
     * @return data
     */
    public String readStringFromDatabase(String key) {
        String data = readFromDatabase(key, String.class);
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
        String data = readFromDatabase(key, String.class);
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
        Integer data = readFromDatabase(key, Integer.class);
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
        Long data = readFromDatabase(key, Long.class);
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
        Double data = readFromDatabase(key, Double.class);
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
        Boolean data = readFromDatabase(key, Boolean.class);
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
    public <V> V readFromMemory(String key, boolean strongly) {
        V data = null;
        Object dataTemp = memoryCacheMap.get(key);
        if (strongly) {
            if (dataTemp instanceof WeakReference) {
                try {
                    throw new IllegalArgumentException("You may have chosen the wrong reference type");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (dataTemp != null)
                        data = (V) dataTemp;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                if (dataTemp != null && !(dataTemp instanceof WeakReference)) {
                    dataTemp = null;
                    throw new IllegalArgumentException("You may have chosen the wrong reference type");
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            if (dataTemp != null) {
                try {
                    data = ((WeakReference<V>) dataTemp).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    public <V> V readFromMemory(String key) {
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
        callOnWorkHandler(message, key, Constant.MSG_SUBTYPE_NORMAL);
    }

    /**
     * Send message to work handler
     *
     * @param message message
     * @param key     key with event prefix
     * @param subtype subtype
     */
    private void callOnWorkHandler(int message, String key, int subtype) {
        Message msg = getWorkHandler().obtainMessage(message);
        msg.obj = key;
        msg.arg1 = subtype;
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
                if (saveBundle != null) {
                    dataMedium.remove(Constant.SAVE_KEY_PREFIX + key);
                    if (saveBundle.callback != null)
                        saveBundle.callback.onResult(saveBundle.success);
                }
                break;
            case Constant.MSG_MAIN_READ_CALLBACK:
                if (msg.arg1 == Constant.MSG_SUBTYPE_READ_ARRAY) {
                    ReadArrayBundle readArrayBundle = (ReadArrayBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                    if (readArrayBundle != null) {
                        dataMedium.remove(Constant.READ_KEY_PREFIX + key);
                        if (readArrayBundle.callback != null) {
                            boolean success = readArrayBundle.data != null;
                            readArrayBundle.callback.onResult(success, readArrayBundle.data);
                        }
                    }
                } else {
                    ReadBundle readBundle = (ReadBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                    if (readBundle != null) {
                        dataMedium.remove(Constant.READ_KEY_PREFIX + key);
                        if (readBundle.callback != null) {
                            boolean success = readBundle.data != null;
                            readBundle.callback.onResult(success, readBundle.data);
                        }
                    }
                }
                break;
            case Constant.MSG_MAIN_DELETE_CALLBACK:
                DeleteBundle deleteBundle = (DeleteBundle) dataMedium.get(Constant.DELETE_KEY_PREFIX + key);
                if (deleteBundle != null) {
                    dataMedium.remove(Constant.DELETE_KEY_PREFIX + key);
                    if (deleteBundle.callback != null)
                        deleteBundle.callback.onResult(deleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_MASS_DELETE_CALLBACK:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) dataMedium.get(Constant.MASS_DELETE_KEY_PREFIX + key);
                if (massDeleteBundle != null) {
                    dataMedium.remove(Constant.MASS_DELETE_KEY_PREFIX + key);
                    if (massDeleteBundle.callback != null)
                        massDeleteBundle.callback.onResult(massDeleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) dataMedium.get(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                if (findKeysByPrefixBundle != null) {
                    dataMedium.remove(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                    if (findKeysByPrefixBundle.callback != null)
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
        callOnMainHandler(message, key, Constant.MSG_SUBTYPE_NORMAL);
    }

    /**
     * Send message to work handler
     *
     * @param message message
     * @param key     key with event prefix
     * @param subtype subtype
     */
    private void callOnMainHandler(int message, String key, int subtype) {
        Message msg = getMainHandler().obtainMessage(message);
        msg.obj = key;
        msg.arg1 = subtype;
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
                if (saveBundle != null) {
                    saveBundle.success = writeInDatabase(saveBundle.key, saveBundle.data);
                    callOnMainHandler(Constant.MSG_MAIN_SAVE_CALLBACK, key);
                }
                break;
            case Constant.MSG_WORK_READ:
                if (msg.arg1 == Constant.MSG_SUBTYPE_READ_ARRAY) {
                    ReadArrayBundle readArrayBundle = (ReadArrayBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                    if (readArrayBundle != null) {
                        readArrayBundle.data = readArrayFromDatabase(readArrayBundle.key, readArrayBundle.dataClass);
                        callOnMainHandler(Constant.MSG_MAIN_READ_CALLBACK, key, msg.arg1);
                    }
                } else {
                    ReadBundle readBundle = (ReadBundle) dataMedium.get(Constant.READ_KEY_PREFIX + key);
                    if (readBundle != null) {
                        readBundle.data = readFromDatabase(readBundle.key, readBundle.dataClass);
                        callOnMainHandler(Constant.MSG_MAIN_READ_CALLBACK, key);
                    }
                }
                break;
            case Constant.MSG_WORK_DELETE:
                DeleteBundle deleteBundle = (DeleteBundle) dataMedium.get(Constant.DELETE_KEY_PREFIX + key);
                if (deleteBundle != null) {
                    deleteBundle.success = deleteFromDatabase(deleteBundle.key);
                    callOnMainHandler(Constant.MSG_MAIN_DELETE_CALLBACK, key);
                }
                break;
            case Constant.MSG_WORK_MASS_DELETE:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) dataMedium.get(Constant.MASS_DELETE_KEY_PREFIX + key);
                if (massDeleteBundle != null) {
                    for (String k : massDeleteBundle.keys) {
                        if (deleteFromDatabase(k)) {
                            massDeleteBundle.success = true;
                        }
                    }
                    callOnMainHandler(Constant.MSG_MAIN_MASS_DELETE_CALLBACK, key);
                }
                break;
            case Constant.MSG_WORK_FIND_KEYS_BY_PREFIX:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) dataMedium.get(Constant.FIND_KEYS_BY_PREFIX_KEY_PREFIX + key);
                if (findKeysByPrefixBundle != null) {
                    findKeysByPrefixBundle.keys = findKeysByPrefix(findKeysByPrefixBundle.prefix);
                    callOnMainHandler(Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK, key);
                }
                break;
        }
    }

    private MainHandler getMainHandler() {
        if (mainHandler == null || mainHandler.reference.get() == null) {
            mainHandler = new MainHandler(this, Looper.getMainLooper());
        }
        return mainHandler;
    }

    private WorkHandler getWorkHandler() {
        if (workThread == null || !workThread.isAlive() || workHandler == null || workHandler.reference.get() == null) {
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
        private final SoftReference<Panther> reference;

        private MainHandler(Panther panther, Looper Looper) {
            super(Looper);
            reference = new SoftReference<>(panther);
        }

        @Override
        public void handleMessage(Message msg) {
            Panther panther = reference.get();
            if (panther != null && msg != null) {
                panther.handleMainMessage(msg);
            }
        }
    }

    private static class WorkHandler extends Handler {
        private final SoftReference<Panther> reference;

        private WorkHandler(Panther panther, Looper Looper) {
            super(Looper);
            reference = new SoftReference<>(panther);
        }

        @Override
        public void handleMessage(Message msg) {
            Panther panther = reference.get();
            if (panther != null && msg != null) {
                panther.handleWorkMessage(msg);
            }
        }
    }
}