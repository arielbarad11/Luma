package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

public class SplashActivity extends BaseActivity {

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Thread splashThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }

            runOnUiThread(() -> {
                if (SharedPreferencesUtil.isUserLoggedIn(this)) {

                    User current = SharedPreferencesUtil.getUser(this);
                    if (current != null) {
                        DatabaseService.getInstance()
                                .getUser(current.getId(), new DatabaseService.DatabaseCallback<User>() {

                                    @Override
                                    public void onCompleted(User user) {
                                        if (user != null) {
                                            SharedPreferencesUtil.saveUser(SplashActivity.this, user);

                                            if (user.isAdmin()) {
                                                intent = new Intent(SplashActivity.this, AdminActivity.class);
                                            } else {
                                                intent = new Intent(SplashActivity.this, MainActivity.class);
                                            }
                                        } else {
                                            SharedPreferencesUtil.signOutUser(SplashActivity.this);
                                            intent = new Intent(SplashActivity.this, LandingActivity.class);
                                        }

                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        );
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onFailed(Exception e) {
                                        SharedPreferencesUtil.signOutUser(SplashActivity.this);
                                        intent = new Intent(SplashActivity.this, LandingActivity.class);
                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        );
                                        startActivity(intent);
                                    }
                                });
                    } else {
                        intent = new Intent(SplashActivity.this, LandingActivity.class);
                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );
                        startActivity(intent);
                    }

                } else {
                    intent = new Intent(SplashActivity.this, LandingActivity.class);
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    startActivity(intent);
                }
            });
        });

        splashThread.start();
    }
}
