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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import io.panther.observer.DataBaseSuccessObserver;
import io.panther.observer.DatabaseListObserver;
import io.panther.observer.DatabaseObserver;
import io.panther.util.GZIPUtil;
import io.panther.util.JSONUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO Challenge of Multi Process
 * Created by LiShen on 2016/7/8.
 * Panther
 */
@SuppressWarnings("unchecked")
public final class Panther {
    static final String PANTHER_MODULE_NAME = "io.panther.PantherModule";

    static final int DEFAULT_MEMORY_CACHE_SIZE = 128;

    private static final String KEY = "key";
    private static final String DATA = "data";
    private static final String DATA_CLASS = "dataClass";

    private static final int GZIP_TRIGGER_LENGTH = 1024;

    private static volatile Panther panther;

    @NonNull
    PantherConfiguration configuration;

    // database
    private final PantherDatabase database = new PantherDatabase();
    // memory cache
    private final PantherMemoryCache memoryCache;

    private Panther(PantherConfiguration configuration) {
        this.configuration = configuration;

        // configuration
        log("\n========== Panther configuration =========="
                + "\nDatabase folder: " + configuration.databaseFolder.getPath()
                + "\nDatabase name: " + configuration.databaseName
                + "\nMemory cache size: " + configuration.memoryCacheSize
                + "\n===========================================");

        // memory cache
        memoryCache = new PantherMemoryCache(configuration.memoryCacheSize);

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
                    ConfigurationParser parser = new ConfigurationParser(context);
                    panther = new Panther(parser.parse());
                }
            }
        }
        return panther;
    }

    /**
     * Open database
     */
    private boolean openDatabase() {
        boolean result;
        synchronized (database) {
            result = database.open(configuration.databaseFolder.getPath(), configuration.databaseName);
        }
        if (result) {
            log("Database " + configuration.databaseName + " open success");
        } else {
            logError("Database " + configuration.databaseName + " open failed",
                    new RuntimeException("Database " + configuration.databaseName + " open failed"));
        }
        return result;
    }

    /**
     * Close the database
     */
    public void closeDatabase() {
        synchronized (database) {
            boolean result = database.close();
            if (result) {
                log("Database " + configuration.databaseName + " close success");
            } else {
                logError(configuration.databaseName + "Database close failed",
                        new RuntimeException("Database close failed"));
            }
        }
    }

    /**
     * Whether database available
     *
     * @return available
     */
    private boolean checkDatabaseAvailable() {
        boolean result;
        synchronized (database) {
            result = database.isAvailable();
        }
        return result;
    }

    /**
     * Check the key and database before the database operation
     *
     * @param key key
     */
    private void databaseOperationPreCheck(String key) throws RuntimeException {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("KEY or PREFIX can not be null !");
        }
        if (!checkDatabaseAvailable()) {
            boolean openResult = openDatabase();
            if (!openResult)
                throw new RuntimeException("Database open failed!");
        }
    }

    /**
     * Save in database synchronously, core method
     * Not recommended to call for storing large data in the main thread
     * Large data use {@link #writeInDatabaseAsync(String, Object, DataBaseSuccessObserver)}
     *
     * @param key  key
     * @param data data
     * @return result
     */
    public boolean writeInDatabase(String key, Object data) {
        String dataJson;
        try {
            // pre check
            databaseOperationPreCheck(key);
            // data null --> delete
            if (data == null) {
                deleteFromDatabase(key);
                return true;
            }
            // to Json string
            if (data instanceof String) {
                dataJson = (String) data;
            } else {
                dataJson = JSONUtil.toJSONString(data);
            }
            // gzip
            if (TextUtils.isEmpty(dataJson)) {
                throw new RuntimeException("Save data parse failed!");
            }
            // start
            DataBundle dataBundle = new DataBundle();
            dataBundle.key = key;
            dataBundle.dataJson = dataJson;
            dataBundle.gzip = false;
            if (dataJson != null && dataJson.length() >= GZIP_TRIGGER_LENGTH) {
                String dataJsonGzip = GZIPUtil.compress(dataJson);
                if (!TextUtils.isEmpty(dataJson)) {
                    dataBundle.dataJson = dataJsonGzip;
                    dataBundle.gzip = true;
                }
            }
            dataBundle.time = System.currentTimeMillis();
            String dataBundleJson = JSONUtil.toJSONString(dataBundle);
            if (dataBundleJson == null) {
                throw new RuntimeException("Save data parse failed!");
            }
            // compress
            synchronized (database) {
                database.get().put(key, dataBundleJson);
                log("{ key = " + key + " value = " + dataJson + " } saved in database finished");
            }
        } catch (Exception e) {
            logError("{ key = " + key + " value = " + String.valueOf(data) + " } save in database failed", e);
            return false;
        }
        return true;
    }

    /**
     * Save in database asynchronously
     *
     * @param key      key
     * @param data     data
     * @param callback callback
     */
    public void writeInDatabaseAsync(String key, Object data, DataBaseSuccessObserver callback) {
        ArrayMap<String, Object> bundle = new ArrayMap<>();
        bundle.put(KEY, key);
        bundle.put(DATA, data);
        Observable.just(bundle)
                .map(new Function<ArrayMap<String, Object>, Boolean>() {
                    @Override
                    public Boolean apply(ArrayMap<String, Object> bundle) throws Exception {
                        String key = (String) bundle.get(KEY);
                        Object data = bundle.get(DATA);
                        bundle.clear();
                        return writeInDatabase(key, data);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }


    /**
     * Read from database synchronously, core method.
     * Not recommended to call for read large data in the main thread.
     * Read large data use {@link #readFromDatabaseAsync(String, Class, DatabaseObserver)}
     *
     * @param key       key
     * @param dataClass class of data
     * @return data
     */
    @Nullable
    public <T> T readFromDatabase(String key, Class<T> dataClass) {
        T data = null;
        try {
            // pre check
            databaseOperationPreCheck(key);
            // read data bundle json
            String dataBundleJson;
            synchronized (database) {
                dataBundleJson = database.get().get(key);
            }
            if (dataBundleJson == null) {
                throw new RuntimeException("Read { key = " + key + " } from database failed, no data");
            }
            DataBundle dataBundle = JSONUtil.parseObject(dataBundleJson, DataBundle.class);
            if (dataBundle == null) {
                throw new RuntimeException("Read { key = " + key + " } from database failed, parse failed");
            }
            String dataJson = dataBundle.dataJson;
            if (dataBundle.gzip) {
                dataJson = GZIPUtil.decompress(dataBundle.dataJson);
                if (dataJson == null) {
                    throw new RuntimeException("Read { key = " + key + " } from database failed, GZIP failed");
                }
            }
            data = JSONUtil.parseObject(dataJson, dataClass);
            if (data != null) {
                log("Read { key = " + key + " value = " + dataJson + " } read from database finished");
            } else {
                throw new RuntimeException("Read { key = " + key + " } from database failed, parse failed");
            }
        } catch (Exception e) {
            logError("Read { key = " + key + " } from database failed", e);
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
    public <T> void readFromDatabaseAsync(String key, Class<T> dataClass, DatabaseObserver<T> callback) {
        ArrayMap<String, Object> bundle = new ArrayMap<>();
        bundle.put(KEY, key);
        bundle.put(DATA_CLASS, dataClass);
        Observable.just(bundle)
                .map(new Function<ArrayMap<String, Object>, T>() {
                    @Override
                    public T apply(ArrayMap<String, Object> bundle) throws Exception {
                        String key = (String) bundle.get(KEY);
                        Class<T> dataClass = (Class<T>) bundle.get(DATA_CLASS);
                        return readFromDatabase(key, dataClass);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }

    /**
     * Read array data from database synchronously, database method.
     * Not recommended to call for read large data in the main thread.
     * Read large data use {@link #readListFromDatabaseAsync(String, Class, DatabaseListObserver)}
     *
     * @param key       key
     * @param dataClass class of data
     * @return array data
     */
    @Nullable
    public <T> List<T> readListFromDatabase(String key, Class<T> dataClass) {
        List<T> data = null;
        try {
            // pre check
            databaseOperationPreCheck(key);
            // read data bundle json
            String dataBundleJson;
            synchronized (database) {
                dataBundleJson = database.get().get(key);
            }
            if (dataBundleJson == null) {
                throw new RuntimeException("Read { key = " + key + " } from database failed, no data");
            }
            DataBundle dataBundle = JSONUtil.parseObject(dataBundleJson, DataBundle.class);
            if (dataBundle == null) {
                throw new RuntimeException("Read { key = " + key + " } from database failed, parse failed");
            }
            String dataJson = dataBundle.dataJson;
            if (dataBundle.gzip) {
                dataJson = GZIPUtil.decompress(dataBundle.dataJson);
                if (dataJson == null) {
                    throw new RuntimeException("Read { key = " + key + " } from database failed, GZIP failed");
                }
            }
            data = JSONUtil.parseList(dataJson, dataClass);
            if (data != null) {
                log("Read { key = " + key + " value = " + dataJson + " } read from database finished");
            } else {
                throw new RuntimeException("Read { key = " + key + " } from database failed, parse failed");
            }
        } catch (Exception e) {
            logError("Read { key = " + key + " } from database failed", e);
        }
        return data;
    }

    /**
     * Read list data from database asynchronously
     *
     * @param key       key
     * @param dataClass class of data
     * @param callback  callback
     */
    public <T> void readListFromDatabaseAsync(String key, Class<T> dataClass, DatabaseListObserver<T> callback) {
        ArrayMap<String, Object> bundle = new ArrayMap<>();
        bundle.put(KEY, key);
        bundle.put(DATA_CLASS, dataClass);
        Observable.just(bundle)
                .map(new Function<ArrayMap<String, Object>, List<T>>() {
                    @Override
                    public List<T> apply(ArrayMap<String, Object> bundle) throws Exception {
                        String key = (String) bundle.get(KEY);
                        Class<T> dataClass = (Class<T>) bundle.get(DATA_CLASS);
                        return readListFromDatabase(key, dataClass);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }


    /**
     * Read String from database synchronously
     *
     * @param key key
     * @return data
     */
    @NonNull
    public String readStringFromDatabase(String key) {
        return readStringFromDatabase(key, "");
    }

    /**
     * Read String from database synchronously
     *
     * @param key          key
     * @param defaultValue default value
     * @return data
     */
    @NonNull
    public String readStringFromDatabase(String key, @NonNull String defaultValue) {
        String data = readFromDatabase(key, String.class);
        if (data == null) {
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
    @NonNull
    public Integer readIntFromDatabase(String key, @NonNull Integer defaultValue) {
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
    @NonNull
    public Long readLongFromDatabase(String key, @NonNull Long defaultValue) {
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
    @NonNull
    public double readDoubleFromDatabase(String key, @NonNull Double defaultValue) {
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
    @NonNull
    public boolean readBooleanFromDatabase(String key, @NonNull Boolean defaultValue) {
        Boolean data = readFromDatabase(key, Boolean.class);
        if (data == null) {
            return defaultValue;
        } else {
            return data;
        }
    }

    /**
     * Delete data from database, database method, synchronously
     *
     * @param key key
     */
    public boolean deleteFromDatabase(String key) {
        try {
            databaseOperationPreCheck(key);
            synchronized (database) {
                database.get().del(key);
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
    public void deleteFromDatabaseAsync(String key, DataBaseSuccessObserver callback) {
        Observable.just(key)
                .map(new Function<String, Boolean>() {
                    @Override
                    public Boolean apply(String key) throws Exception {
                        return deleteFromDatabase(key);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }

    /**
     * Mass delete from database, asynchronously
     *
     * @param keys     keys
     * @param callback callback
     */
    public void massDeleteFromDatabaseAsync(List<String> keys, DataBaseSuccessObserver callback) {
        Observable.just(keys)
                .map(new Function<List<String>, Boolean>() {
                    @Override
                    public Boolean apply(List<String> keys) throws Exception {
                        if (keys == null)
                            return false;
                        boolean massDeleteSuccess = true;
                        for (String key : keys) {
                            // once failed, consider it as failed
                            if (!deleteFromDatabase(key))
                                massDeleteSuccess = false;
                        }
                        return massDeleteSuccess;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }

    /**
     * Mass delete from database by prefix, asynchronously
     *
     * @param prefix   prefix
     * @param callback callback
     */
    public void massDeleteByPrefixFromDatabaseAsync(String prefix, DataBaseSuccessObserver callback) {
        Observable.just(prefix)
                .map(new Function<String, Boolean>() {
                    @Override
                    public Boolean apply(String prefix) throws Exception {
                        List<String> keys = findKeysByPrefix(prefix);
                        boolean massDeleteSuccess = true;
                        for (String key : keys) {
                            // once failed, consider it as failed
                            if (!deleteFromDatabase(key))
                                massDeleteSuccess = false;
                        }
                        return massDeleteSuccess;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
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
                exist = database.get().exists(key);
            }
        } catch (Exception e) {
            logError("Find key exist failed", e);
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
    @NonNull
    public List<String> findKeysByPrefix(String prefix) {
        String[] keys = null;
        try {
            databaseOperationPreCheck(prefix);
            synchronized (database) {
                keys = database.get().findKeys(prefix);
            }
        } catch (Exception e) {
            logError("Find keys by prefix failed", e);
        }
        if (keys == null) {
            keys = new String[]{};
        }
        log("{ prefix = " + prefix + " } has " + keys.length + " keys");
        return Arrays.asList(keys);
    }

    /**
     * Return keys with same prefix from database, asynchronously
     *
     * @param prefix   prefix
     * @param callback callback
     */
    public void findKeysByPrefix(String prefix, DatabaseListObserver<String> callback) {
        Observable.just(prefix)
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String prefix) throws Exception {
                        return findKeysByPrefix(prefix);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback);
    }


    /**
     * Save data in memory cache, default will be weak reference mode
     *
     * @param key      key
     * @param data     data
     * @param strongly strongly or weak reference
     */
    public void writeInMemory(String key, Object data, boolean strongly) {
        memoryCache.put(key, data, strongly);
        log("{ key = " + key + " data = " + data + " } save in memory finished");
        log("Memory cache size: " + memoryCache.size());
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
        V data = memoryCache.get(key, strongly);
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
        memoryCache.delete(key);
        log("{ key = " + key + " } delete from memory finished");
        log("Memory cache size: " + memoryCache.size());
    }

    /**
     * Memory cache key array
     *
     * @return key array
     */
    public List<String> memoryCacheKeys() {
        return Arrays.asList(memoryCache.keySet());
    }

    /**
     * Clear memory cache
     */
    public void clearMemoryCache() {
        memoryCache.clear();
        log("Memory cache clear finish");
    }

    private void log(String content) {
        if (configuration.logEnabled)
            Log.d("Panther", content);
    }

    private void logError(String content, Throwable error) {
        if (configuration.logEnabled)
            Log.e("Panther", content, error);
    }
}