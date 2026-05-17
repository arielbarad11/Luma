package com.example.luma.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.CrisisTime;
import com.example.luma.models.User;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class YourEmergencyPlanActivity extends BaseActivity {

    private LinearLayout llContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_your_emergency_plan);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        llContainer = findViewById(R.id.ll_container);

        loadAndDisplayPlan();
    }

    private void loadAndDisplayPlan() {
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) return;

        databaseService.getUser(currentUser.getId(), new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user == null || user.getCrisisTime() == null || user.getCrisisTime().isEmpty()) {
                    showEmptyState();
                    return;
                }
                buildChecklist(user.getCrisisTime());
            }

            @Override
            public void onFailed(Exception e) {
                showEmptyState();
            }
        });
    }

    private void buildChecklist(ArrayList<String> savedIds) {
        List<CrisisTime.CrisisOption> allOptions = CrisisTime.getDefaultOptions();
        String currentCategory = "";

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);

        for (CrisisTime.CrisisOption option : allOptions) {
            // רק פריטים שהמשתמש בחר
            if (!savedIds.contains(option.getId())) continue;

            // כותרת קטגוריה — רק כשמשתנה
            if (!option.getCategory().equals(currentCategory)) {
                currentCategory = option.getCategory();

                TextView tvCategory = new TextView(this);
                tvCategory.setText(getCategoryEmoji(currentCategory) + " " + currentCategory);
                tvCategory.setTextSize(17f);
                tvCategory.setTextColor(0xFF700053);
                tvCategory.setTypeface(null, android.graphics.Typeface.BOLD);

                LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                catParams.setMargins(0, 20, 0, 10);
                llContainer.addView(tvCategory, catParams);
            }

            // צ'קבוקס
            CheckBox cb = new CheckBox(this);
            cb.setText(option.getEmoji() + "  " + option.getLabel());
            cb.setTextSize(15f);
            cb.setTextColor(0xFF1F2937);
            cb.setPadding(0, 12, 0, 12);
            llContainer.addView(cb, params);
        }
    }

    private String getCategoryEmoji(String category) {
        switch (category) {
            case "הרגעה מיידית":  return "⚡";
            case "תמיכה רגשית":   return "💙";
            case "הסחת דעת":      return "🎯";
            case "עזרה מקצועית":  return "🩺";
            default:              return "";
        }
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("עדיין לא בנית תוכנית חירום 💙\nלכי לעדכון פרטים ובחרי פעולות שעוזרות לך.");
        tv.setTextSize(16f);
        tv.setTextColor(0xFF6B7280);
        tv.setLineSpacing(8f, 1f);
        llContainer.addView(tv);
    }
}