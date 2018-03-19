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

import com.alibaba.fastjson.JSON;
import com.snappydb.DB;
import com.snappydb.DBFactory;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.panther.bundle.DataBundle;
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

/**
 * Created by LiShen on 2016/7/8.
 * Panther
 */
public final class Panther {
    private static volatile Panther panther;

    private PantherConfiguration configuration;
    private MemoryCacheMap memoryCacheMap;
    private static volatile DB database;
    private MainHandler mainHandler;
    private HandlerThread workThread;
    private WorkHandler workHandler;

    private Panther(PantherConfiguration pantherConfiguration) {
        configuration = pantherConfiguration;
        log("========== Panther configuration =========="
                + "\ndatabase folder: " + configuration.databaseFolder.getAbsolutePath()
                + "\ndatabase name: " + configuration.databaseName
                + "\nmemory cache size: " + configuration.memoryCacheSize
                + "\n===========================================");
        // memory cache
        memoryCacheMap = new MemoryCacheMap(configuration.memoryCacheSize);
        // database
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
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Open database
     */
    public void openDatabase() {
        try {
            if (database != null && database.isOpen()) {
                closeDatabase();
            }
            synchronized (DB.class) {
                database = DBFactory.open(configuration.databaseFolder.getAbsolutePath(),
                        configuration.databaseName);
            }
            log("database: " + configuration.databaseName + " open success");
        } catch (Exception e) {
            e.printStackTrace();
            logError("database: " + configuration.databaseName + " open failed");
        }
    }

    /**
     * Whether database available
     *
     * @return available
     */
    public boolean checkDatabaseAvailable() {
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
            logError("KEY can not be null !");
            throw new IllegalArgumentException();
        }
        if (!checkDatabaseAvailable()) {
            logError("database is unavailable");
            throw new IllegalStateException();
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
            DataBundle dataBundle = new DataBundle();
            dataBundle.setKey(key);
            dataBundle.setData(data);
            dataBundle.setUpdateTime(System.currentTimeMillis());
            String dataBundleJson = JSON.toJSONString(dataBundle);
            synchronized (database) {
                database.put(key, dataBundleJson);
            }
            log(dataBundleJson + "\n saved in database finished");
            return true;
        } catch (Exception e) {
            logError("{key=" + key + "} save data in database failed");
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
        Message msg = getWorkHandler().obtainMessage(Constant.MSG_WORK_SAVE);
        msg.obj = new SaveBundle(key, data, callback);
        getWorkHandler().sendMessage(msg);
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
    public DataBundle readFromDatabase(String key, Class dataClass) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.setKey(key);
        // data bundle json
        String dataBundleJson;
        try {
            databaseOperationPreCheck(key);
            synchronized (database) {
                dataBundleJson = database.get(key);
            }
        } catch (Exception e) {
            logError("read {key=" + key + "} from database failed");
            return dataBundle;
        }
        // data parse
        JSONObject dataBundleJsonObject = null;
        try {
            dataBundleJsonObject = new JSONObject(dataBundleJson);
        } catch (Exception ignore) {
        }
        if (dataBundleJsonObject != null) {
            // update time
            try {
                dataBundle.setUpdateTime(dataBundleJsonObject.getLong(Constant.JSON_KEY_UPDATE_TIME));
            } catch (Exception ignore) {
            }
            // data json
            String dataJson = null;
            try {
                dataJson = dataBundleJsonObject.getString(Constant.JSON_KEY_DATA);
            } catch (Exception ignore) {
            }
            if (TextUtils.isEmpty(dataJson)) {
                dataJson = "";
            }
            dataBundle.setDataJson(dataJson);
            // data parse
            if (dataClass == String.class) {
                // String.class
                dataBundle.setData(dataJson);
            } else if (!TextUtils.isEmpty(dataJson)) {
                if (dataJson.startsWith(Constant.JSON_ARRAY_PREFIX)) {
                    // parse to array
                    try {
                        dataBundle.setData(JSON.parseArray(dataJson, dataClass));
                    } catch (Exception e) {
                        logError("read {key=" + key + "} from database parse failed");
                        return dataBundle;
                    }
                } else {
                    // parse to object
                    try {
                        dataBundle.setData(JSON.parseObject(dataJson, dataClass));
                    } catch (Exception e) {
                        logError("read {key=" + key + "} from database parse failed");
                        return dataBundle;
                    }
                }
            }
        } else {
            logError("read {key=" + key + "} from database failed");
            return dataBundle;
        }
        log(dataBundle + "\n read from database finished");
        return dataBundle;
    }

    /**
     * Read data from database asynchronously
     *
     * @param key       key
     * @param dataClass class of data
     * @param callback  callback
     */
    public void readFromDatabaseAsync(String key, Class dataClass, ReadCallback callback) {
        Message msg = getWorkHandler().obtainMessage(Constant.MSG_WORK_READ);
        msg.obj = new ReadBundle(key, dataClass, callback);
        getWorkHandler().sendMessage(msg);
    }

    /**
     * Read data from database directly and synchronously
     * Not recommended to call for read large data in the main thread.
     * Read large data use {@link #readFromDatabaseAsync(String, Class, ReadCallback)}
     *
     * @param key       key
     * @param dataClass class of data
     * @return data
     */
    public Object readObjectFromDatabase(String key, Class dataClass) {
        return readFromDatabase(key, dataClass).getData();
    }

    /**
     * Read String from database synchronously
     *
     * @param key key
     * @return data
     */
    public String readStringFromDatabase(String key) {
        String data = (String) readObjectFromDatabase(key, String.class);
        if (data == null) {
            data = "";
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
        Integer data = (Integer) readObjectFromDatabase(key, Integer.class);
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
        Long data = (Long) readObjectFromDatabase(key, Long.class);
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
        Double data = (Double) readObjectFromDatabase(key, Double.class);
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
        Boolean data = (Boolean) readObjectFromDatabase(key, Boolean.class);
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
            log("{key=" + key + "}\n delete from database finished");
            return true;
        } catch (Exception e) {
            logError("{key=" + key + "}\n delete from database failed");
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
        Message msg = getWorkHandler().obtainMessage(Constant.MSG_WORK_DELETE);
        msg.obj = new DeleteBundle(key, callback);
        getWorkHandler().sendMessage(msg);
    }

    /**
     * Mass delete from database, asynchronously
     *
     * @param keys     keys
     * @param callback callback
     */
    public void massDeleteFromDatabaseAsync(String[] keys, MassDeleteCallback callback) {
        Message msg = getWorkHandler().obtainMessage(Constant.MSG_WORK_MASS_DELETE);
        msg.obj = new MassDeleteBundle(keys, callback);
        getWorkHandler().sendMessage(msg);
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
        log("{key=" + key + "} exist=" + exist);
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
            if (checkDatabaseAvailable() && !TextUtils.isEmpty(prefix)) {
                synchronized (database) {
                    keys = database.findKeys(prefix);
                }
            }
        } catch (Exception ignore) {
        }
        if (keys == null) {
            keys = new String[]{};
        }
        log("{prefix=" + prefix + "} has " + keys.length + " keys");
        return keys;
    }

    /**
     * Return keys with same prefix from database, asynchronously
     *
     * @param prefix   prefix
     * @param callback callback
     */
    public void findKeysByPrefix(String prefix, FindKeysCallback callback) {
        Message msg = getWorkHandler().obtainMessage(Constant.MSG_WORK_FIND_KEYS_BY_PREFIX);
        msg.obj = new FindKeysByPrefixBundle(prefix, callback);
        getWorkHandler().sendMessage(msg);
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
        log("{key=" + key + " data=" + data + "}\nsave in memory finished");
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
        } else {
            WeakReference<Object> dataReference = (WeakReference<Object>) memoryCacheMap.get(key);
            if (dataReference != null) {
                data = dataReference.get();
            }
        }
        log("{key=" + key + " data=" + data + "}\nread from memory finished");
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
        log("{key=" + key + "}\ndelete from memory finished");
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
     * Handle job in main thread
     *
     * @param msg msg
     */
    private void handleMainMessage(Message msg) {
        switch (msg.what) {
            case Constant.MSG_MAIN_SAVE_CALLBACK:
                SaveBundle saveBundle = (SaveBundle) msg.obj;
                if (saveBundle != null && saveBundle.callback != null) {
                    saveBundle.callback.onResult(saveBundle.success);
                }
                break;
            case Constant.MSG_MAIN_READ_CALLBACK:
                ReadBundle readBundle = (ReadBundle) msg.obj;
                if (readBundle != null && readBundle.callback != null) {
                    readBundle.callback.onResult(readBundle.data);
                }
                break;
            case Constant.MSG_MAIN_DELETE_CALLBACK:
                DeleteBundle deleteBundle = (DeleteBundle) msg.obj;
                if (deleteBundle != null && deleteBundle.callback != null) {
                    deleteBundle.callback.onResult(deleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_MASS_DELETE_CALLBACK:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) msg.obj;
                if (massDeleteBundle != null && massDeleteBundle.callback != null) {
                    massDeleteBundle.callback.onResult(massDeleteBundle.success);
                }
                break;
            case Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) msg.obj;
                if (findKeysByPrefixBundle != null && findKeysByPrefixBundle.callback != null) {
                    findKeysByPrefixBundle.callback.onResult(findKeysByPrefixBundle.keys);
                }
                break;
        }
    }

    /**
     * Handle job in work thread
     *
     * @param msg msg
     */
    private void handleWorkMessage(Message msg) {
        Message m;
        switch (msg.what) {
            case Constant.MSG_WORK_SAVE:
                SaveBundle saveBundle = (SaveBundle) msg.obj;
                saveBundle.success = writeInDatabase(saveBundle.key, saveBundle.data);
                m = getMainHandler().obtainMessage(Constant.MSG_MAIN_SAVE_CALLBACK);
                m.obj = saveBundle;
                break;
            case Constant.MSG_WORK_READ:
                ReadBundle readBundle = (ReadBundle) msg.obj;
                readBundle.data = readFromDatabase(readBundle.key, readBundle.dataClass);
                m = getMainHandler().obtainMessage(Constant.MSG_MAIN_READ_CALLBACK);
                m.obj = readBundle;
                break;
            case Constant.MSG_WORK_DELETE:
                DeleteBundle deleteBundle = (DeleteBundle) msg.obj;
                deleteBundle.success = deleteFromDatabase(deleteBundle.key);
                m = getMainHandler().obtainMessage(Constant.MSG_MAIN_DELETE_CALLBACK);
                m.obj = deleteBundle;
                break;
            case Constant.MSG_WORK_MASS_DELETE:
                MassDeleteBundle massDeleteBundle = (MassDeleteBundle) msg.obj;
                for (String key : massDeleteBundle.keys) {
                    if (deleteFromDatabase(key)) {
                        massDeleteBundle.success = true;
                    }
                }
                m = getMainHandler().obtainMessage(Constant.MSG_MAIN_MASS_DELETE_CALLBACK);
                m.obj = massDeleteBundle;
                break;
            case Constant.MSG_WORK_FIND_KEYS_BY_PREFIX:
                FindKeysByPrefixBundle findKeysByPrefixBundle = (FindKeysByPrefixBundle) msg.obj;
                findKeysByPrefixBundle.keys = findKeysByPrefix(findKeysByPrefixBundle.prefix);
                m = getMainHandler().obtainMessage(Constant.MSG_MAIN_FIND_KEYS_BY_PREFIX_CALLBACK);
                m.obj = findKeysByPrefixBundle;
                break;
            default:
                return;
        }
        getMainHandler().sendMessage(m);
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

    private void logError(String content) {
        if (configuration.logEnabled)
            Log.e("Panther", content);
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