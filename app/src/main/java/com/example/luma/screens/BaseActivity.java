package com.example.luma.screens;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.luma.R;
import com.example.luma.services.DatabaseService;

public class BaseActivity extends AppCompatActivity {

    protected DatabaseService databaseService;
//    protected DrawerLayout drawerLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // שמירת שירות הנתונים
        databaseService = DatabaseService.getInstance();

        // Drawer
//        drawerLayout = findViewById(R.id.drawer_layout);
    }
}
