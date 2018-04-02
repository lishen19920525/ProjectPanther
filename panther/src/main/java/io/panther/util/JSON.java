package io.panther.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiShen on 2018/4/2.
 * Fake fastjson with Gson
 */

public class JSON {
    private static final Gson GSON = new Gson();

    public static String toJSONString(Object obj) {
        try {
            return GSON.toJson(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] parseArray(String json, Class<T> clazz) {
        try {
            JsonObject[] jsonObjects = GSON.fromJson(json, new TypeToken<JsonObject[]>() {
            }.getType());
            List<T> list = new ArrayList<>();
            for (JsonObject jsonObject : jsonObjects) {
                list.add(GSON.fromJson(jsonObject, clazz));
            }
            T[] ts = (T[]) Array.newInstance(clazz, list.size());
            return list.toArray(ts);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}