package com.example.luma.screens.adminPages;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.adapters.UserAdapter;
import com.example.luma.models.User;
import com.example.luma.screens.BaseActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.List;

public class UsersListActivity extends BaseActivity {

    private static final String TAG = "UsersListActivity";

    private UserAdapter userAdapter;
    private TextView tvUserCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list);

        // התאמה ל־system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.users_list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔐 רק אדמין יכול להיכנס למסך
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            finish();
            return;
        }

        RecyclerView usersList = findViewById(R.id.rv_users_list);
        tvUserCount = findViewById(R.id.tv_user_count);

        usersList.setLayoutManager(new LinearLayoutManager(this));

        // יצירת Adapter
        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                Log.d(TAG, "User clicked: " + user.getId());
            }

            @Override
            public void onLongUserClick(User user) {
                showAdminActionsDialog(user);
            }

            @Override
            public boolean showAdminChip(User user) {
                return user.isAdmin();
            }

            @Override
            public boolean showRemoveAdminBtn(User user) {
                return user.isAdmin();
            }

            @Override
            public boolean showMakeAdminBtn(User user) {
                return (!user.isAdmin());
            }
        });

        usersList.setAdapter(userAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // טעינת משתמשים מה־DB
        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                userAdapter.setUserList(users);
                tvUserCount.setText("מספר המשתמשים שלנו: " + users.size());
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load users", e);
            }
        });
    }

    // =======================
    // תפריט פעולות אדמין
    // =======================
    private void showAdminActionsDialog(User user) {

        String[] options = user.isAdmin()
                ? new String[]{"הסרת הרשאת מנהל", "מחיקת המנהל"}
                : new String[]{"הפוך למנהל", "מחיקת המשתמש"};

        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle(user.getFirstName())
                .setItems(options, (dialog, which) -> {

                    String choice = options[which];

                    if (choice.equals("הפוך למנהל")) {
                        makeAdmin(user);
                    } else if (choice.equals("הסרת הרשאת מנהל")) {
                        removeAdmin(user);
                    } else {
                        confirmDeleteUser(user);
                    }
                })
                .show();
    }

    // =======================
    // פעולות DB + UI
    // =======================

    private void makeAdmin(User user) {
        databaseService.updateUser(user.getId(), user1 -> {
                    if (user1 == null) return null;
                    user1.setAdmin(true);
                    return user1;
                },
                new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(Void object) {
                        // ✅ עדכון מיידי של UI
                        user.setAdmin(true);
                        userAdapter.updateUser(user);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "הרשאת מנהל ליוזר ננכשלה", e);
                    }
                });
    }

    private void removeAdmin(User user) {
        if (user.getId().equals(SharedPreferencesUtil.getUserId(this))) {
            Toast.makeText(this, "לא יכול להסיר את הרשאת המנהל של היוזר הזה", Toast.LENGTH_SHORT).show();
            return;
        }
        databaseService.updateUser(user.getId(), user1 -> {
                    if (user1 == null) return null;
                    user1.setAdmin(false);
                    return user1;
                },
                new DatabaseService.DatabaseCallback<>() {
                    @Override
                    public void onCompleted(Void object) {
                        // ✅ עדכון מיידי של UI
                        user.setAdmin(false);
                        userAdapter.updateUser(user);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "הסרת המנהל נכשלה", e);
                    }
                });
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("מחק משתמש")
                .setMessage("בטוח שאתה רוצה למחוק את " + user.getFirstName() + "?")
                .setPositiveButton("מחק", (dialog, which) -> databaseService.deleteUser(user.getId(),
                        new DatabaseService.DatabaseCallback<>() {
                            @Override
                            public void onCompleted(Void object) {
                                // ✅ הסרה מיידית מהרשימה
                                userAdapter.removeUser(user);
                            }

                            @Override
                            public void onFailed(Exception e) {
                                Log.e(TAG, "מחיקת המשתמש נכשלה", e);
                            }
                        }))
                .setNegativeButton("ביטול", null)
                .show();
    }
}
