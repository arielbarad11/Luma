package com.example.luma.screens.adminPages;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;

import java.util.List;

public class AdminPsychologistListActivity extends BaseActivity {

    private static final String TAG = "AdminPsychologistListActivity";

    private PsychologistAdapter psychologistAdapter;
    private TextView tvPsychologistCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_psychologist_list);

        View mainLayout = findViewById(R.id.admin_psychologist_list);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 🔐 בדיקת הרשאות אדמין
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            finish();
            return;
        }

        RecyclerView psychologistList = findViewById(R.id.rv_admin_psychologist_list);
        tvPsychologistCount = findViewById(R.id.tv_item_psychologist_count);

        if (psychologistList != null) {
            psychologistList.setLayoutManager(new LinearLayoutManager(this));
        }

        psychologistAdapter = new PsychologistAdapter(
                new PsychologistAdapter.OnClickListener() {
                    @Override
                    public void onClick(Psychologist psychologist) {
                        // לחיצה רגילה - הצגת הדיאלוג המעוצב
                        showUserViewDialog(psychologist);
                    }

                    @Override
                    public void onLongClick(Psychologist psychologist) {
                        // לחיצה ארוכה - תפריט ניהול
                        showAdminActionsDialog(psychologist);
                    }

                    @Override
                    public void onEmailCLick(Psychologist psychologist) {
                        sendEmail(psychologist);
                    }
                }
        );

        if (psychologistList != null) {
            psychologistList.setAdapter(psychologistAdapter);
        }

        TextView tvAddPsychologist = findViewById(R.id.tv_add_item_psychologist);
        if (tvAddPsychologist != null) {
            tvAddPsychologist.setOnClickListener(v -> showAddPsychologistDialog());
        }
    }

    // הצגת הדיאלוג המעוצב (תצוגת משתמש)
    private void showUserViewDialog(Psychologist psychologist) {
        Dialog dialog = new Dialog(this);
        // וודאי ששם הקובץ XML הזה נכון:
        dialog.setContentView(R.layout.dialog_contact_psychologist);

        TextView tvName = dialog.findViewById(R.id.tv_dialog_psychologist_name);
        Button btnClose = dialog.findViewById(R.id.btn_dialog_close);
        Button btnSendEmail = dialog.findViewById(R.id.btn_dialog_send_email);

        if (tvName != null) tvName.setText(psychologist.getName());

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnSendEmail != null) {
            btnSendEmail.setOnClickListener(v -> {
                sendEmail(psychologist);
                dialog.dismiss();
            });
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // עדכון דיאלוג ניהול
    private void showAdminActionsDialog(Psychologist psychologist) {
        String[] options = {"צפה בכרטיסייה (תצוגת משתמש)", "ערוך פרטי פסיכולוג", "מחק פסיכולוג"};
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle(psychologist.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showUserViewDialog(psychologist); // תוקן השם כאן
                    } else if (which == 1) {
                        editPsychologist(psychologist);
                    } else {
                        confirmDeletePsychologist(psychologist);
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPsychologists();
    }

    private void loadPsychologists() {
        databaseService.getPsychologistList(new DatabaseService.DatabaseCallback<List<Psychologist>>() {
            @Override
            public void onCompleted(List<Psychologist> psychologists) {
                psychologistAdapter.setList(psychologists);
                updatePsychologistCount();
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "טעינת הפסיכולוגים נכשלה", e);
            }
        });
    }

    private void editPsychologist(Psychologist psychologist) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_psychologist, null);

        EditText etName = dialogView.findViewById(R.id.et_psychologist_name);
        EditText etEmail = dialogView.findViewById(R.id.et_psychologist_email);
        EditText etCity = dialogView.findViewById(R.id.et_psychologist_city);
        EditText etPrice = dialogView.findViewById(R.id.et_psychologist_price);

        etName.setText(psychologist.getName());
        etEmail.setText(psychologist.getEmail());
        etCity.setText(psychologist.getCity());
        etPrice.setText(String.valueOf(psychologist.getSessionPrice()));

        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("ערוך פסיכולוג")
                .setView(dialogView)
                .setPositiveButton("עדכן", (dialog, which) -> {
                    psychologist.setName(etName.getText().toString());
                    psychologist.setEmail(etEmail.getText().toString());
                    psychologist.setCity(etCity.getText().toString());
                    String stPrice = etPrice.getText().toString();
                    psychologist.setSessionPrice(stPrice.isEmpty() ? 0 : Integer.parseInt(stPrice));

                    databaseService.updatePsychologist(psychologist, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void result) {
                            psychologistAdapter.update(psychologist);
                            Toast.makeText(AdminPsychologistListActivity.this, "הנתונים עודכנו", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailed(Exception e) { Log.e(TAG, "עריכה נכשלה", e); }
                    });
                })
                .setNegativeButton("בטל", null)
                .show();
    }

    private void confirmDeletePsychologist(Psychologist psychologist) {
        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("מחיקת פסיכולוג")
                .setMessage("האם אתה בטוח שברצונך למחוק את " + psychologist.getName() + "?")
                .setPositiveButton("מחק", (dialog, which) -> databaseService.deletePsychologist(psychologist.getId(), new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void object) {
                        psychologistAdapter.remove(psychologist);
                        updatePsychologistCount();
                    }
                    @Override
                    public void onFailed(Exception e) { Log.e(TAG, "מחיקה נכשלה", e); }
                }))
                .setNegativeButton("ביטול", null)
                .show();
    }

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

                    showConfirmAddPsychologistDialog(psychologist);
                })
                .setNegativeButton("בטל", null)
                .show();
    }

    private void showConfirmAddPsychologistDialog(Psychologist psychologist) {
        String msg = "שם: " + psychologist.getName() + "\n" +
                "אימייל: " + psychologist.getEmail() + "\n" +
                "עיר: " + psychologist.getCity() + "\n" +
                "מחיר: " + psychologist.getSessionPrice();

        new AlertDialog.Builder(this, R.style.RtlDialogTheme)
                .setTitle("אשר פרטים")
                .setMessage(msg)
                .setPositiveButton("הוסף", (dialog, which) -> addPsychologist(psychologist))
                .setNegativeButton("בטל", null)
                .show();
    }

    private void addPsychologist(Psychologist psychologist) {
        databaseService.createNewPsychologist(psychologist, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                psychologistAdapter.add(psychologist);
                updatePsychologistCount();
            }
            @Override
            public void onFailed(Exception e) { Log.e(TAG, "הוספה נכשלה", e); }
        });
    }

    private void updatePsychologistCount() {
        int count = psychologistAdapter.getItemCount();
        tvPsychologistCount.setText("מספר הפסיכולוגים: " + count);
    }

    private void sendEmail(Psychologist psychologist) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{psychologist.getEmail()});
        intent.putExtra(Intent.EXTRA_SUBJECT, "שאלה בנושא אפליקציית Luma");
        try {
            startActivity(Intent.createChooser(intent, "שלח אימייל..."));
        } catch (Exception ex) {
            Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
        }
    }
}