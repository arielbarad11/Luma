package com.example.luma.screens.adminPages;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.adapters.UserAdapter;
import com.example.luma.models.User;
import com.example.luma.screens.BaseActivity;
import com.example.luma.screens.UpdateUserActivity;
import com.example.luma.services.DatabaseService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * UsersListActivity - מסך ניהול משתמשים (עבור ה-Admin).
 * מציג את כל המשתמשים הרשומים ב-Firebase ומאפשר חיפוש ועריכה שלהם.
 */
public class UsersListActivity extends BaseActivity {

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
                // לחיצה ארוכה - ניתן להוסיף כאן פעולות נוספות
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
}