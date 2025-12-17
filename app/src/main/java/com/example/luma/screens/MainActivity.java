package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.LogoutHelper;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Button btnToExit;
    private TextView btnToUpdateUser;
    private TextView txtHomePageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> insets
        );

        // ===== init views =====
        btnToExit = findViewById(R.id.btn_main_to_exit);
        btnToUpdateUser = findViewById(R.id.tv_main_UpdateUser);

        // ===== listeners =====
        btnToExit.setOnClickListener(v ->
                LogoutHelper.logout(MainActivity.this)
        );

        btnToUpdateUser.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UpdateUserActivity.class);
            startActivity(intent);
        });

        // ===== refresh user from DB =====
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        DatabaseService.getInstance().getUser(
                userId,
                new DatabaseService.DatabaseCallback<User>() {
                    @Override
                    public void onCompleted(User updatedUser) {
                        if (updatedUser == null) {
                            redirectToLogin();
                            return;
                        }
                        SharedPreferencesUtil.saveUser(MainActivity.this, updatedUser);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        redirectToLogin();
                    }
                }
        );
    }

    private void redirectToLogin() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
