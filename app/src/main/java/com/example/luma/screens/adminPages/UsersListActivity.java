package com.example.luma.screens.adminPages;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;

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

        // 🔐 בדיקה: רק אדמין יכול להיכנס למסך
        User user = SharedPreferencesUtil.getUser(this);
        if (user == null || !user.isAdmin()) {
            finish(); // לא אדמין → יוצאים מהמסך
            return;
        }

        // חיבור רכיבי UI
        RecyclerView usersList = findViewById(R.id.rv_users_list);
        tvUserCount = findViewById(R.id.tv_user_count);

        usersList.setLayoutManager(new LinearLayoutManager(this));

        // יצירת Adapter עם מאזינים ללחיצה ארוכה
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

        // טעינת רשימת משתמשים מה־DB
        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                userAdapter.setUserList(users);
                tvUserCount.setText("Total users: " + users.size());
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to get users list", e);
            }
        });
    }

    // =======================
    // פעולות מנהל (Long Click)
    // =======================

    private void showAdminActionsDialog(User user) {

        String[] options;
        if (user.isAdmin()) {
            options = new String[]{"Remove admin", "Delete user"};
        } else {
            options = new String[]{"Make admin", "Delete user"};
        }

        new AlertDialog.Builder(this)
                .setTitle(user.getFirstName())
                .setItems(options, (dialog, which) -> {

                    String choice = options[which];

                    if (choice.equals("Make admin")) {
                        makeAdmin(user);
                    } else if (choice.equals("Remove admin")) {
                        removeAdmin(user);
                    } else if (choice.equals("Delete user")) {
                        confirmDeleteUser(user);
                    }
                })
                .show();
    }

    private void makeAdmin(User user) {
        databaseService.updateUserAdminStatus(user.getId(), true, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) { }

            @Override
            public void onFailed(Exception e) { }
        });
    }

    private void removeAdmin(User user) {
        databaseService.updateUserAdminStatus(user.getId(), false, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) { }

            @Override
            public void onFailed(Exception e) { }
        });
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete user")
                .setMessage("Are you sure you want to delete " + user.getFirstName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        databaseService.deleteUser(user.getId(), new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void object) {
                                userAdapter.removeUser(user);
                            }

                            @Override
                            public void onFailed(Exception e) { }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
