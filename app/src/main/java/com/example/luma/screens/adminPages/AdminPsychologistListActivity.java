package com.example.luma.screens.adminPages;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.example.luma.adapters.PsychologistAdapter;
import com.example.luma.models.Psychologist;
import com.example.luma.models.User;
import com.example.luma.screens.BaseActivity;
import com.example.luma.screens.LandingActivity;
import com.example.luma.screens.LoginActivity;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AdminPsychologistListActivity - מסך ניהול פסיכולוגים (עבור ה-Admin).
 * מאפשר למנהל המערכת לצפות, להוסיף, לערוך ולמחוק פסיכולוגים מהמאגר.
 */
public class AdminPsychologistListActivity extends BaseActivity {

    private static final String TAG = "AdminPsychologistListActivity";

    // רכיבי ממשק המשתמש
    private PsychologistAdapter psychologistAdapter;
    private TextView tvPsychologistCount;
    private TextView tvSort;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private List<Psychologist> fullList = new ArrayList<>(); // רשימה מלאה לסינון וחיפוש מהיר
    private boolean sortedAsc = true; // דגל לכיוון המיון (עולה/יורד)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_psychologist_list);

        // הגדרת Padding למניעת חפיפה עם שולי המסך
        View mainLayout = findViewById(R.id.admin_psychologist_list);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
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

        // אתחול רכיבי ה-UI
        tvPsychologistCount = findViewById(R.id.tv_item_psychologist_count);
        tvSort              = findViewById(R.id.tv_sort_by_price);
        emptyState          = findViewById(R.id.ll_empty_state);
        progressBar         = findViewById(R.id.pb_loading);

        RecyclerView psychologistList = findViewById(R.id.rv_admin_psychologist_list);
        if (psychologistList != null) {
            psychologistList.setLayoutManager(new LinearLayoutManager(this));
        }

        // הגדרת ה-Adapter וטיפול באירועי לחיצה
        psychologistAdapter = new PsychologistAdapter(
                new PsychologistAdapter.OnClickListener() {
                    @Override
                    public void onClick(Psychologist psychologist) {
                        // לחיצה רגילה - הצגת כרטיסיית הפסיכולוג בתצוגת משתמש
                        showUserViewDialog(psychologist);
                    }

                    @Override
                    public void onLongClick(Psychologist psychologist) {
                        // לחיצה ארוכה - פתיחת תפריט פעולות ניהול (עריכה/מחיקה)
                        showAdminActionsDialog(psychologist);
                    }

                    @Override
                    public void onEmailCLick(Psychologist psychologist) {
                        sendEmail(psychologist);
                    }
                }
        );

        if (psychologistList != null) psychologistList.setAdapter(psychologistAdapter);

        // כפתור חזור
        ImageButton btnBack = findViewById(R.id.btn_back_psychologist_to_mainAdmin);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // כפתור הוספת פסיכולוג חדש
        TextView tvAddPsychologist = findViewById(R.id.tv_add_item_psychologist);
        if (tvAddPsychologist != null) tvAddPsychologist.setOnClickListener(v -> showAddPsychologistDialog());

        // הגדרת חיפוש דינמי לפי שם או עיר
        TextInputEditText etSearch = findViewById(R.id.et_search_psychologist);
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

