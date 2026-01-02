package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

public class LandingActivity extends BaseActivity {

    Button toRegister;
    Button toLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User current = SharedPreferencesUtil.getUser(this);
        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            databaseService.getUser(current.getId(), new DatabaseService.DatabaseCallback<User>() {
                @Override
                public void onCompleted(User user) {
                    if (user != null) {
                        Intent intent;
                        if (user.isAdmin()) {
                            intent = new Intent(LandingActivity.this, AdminActivity.class);
                        } else {
                            intent = new Intent(LandingActivity.this, MainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        }

        toRegister = findViewById(R.id.btn_landing_go_to_register);
        toLogin = findViewById(R.id.btn_landing_go_to_login);

        toRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandingActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
        toLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LandingActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}