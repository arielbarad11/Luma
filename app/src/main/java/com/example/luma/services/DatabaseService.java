package com.example.luma.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class DatabaseService {
    private static final String TAG = "DatabaseService";

    private static final String USERS_PATH = "users";
    private static final String Psychologist_PATH = "psychologist";
    private static final String MOOD_HISTORY_PATH = "mood_history";
    private static DatabaseService instance;
    private final DatabaseReference databaseReference;
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://luma-785c3-default-rtdb.europe-west1.firebasedatabase.app/");
        databaseReference = firebaseDatabase.getReference();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // region Private Generic Methods
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });
            callback.onCompleted(tList);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

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
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // region User Section
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }
    // endregion

    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    public void getUserByEmail(String email, DatabaseCallback<User> databaseCallback) {
        getUserList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (user.getEmail().equals(email)) {
                        databaseCallback.onCompleted(user);
                        return;
                    }
                }
                databaseCallback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                databaseCallback.onFailed(e);
            }
        });
    }

    public void checkIfEmailExists(String email, DatabaseCallback<Boolean> databaseCallback) {
        getUserByEmail(email, new DatabaseCallback<>() {
            @Override
            public void onCompleted(User user) {
                databaseCallback.onCompleted(user != null);
            }

            @Override
            public void onFailed(Exception e) {
                databaseCallback.onFailed(e);
            }
        });
    }

    public void getUserByEmailAndPassword(@NotNull final String email, @NotNull final String password, @NotNull final DatabaseCallback<User> callback) {
        getUserList(new DatabaseCallback<>() {
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
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    public void updateUser(@NotNull final String uid, UnaryOperator<User> operator, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(USERS_PATH + "/" + uid, User.class, operator, new DatabaseCallback<>() {
            @Override
            public void onCompleted(User object) {
                if (callback != null) callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    // region Psychologist Section
    public String generatePsychologistId() {
        return generateNewId(Psychologist_PATH);
    }
    // endregion

    public void createNewPsychologist(@NotNull final Psychologist psychologist, @Nullable final DatabaseCallback<Void> callback) {
        writeData(Psychologist_PATH + "/" + psychologist.getId(), psychologist, callback);
    }

    public void getPsychologist(@NotNull final String uid, @NotNull final DatabaseCallback<Psychologist> callback) {
        getData(Psychologist_PATH + "/" + uid, Psychologist.class, callback);
    }

    public void getPsychologistList(@NotNull final DatabaseCallback<List<Psychologist>> callback) {
        getDataList(Psychologist_PATH, Psychologist.class, callback);
    }

    public void updatePsychologist(@NotNull final Psychologist psychologist, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(Psychologist_PATH + "/" + psychologist.getId(), Psychologist.class, current -> psychologist, new DatabaseCallback<>() {
            @Override
            public void onCompleted(Psychologist object) {
                if (callback != null) callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    public void deletePsychologist(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(Psychologist_PATH + "/" + uid, callback);
    }

    // region Mood Tracker Section
    public void saveMoodEntry(@NotNull final MoodEntry entry, @Nullable final DatabaseCallback<Void> callback) {
        writeData(MOOD_HISTORY_PATH + "/" + entry.userId + "/" + entry.id, entry, callback);
    }
    // endregion

    public void getMoodHistory(@NotNull final String userId, @NotNull final DatabaseCallback<List<MoodEntry>> callback) {
        getDataList(MOOD_HISTORY_PATH + "/" + userId, MoodEntry.class, callback);
    }

    public interface DatabaseCallback<T> {
        void onCompleted(T object);

        void onFailed(Exception e);
    }
    // endregion
}