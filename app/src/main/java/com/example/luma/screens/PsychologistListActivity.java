package com.example.luma.screens;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.adapters.PsychologistAdapter;
import com.example.luma.models.Psychologist;
import com.example.luma.services.DatabaseService;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PsychologistListActivity extends BaseActivity {

    private static final String TAG = "UserPsychologistList";

    private PsychologistAdapter psychologistAdapter;
    private TextView tvPsychologistCount;
    private TextView tvSort;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private List<Psychologist> fullList = new ArrayList<>();
    private boolean sortedAsc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_psychologist_list);

        // ── Views ──
        tvPsychologistCount = findViewById(R.id.tv_item_psychologist_count);
        tvSort              = findViewById(R.id.tv_sort_by_price);
        emptyState          = findViewById(R.id.empty_state);
        progressBar         = findViewById(R.id.progress_bar);

        RecyclerView rvPsychologists = findViewById(R.id.rv_psychologist_list);
        rvPsychologists.setLayoutManager(new LinearLayoutManager(this));

        // ── כפתור חזור ──
        ImageButton btnBack = findViewById(R.id.btn_back_psychologist);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ── Adapter ──
        psychologistAdapter = new PsychologistAdapter(
                new PsychologistAdapter.OnClickListener() {
                    @Override
                    public void onClick(Psychologist psychologist) {
                        // ✅ קריאה לדיאלוג המעוצב בלחיצה
                        showPsychologistDialog(psychologist);
                    }

                    @Override
                    public void onLongClick(Psychologist psychologist) {
                        showPsychologistDialog(psychologist);
                    }

                    @Override
                    public void onEmailCLick(Psychologist psychologist) {
                        sendEmail(psychologist);
                    }
                }
        );

        rvPsychologists.setAdapter(psychologistAdapter);

        // ── חיפוש לפי שם / עיר ──
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

        // ── מיון לפי מחיר ──
        if (tvSort != null) {
            tvSort.setOnClickListener(v -> {
                sortedAsc = !sortedAsc;
                tvSort.setText(sortedAsc ? "מחיר ↑" : "מחיר ↓");
                sortList();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPsychologists();
    }

    // =======================
    // טעינה מ-DB
    // =======================
    private void loadPsychologists() {
        showLoading(true);
        databaseService.getPsychologistList(new DatabaseService.DatabaseCallback<List<Psychologist>>() {
            @Override
            public void onCompleted(List<Psychologist> psychologists) {
                showLoading(false);
                fullList = new ArrayList<>(psychologists);
                psychologistAdapter.setList(psychologists);
                updatePsychologistCount();
                updateEmptyState();
            }

            @Override
            public void onFailed(Exception e) {
                showLoading(false);
                Log.e(TAG, "טעינת הפסיכולוגים נכשלה", e);
                Toast.makeText(PsychologistListActivity.this, "שגיאה בטעינת הרשימה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =======================
    // ✅ הדיאלוג המעוצב החדש
    // =======================
    private void showPsychologistDialog(Psychologist psychologist) {
        if (psychologist == null) return;

        // יצירת אובייקט הדיאלוג
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_contact_psychologist);

        // מציאת הרכיבים בתוך ה-Layout של הדיאלוג
        TextView tvName = dialog.findViewById(R.id.tv_dialog_psychologist_name);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);
        Button btnSendEmail = dialog.findViewById(R.id.btn_dialog_send_email);

        // הזרקת השם של הפסיכולוג שנלחץ
        tvName.setText(psychologist.getName());

        // כפתור סגירה
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // כפתור שליחת מייל
        btnSendEmail.setOnClickListener(v -> {
            sendEmail(psychologist);
            dialog.dismiss();
        });

        // הגדרת עיצוב לחלון: רקע שקוף (בשביל הפינות המעוגלות) ורוחב מלא
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    // =======================
    // חיפוש לפי שם / עיר
    // =======================
    private void filterList(String query) {
        if (query.isEmpty()) {
            psychologistAdapter.setList(fullList);
        } else {
            List<Psychologist> filtered = new ArrayList<>();
            String lower = query.toLowerCase();
            for (Psychologist p : fullList) {
                boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(lower);
                boolean cityMatch = p.getCity() != null && p.getCity().toLowerCase().contains(lower);
                if (nameMatch || cityMatch) filtered.add(p);
            }
            psychologistAdapter.setList(filtered);
        }
        updatePsychologistCount();
        updateEmptyState();
    }

    // =======================
    // מיון לפי מחיר
    // =======================
    private void sortList() {
        List<Psychologist> sorted = new ArrayList<>(fullList);
        sorted.sort(sortedAsc
                ? Comparator.comparingInt(Psychologist::getSessionPrice)
                : Comparator.comparingInt(Psychologist::getSessionPrice).reversed());
        psychologistAdapter.setList(sorted);
    }

    // =======================
    // עדכון UI
    // =======================
    private void updatePsychologistCount() {
        int count = psychologistAdapter.getItemCount();
        tvPsychologistCount.setText("מספר הפסיכולוגים שלנו: " + count);
    }

    private void updateEmptyState() {
        if (emptyState == null) return;
        emptyState.setVisibility(psychologistAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        if (progressBar == null) return;
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // =======================
    // שליחת מייל
    // =======================
    private void sendEmail(Psychologist psychologist) {
        String email = psychologist.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "לא נמצא אימייל לפסיכולוג זה", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "שאלה בנושא תיאום פגישה – אפליקציית Luma");

        try {
            startActivity(Intent.createChooser(intent, "שלח אימייל באמצעות..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "לא נמצאה אפליקציית מייל מותקנת", Toast.LENGTH_SHORT).show();
        }
    }
}