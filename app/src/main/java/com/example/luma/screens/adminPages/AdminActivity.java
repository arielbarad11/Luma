package com.example.luma.screens.adminPages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.screens.LoginActivity;
import com.example.luma.screens.MainActivity;
import com.example.luma.utils.LogoutHelper;
import com.example.luma.utils.SharedPreferencesUtil;

public class AdminActivity extends AppCompatActivity {

    Button toUsersList;
    Button toAdminPsychologistList;

    Button toMain;
    private Button btnToExit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAdmin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        // ===== refresh user from DB =====
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        toUsersList = findViewById(R.id.btn_admin_go_to_usersList);
        toAdminPsychologistList = findViewById(R.id.btn_admin_go_to_adminPsychologistList);
        toMain = findViewById(R.id.btn_admin_go_to_main);


        // ===== init views =====
        btnToExit = findViewById(R.id.btn_admin_to_exit);

        // ===== listeners =====
        btnToExit.setOnClickListener(v ->
                LogoutHelper.logout(AdminActivity.this)
        );


        toUsersList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, UsersListActivity.class);
                startActivity(intent);
            }
        });
        toAdminPsychologistList.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, AdminPsychologistListActivity.class);
                 startActivity(intent);
             }
        });
        toMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }

        private void redirectToLogin() {
            SharedPreferencesUtil.signOutUser(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }


}