        // הגדרת מיון לפי מחיר
        if (tvSort != null) {
            tvSort.setOnClickListener(v -> {
                sortedAsc = !sortedAsc;
                tvSort.setText(sortedAsc ? "מחיר ↑" : "מחיר ↓");
                sortList();
            });
        }
    }

    /**
     * משיכת כל הפסיכולוגים מ-Firebase.
     */
    private void loadPsychologists() {
        showLoading(true);
        databaseService.getPsychologistList(new DatabaseService.DatabaseCallback<List<Psychologist>>() {
            @Override
            public void onCompleted(List<Psychologist> psychologists) {
                showLoading(false);
                fullList = new ArrayList<>(psychologists);
                psychologistAdapter.setList(psychologists);
                updateUIState();
            }

            @Override
            public void onFailed(Exception e) {
                showLoading(false);
                Toast.makeText(AdminPsychologistListActivity.this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * תפריט פעולות ניהול (מופעל בלחיצה ארוכה על פריט ברשימה).
     */
    private void showAdminActionsDialog(Psychologist psychologist) {
        String[] options = {"צפה בכרטיסייה (תצוגת משתמש)", "ערוך פרטי פסיכולוג", "מחק פסיכולוג"};
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle(psychologist.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showUserViewDialog(psychologist);
                    else if (which == 1) editPsychologist(psychologist);
                    else confirmDeletePsychologist(psychologist);
                })
                .show();
    }

    /**
     * עריכת פסיכולוג קיים - פתיחת דיאלוג עם השדות הנוכחיים לעדכון.
     */
    private void editPsychologist(Psychologist psychologist) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_psychologist, null);
        EditText etName = dialogView.findViewById(R.id.et_psychologist_name);
        EditText etEmail = dialogView.findViewById(R.id.et_psychologist_email);
        EditText etCity = dialogView.findViewById(R.id.et_psychologist_city);
        EditText etPrice = dialogView.findViewById(R.id.et_psychologist_price);

        // מילוי השדות בנתונים הקיימים
        etName.setText(psychologist.getName());
        etEmail.setText(psychologist.getEmail());
        etCity.setText(psychologist.getCity());
        etPrice.setText(String.valueOf(psychologist.getSessionPrice()));

        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("ערוך פסיכולוג")
                .setView(dialogView)
                .setPositiveButton("עדכן", (dialog, which) -> {
                    // עדכון אובייקט הפסיכולוג ושליחה לשרת
                    psychologist.setName(etName.getText().toString());
                    psychologist.setEmail(etEmail.getText().toString());
                    psychologist.setCity(etCity.getText().toString());
                    String stPrice = etPrice.getText().toString();
                    psychologist.setSessionPrice(stPrice.isEmpty() ? 0 : Integer.parseInt(stPrice));

                    databaseService.updatePsychologist(psychologist, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void result) {
                            psychologistAdapter.update(psychologist); // עדכון מקומי ברשימה
                            Toast.makeText(AdminPsychologistListActivity.this, "הנתונים עודכנו", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailed(Exception e) { Log.e(TAG, "עריכה נכשלה", e); }
                    });
                })
                .setNegativeButton("בטל", null)
                .show();
    }

    /**
     * מחיקת פסיכולוג - פנייה ל-Firebase ועדכון ה-UI.
     */
    private void confirmDeletePsychologist(Psychologist psychologist) {
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("מחיקת פסיכולוג")
                .setMessage("האם אתה בטוח שברצונך למחוק את " + psychologist.getName() + "?")
                .setPositiveButton("מחק", (dialog, which) ->
                        databaseService.deletePsychologist(psychologist.getId(), new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void object) {
                                fullList.remove(psychologist);
                                psychologistAdapter.remove(psychologist);
                                updateUIState();
                            }
                            @Override
                            public void onFailed(Exception e) { Log.e(TAG, "מחיקה נכשלה", e); }
                        }))
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * הוספת פסיכולוג חדש - שלב 1: הזנת נתונים.
     */
    private void showAddPsychologistDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_psychologist, null);
        EditText etName = dialogView.findViewById(R.id.et_psychologist_name);
        EditText etEmail = dialogView.findViewById(R.id.et_psychologist_email);
        EditText etCity = dialogView.findViewById(R.id.et_psychologist_city);
        EditText etPrice = dialogView.findViewById(R.id.et_psychologist_price);

        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("הוסף פסיכולוג")
                .setView(dialogView)
                .setPositiveButton("המשך", (dialog, which) -> {
                    Psychologist psychologist = new Psychologist();
                    psychologist.setId(DatabaseService.getInstance().generatePsychologistId());
                    psychologist.setName(etName.getText().toString());
                    psychologist.setEmail(etEmail.getText().toString());
                    psychologist.setCity(etCity.getText().toString());
                    String stPrice = etPrice.getText().toString();
                    psychologist.setSessionPrice(stPrice.isEmpty() ? 0 : Integer.parseInt(stPrice));

                    showConfirmAddPsychologistDialog(psychologist); // מעבר לאישור סופי
                })
                .setNegativeButton("בטל", null)
                .show();
    }

    private void showConfirmAddPsychologistDialog(Psychologist psychologist) {
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("אשר פרטים")
                .setMessage("שם: " + psychologist.getName() + "\nאימייל: " + psychologist.getEmail())
                .setPositiveButton("הוסף", (dialog, which) -> addPsychologist(psychologist))
                .setNegativeButton("בטל", null)
                .show();
    }

    private void addPsychologist(Psychologist psychologist) {
        databaseService.createNewPsychologist(psychologist, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                fullList.add(psychologist);
                psychologistAdapter.add(psychologist);
                updateUIState();
            }
            @Override
            public void onFailed(Exception e) { Log.e(TAG, "הוספה נכשלה", e); }
        });
    }

    // פונקציות עזר לסינון, מיון ועדכון UI
    private void filterList(String query) {
        if (query.isEmpty()) psychologistAdapter.setList(fullList);
        else {
            List<Psychologist> filtered = new ArrayList<>();
            String lower = query.toLowerCase();
            for (Psychologist p : fullList) {
                if (p.getName().toLowerCase().contains(lower) || p.getCity().toLowerCase().contains(lower)) filtered.add(p);
            }
            psychologistAdapter.setList(filtered);
        }
        updateUIState();
    }

    private void sortList() {
        List<Psychologist> sorted = new ArrayList<>(fullList);
        sorted.sort(sortedAsc ? Comparator.comparingInt(Psychologist::getSessionPrice) : Comparator.comparingInt(Psychologist::getSessionPrice).reversed());
        psychologistAdapter.setList(sorted);
    }

    private void updateUIState() {
        tvPsychologistCount.setText("מספר הפסיכולוגים: " + psychologistAdapter.getItemCount());
        emptyState.setVisibility(psychologistAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showUserViewDialog(Psychologist p) {
        Dialog d = new Dialog(this);
        d.setContentView(R.layout.dialog_contact_psychologist);
        ((TextView)d.findViewById(R.id.tv_dialog_psychologist_name)).setText(p.getName());
        d.findViewById(R.id.btn_dialog_close).setOnClickListener(v -> d.dismiss());
        d.findViewById(R.id.btn_dialog_send_email).setOnClickListener(v -> { sendEmail(p); d.dismiss(); });
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    private void sendEmail(Psychologist p) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{p.getEmail()});
        try { startActivity(Intent.createChooser(intent, "שלח אימייל...")); } catch (Exception ex) {}
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
}