package com.example.luma.screens.adminPages;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
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
        });

        usersList.setAdapter(userAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // טעינת משתמשים מה־DB
        databaseService.getUserList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                userAdapter.setUserList(users);
                tvUserCount.setText("Total users: " + users.size());
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
                ? new String[]{"Remove admin", "Delete admin"}
                : new String[]{"Make admin", "Delete user"};

        new AlertDialog.Builder(this)
                .setTitle(user.getFirstName())
                .setItems(options, (dialog, which) -> {

                    String choice = options[which];

                    if (choice.equals("Make admin")) {
                        makeAdmin(user);
                    } else if (choice.equals("Remove admin")) {
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
        databaseService.updateUserAdminStatus(user.getId(), true,
                new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        // ✅ עדכון מיידי של UI
                        user.setAdmin(true);
                        userAdapter.updateUser(user);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Make admin failed", e);
                    }
                });
    }

    private void removeAdmin(User user) {
        databaseService.updateUserAdminStatus(user.getId(), false,
                new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        // ✅ עדכון מיידי של UI
                        user.setAdmin(false);
                        userAdapter.updateUser(user);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Remove admin failed", e);
                    }
                });
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete user")
                .setMessage("Are you sure you want to delete " + user.getFirstName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseService.deleteUser(user.getId(),
                                new DatabaseService.DatabaseCallback<Void>() {
                                    @Override
                                    public void onCompleted(Void object) {
                                        // ✅ הסרה מיידית מהרשימה
                                        userAdapter.removeUser(user);
                                    }

                                    @Override
                                    public void onFailed(Exception e) {
                                        Log.e(TAG, "Delete user failed", e);
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
