package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.adminPages.AdminActivity;
import com.example.luma.screens.simulators.BreathingSimulationActivity;
import com.example.luma.screens.simulators.MoodTrackerActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.LogoutHelper;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button btnToAdmin;
    private TextView tvHelloUser; //(tvHelloUser)-> משתנה לברכה האישית
    private View vAdminLine;
    private View adminCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // מאפשר פריסה על כל המסך (EdgeToEdge)
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // תיקון החפיפה: מוסיף Padding אוטומטי כדי שהתוכן לא יחפוף עם השעה והסוללה (System Bars)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // אתחול רכיבים
        tvHelloUser = findViewById(R.id.helloUser);
        Button btnToExit = findViewById(R.id.btn_main_to_exit);
        Button btnToUpdateUser = findViewById(R.id.tv_main_UpdateUser);
        Button btnToPsychologists = findViewById(R.id.btn_main_to_PsychologistList);
        Button btnToBreathingSimulation = findViewById(R.id.btn_main_to_BreathingSimulation);
        Button btnToGoal = findViewById(R.id.btn_main_to_Goals);
        btnToAdmin = findViewById(R.id.btn_main_to_admin);
        vAdminLine = findViewById(R.id.adminLine);
        adminCard = findViewById(R.id.adminCard);
        
        Button btnToMoodTracker = findViewById(R.id.btn_main_to_MoodTracker);
        Button btnToMusic = findViewById(R.id.btn_main_to_Music);

        // האזנה לכפתורים - מעברים בין מסכים
        btnToExit.setOnClickListener(v -> LogoutHelper.logout(MainActivity.this));

        btnToUpdateUser.setOnClickListener(v -> startActivity(new Intent(this, UpdateUserActivity.class)));

        btnToPsychologists.setOnClickListener(v -> startActivity(new Intent(this, PsychologistListActivity.class)));

        btnToBreathingSimulation.setOnClickListener(v -> startActivity(new Intent(this, BreathingSimulationActivity.class)));

        btnToAdmin.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));

        btnToMoodTracker.setOnClickListener(v -> startActivity(new Intent(this, MoodTrackerActivity.class)));

        btnToGoal.setOnClickListener(v -> startActivity(new Intent(this, GoalActivity.class)));
        
        if (btnToMusic != null) {
            btnToMusic.setOnClickListener(v -> startActivity(new Intent(this, MediaPlayerActivity.class)));
        }

        // בדיקת סטטוס משתמש ועדכון הרשאות
        checkUserStatus();
    }

    private void checkUserStatus() {
        String userId = SharedPreferencesUtil.getUserId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        DatabaseService.getInstance().getUser(userId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User updatedUser) {
                if (updatedUser == null) {
                    redirectToLogin();
                    return;
                }

                // שמירת המשתמש המעודכן
                SharedPreferencesUtil.saveUser(MainActivity.this, updatedUser);

                // עדכון הברכה האישית
                updateGreeting(updatedUser.getFirstName());

                // לוגיקת כפתור אדמין: מוצג רק אם המשתמש הוא מנהל
                int visibility = updatedUser.isAdmin() ? View.VISIBLE : View.GONE;
                if (adminCard != null) adminCard.setVisibility(visibility);
                if (vAdminLine != null) vAdminLine.setVisibility(visibility);
            }

            @Override
            public void onFailed(Exception e) {
                redirectToLogin();
            }
        });
    }

    /**
     * פונקציה המעדכנת את ה-TextView בברכה מתאימה לפי השעה ביום
     */
    private void updateGreeting(String firstName) {
        if (tvHelloUser == null) return;

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "בוקר טוב, ";
        } else if (hour >= 12 && hour < 18) {
            greeting = "צהריים טובים, ";
        } else if (hour >= 18 && hour < 23) {
            greeting = "ערב טוב, ";
        } else {
            greeting = "לילה טוב, ";
        }

        tvHelloUser.setText(String.format("%s%s", greeting, firstName));
    }

    private void redirectToLogin() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}