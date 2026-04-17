package com.example.luma.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.luma.models.User;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * SharedPreferencesUtil - כלי עזר לשמירה ושליפה של נתונים מקומיים בתוך המכשיר.
 * משמש בעיקר לשמירת נתוני המשתמש המחובר כדי שלא יצטרך לבצע התחברות בכל פעם.
 */
public class SharedPreferencesUtil {

    private static final String PREF_NAME = "com.example.luma.PREFERENCE_FILE_KEY";

    private static void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private static String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private static void remove(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    private static boolean contains(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.contains(key);
    }

    /**
     * שמירת אובייקט (כמו User) על ידי הפיכתו למחרוזת JSON.
     */
    private static <T> void saveObject(Context context, String key, T object) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        saveString(context, key, json);
    }

    /**
     * שליפת אובייקט וסידורו חזרה ממחרוזת JSON.
     */
    private static <T> T getObject(Context context, String key, Class<T> type) {
        String json = getString(context, key, null);
        if (json == null) return null;
        
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            // אם המבנה של ה-JSON ישן או לא תקין, ננקה אותו כדי למנוע קריסות
            Log.e("SharedPreferencesUtil", "Error parsing JSON, clearing old data", e);
            remove(context, key);
            return null;
        }
    }

    public static void saveUser(Context context, User user) {
        saveObject(context, "user", user);
    }

    public static User getUser(Context context) {
        if (!isUserLoggedIn(context)) return null;
        return getObject(context, "user", User.class);
    }

    public static void signOutUser(Context context) {
        remove(context, "user");
    }

    public static boolean isUserLoggedIn(Context context) {
        return contains(context, "user");
    }

    @Nullable
    public static String getUserId(Context context) {
        User user = getUser(context);
        if (user != null) return user.id;
        return null;
    }
}