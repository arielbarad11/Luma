package com.example.luma.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.luma.models.Goal;
import com.example.luma.models.MoodEntry;
import com.example.luma.models.Psychologist;
import com.example.luma.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * DatabaseService - מחלקת השירות לניהול כל התקשורת מול Firebase Realtime Database.
 * המחלקה משתמשת בתבנית עיצוב Singleton (מופע יחיד) כדי לרכז את כל הפעולות במקום אחד.
 */
public class DatabaseService {
    private static final String TAG = "DatabaseService";

    // שמות התיקיות (Nodes) בתוך מסד הנתונים
    private static final String USERS_PATH = "users";
    private static final String Psychologist_PATH = "psychologist";
    private static final String MOOD_HISTORY_PATH = "mood_history";
    private static final String GOALS_PATH = "goals";

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    // בנאי פרטי (Private Constructor) כחלק מתבנית ה-Singleton
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://luma-785c3-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();
    }

    /**
     * מחזיר את המופע היחיד של ה-DatabaseService.
     */
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // ───────────── מתודות גנריות (לשימוש פנימי) ─────────────

    /**
     * כתיבת נתונים לנתיב ספציפי ב-Database.
     */
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    /**
     * מחיקת נתונים מנתיב ספציפי ב-Database.
     */
    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    /**
     * מחזירה הפניה (Reference) לנתיב מסוים.
     */
    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    /**
     * שליפת אובייקט בודד מנתיב מסוים.
     */
    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    /**
     * שליפת רשימה של אובייקטים מנתיב מסוים.
     */
    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                if (t != null) tList.add(t);
            });
            callback.onCompleted(tList);
        });
    }

    /**
     * מייצר מזהה ייחודי (ID) חדש עבור נתיב מסוים.
     */
    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    /**
     * הרצת טרנזקציה (Transaction) - משמש לעדכון בטוח של נתונים, מונע דריסת נתונים אם שני משתמשים מעדכנים בו-זמנית.
     */
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                currentValue = (currentValue == null) ? function.apply(null) : function.apply(currentValue);
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // ───────────── ניהול משתמשים (User Section) ─────────────

    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    public void getUsersList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    /**
     * חיפוש משתמש לפי כתובת אימייל.
     */
    public void getUserByEmail(String email, DatabaseCallback<User> callback) {
        getUsersList(new DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (user.getEmail().equalsIgnoreCase(email)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null); // המשתמש לא נמצא
            }
            @Override
            public void onFailed(Exception e) { callback.onFailed(e); }
        });
    }

    /**
     * בדיקה האם אימייל קיים כבר במערכת.
     */
    public void checkIfEmailExists(String email, DatabaseCallback<Boolean> callback) {
        getUserByEmail(email, new DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) { callback.onCompleted(user != null); }
            @Override
            public void onFailed(Exception e) { callback.onFailed(e); }
        });
    }

    /**
     * התחברות: בדיקת התאמה בין אימייל לסיסמה.
     */
    public void getUserByEmailAndPassword(@NotNull final String email, @NotNull final String password, @NotNull final DatabaseCallback<User> callback) {
        getUsersList(new DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email) && Objects.equals(user.getPassword(), password)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null);
            }
            @Override
            public void onFailed(Exception e) { callback.onFailed(e); }
        });
    }

    /**
     * עדכון פרטי משתמש באמצעות טרנזקציה.
     */
    public void updateUser(@NotNull final String uid, UnaryOperator<User> operator, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(USERS_PATH + "/" + uid, User.class, operator, new DatabaseCallback<User>() {
            @Override
            public void onCompleted(User object) { if (callback != null) callback.onCompleted(null); }
            @Override
            public void onFailed(Exception e) { if (callback != null) callback.onFailed(e); }
        });
    }

    // ───────────── ניהול פסיכולוגים (Psychologist Section) ─────────────

    public String generatePsychologistId() { return generateNewId(Psychologist_PATH); }

    public void createNewPsychologist(Psychologist p, DatabaseCallback<Void> cb) { writeData(Psychologist_PATH + "/" + p.getId(), p, cb); }

    public void getPsychologistList(DatabaseCallback<List<Psychologist>> cb) { getDataList(Psychologist_PATH, Psychologist.class, cb); }

    public void updatePsychologist(Psychologist p, DatabaseCallback<Void> cb) { writeData(Psychologist_PATH + "/" + p.getId(), p, cb); }

    public void deletePsychologist(String id, DatabaseCallback<Void> cb) { deleteData(Psychologist_PATH + "/" + id, cb); }

    // ───────────── מעקב מצב רוח (Mood Tracker Section) ─────────────

    public void saveMoodEntry(MoodEntry entry, DatabaseCallback<Void> cb) { writeData(MOOD_HISTORY_PATH + "/" + entry.userId + "/" + entry.id, entry, cb); }

    public void getMoodHistory(String userId, DatabaseCallback<List<MoodEntry>> cb) { getDataList(MOOD_HISTORY_PATH + "/" + userId, MoodEntry.class, cb); }

    // ───────────── ניהול מטרות (Goals Section) ─────────────

    public void saveGoal(String userId, Goal goal, DatabaseCallback<Void> cb) { writeData(USERS_PATH + "/" + userId + "/" + GOALS_PATH + "/" + goal.getId(), goal, cb); }

    public void getGoals(String userId, DatabaseCallback<List<Goal>> cb) { getDataList(USERS_PATH + "/" + userId + "/" + GOALS_PATH, Goal.class, cb); }

    public void deleteGoal(String userId, String goalId, DatabaseCallback<Void> cb) { deleteData(USERS_PATH + "/" + userId + "/" + GOALS_PATH + "/" + goalId, cb); }

    /**
     * ממשק (Interface) המשמש כ-Callback לקבלת תשובות מה-Database בצורה אסינכרונית.
     */
    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }
}