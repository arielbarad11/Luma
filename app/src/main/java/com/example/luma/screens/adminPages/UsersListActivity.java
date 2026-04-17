package com.example.luma.screens.adminPages;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
import com.example.luma.screens.LandingActivity;
import com.example.luma.screens.UpdateUserActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * UsersListActivity - מסך ניהול משתמשים (עבור ה-Admin).
 * מציג את כל המשתמשים הרשומים ב-Firebase ומאפשר חיפוש ועריכה שלהם.
 */
public class UsersListActivity extends BaseActivity {

    private static final String TAG = "UsersListActivity";


    private UserAdapter userAdapter;
    private TextView tvUserCount;
    private ProgressBar progressBar;
    private List<User> fullList = new ArrayList<>(); // רשימה מלאה לסינון מהיר (חיפוש)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list);

        // הגדרת Padding אוטומטי למניעת חפיפה עם שולי המסך
        View mainView = findViewById(R.id.users_list);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // אבטחה: וידוא שהמשתמש הוא אכן אדמין
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            directToLanding();
            return;
        }

        // אתחול רכיבי הממשק מה-XML
        tvUserCount = findViewById(R.id.tv_user_count);
        progressBar = findViewById(R.id.progress_bar);
        RecyclerView rvUsers = findViewById(R.id.rv_users_list);
        
        if (rvUsers != null) {
            rvUsers.setLayoutManager(new LinearLayoutManager(this));
        }

        // כפתור חזור
        View btnBack = findViewById(R.id.btn_back_users);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // הגדרת ה-Adapter עם מימוש מלא של הממשק OnUserClickListener
        userAdapter = new UserAdapter(new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // מעבר למסך עדכון משתמש עם ה-id של המשתמש שנבחר
                Intent intent = new Intent(UsersListActivity.this, UpdateUserActivity.class);
                intent.putExtra("USER_UID", user.id);
                startActivity(intent);
            }

            @Override
            public void onLongUserClick(User user) {
                showAdminActionsDialog(user);
            }

            @Override
            public boolean showAdminChip(User user) {
                return user.admin; // הצגת תג Admin אם המשתמש הוא מנהל
            }

            @Override
            public boolean showRemoveAdminBtn(User user) {
                return user.admin;
            }

            @Override
            public boolean showMakeAdminBtn(User user) {
                return !user.admin;
            }
        });
        
        if (rvUsers != null) {
            rvUsers.setAdapter(userAdapter);
        }

        // הגדרת חיפוש משתמשים בזמן אמת
        TextInputEditText etSearch = findViewById(R.id.et_search_user);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable e) {}
                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    filterList(s.toString());
                }
            });
        }

        // טעינה ראשונית של המשתמשים
        loadUsers();
    }

    /**
     * שליפת כל המשתמשים מה-Database.
     */
    private void loadUsers() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        DatabaseService.getInstance().getUsersList(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                fullList = (users != null) ? new ArrayList<>(users) : new ArrayList<>();
                userAdapter.setUserList(fullList); // שימוש במתודה הנכונה של ה-Adapter
                if (tvUserCount != null) {
                    tvUserCount.setText("מספר המשתמשים שלנו: " + fullList.size());
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(UsersListActivity.this, "טעינת המשתמשים נכשלה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * סינון הרשימה (חיפוש) בזמן אמת.
     */
    private void filterList(String query) {
        if (query.isEmpty()) {
            userAdapter.setUserList(fullList);
        } else {
            List<User> filtered = new ArrayList<>();
            String lower = query.toLowerCase();
            for (User u : fullList) {
                // בדיקה בטוחה של שם ואימייל
                boolean nameMatch = u.firstName != null && u.firstName.toLowerCase().contains(lower);
                boolean emailMatch = u.email != null && u.email.toLowerCase().contains(lower);
                if (nameMatch || emailMatch) {
                    filtered.add(u);
                }
            }
            userAdapter.setUserList(filtered);
        }
        if (tvUserCount != null) {
            tvUserCount.setText("מספר המשתמשים שלנו: " + userAdapter.getItemCount());
        }
    }

    /**
     * ניתוק והפניה למסך הנחיתה במקרה של אי הרשאה.
     */
    private void directToLanding() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


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