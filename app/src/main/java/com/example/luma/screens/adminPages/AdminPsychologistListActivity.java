package com.example.luma.screens.adminPages;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔐 בדיקת אדמין
        User currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null || !currentUser.isAdmin()) {
            finish();
            return;
        }

        RecyclerView usersList = findViewById(R.id.rv_users_list);
        tvPsychologistCount = findViewById(R.id.tv_item_psychologist_count);

        usersList.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Adapter תקין
        psychologistAdapter = new PsychologistAdapter(
                new PsychologistAdapter.OnClickListener() {

                    @Override
                    public void onClick(Psychologist psychologist) {
                        Log.d(TAG, "Psychologist clicked: " + psychologist.getId());
                    }

                    @Override
                    public void onLongClick(Psychologist psychologist) {
                        showAdminActionsDialog(psychologist);
                    }
                }
        );

        usersList.setAdapter(psychologistAdapter);

        TextView tvAddPsychologist = findViewById(R.id.tv_add_item_psychologist);

        tvAddPsychologist.setOnClickListener(v -> {
            showAddPsychologistDialog();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        databaseService.getPsychologistList(
                new DatabaseService.DatabaseCallback<List<Psychologist>>() {

                    @Override
                    public void onCompleted(List<Psychologist> psychologists) {
                        psychologistAdapter.setList(psychologists);
                        tvPsychologistCount.setText(
                                "מספר הפסיכולוגים: " + psychologists.size()
                        );
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Failed to load psychologists", e);
                    }
                }
        );
    }

    // =======================
    // תפריט פעולות אדמין
    // =======================
    private void showAdminActionsDialog(Psychologist psychologist) {

        String[] options = {"Edit psychologist", "Delete psychologist"};

        new AlertDialog.Builder(this)
                .setTitle(psychologist.getName())
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        EditPsychologist(psychologist);
                    } else {
                        confirmDeletePsychologist(psychologist);
                    }
                })
                .show();
    }

    // =======================
    // פעולות DB + UI
    // =======================

    private void EditPsychologist(Psychologist psychologist) {
        databaseService.updatePsychologist(
                psychologist,
                new DatabaseService.DatabaseCallback<Void>() {

                    @Override
                    public void onCompleted(Void result) {
                        psychologistAdapter.update(psychologist);
                        Log.d(TAG, "Psychologist updated successfully");
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "Edit psychologist failed", e);
                    }
                }
        );
    }

    private void confirmDeletePsychologist(Psychologist psychologist) {
        new AlertDialog.Builder(this)
                .setTitle("Delete psychologist")
                .setMessage("Are you sure you want to delete " + psychologist.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        databaseService.deletePsychologist(
                                psychologist.getId(),
                                new DatabaseService.DatabaseCallback<Void>() {

                                    @Override
                                    public void onCompleted(Void object) {
                                        psychologistAdapter.remove(psychologist);
                                    }

                                    @Override
                                    public void onFailed(Exception e) {
                                        Log.e(TAG, "Delete psychologist failed", e);
                                    }
                                }
                        );
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddPsychologistDialog() {

        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_add_psychologist, null);

        EditText etName = dialogView.findViewById(R.id.et_psychologist_name);
        EditText etEmail = dialogView.findViewById(R.id.et_psychologist_email);
        EditText etCity = dialogView.findViewById(R.id.et_psychologist_city);
        EditText etPrice = dialogView.findViewById(R.id.et_psychologist_price);

        new AlertDialog.Builder(this)
                .setTitle("Add psychologist")
                .setView(dialogView)
                .setPositiveButton("Continue", (dialog, which) -> {

                    Psychologist psychologist = new Psychologist();
                    String id = DatabaseService.getInstance().generatePsychologistId();
                    psychologist.setId(id);
                    psychologist.setName(etName.getText().toString());
                    psychologist.setEmail(etEmail.getText().toString());
                    psychologist.setCity(etCity.getText().toString());
                    String stPrice = etPrice.getText().toString();
                    int price = 0;
                    if(!stPrice.isEmpty()) {
                        price = Integer.parseInt(stPrice);
                    }
                    psychologist.setSessionPrice(price);
                    showConfirmAddPsychologistDialog(psychologist);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmAddPsychologistDialog(Psychologist psychologist) {

        new AlertDialog.Builder(this)
                .setTitle("Confirm add psychologist")
                .setMessage(
                        "Name: " + psychologist.getName() + "\n" +
                                "Email: " + psychologist.getEmail() + "\n" +
                                "City: " + psychologist.getCity() + "\n" +
                                "Price: " + psychologist.getSessionPrice()
                )
                .setPositiveButton("Add", (dialog, which) -> {
                    addPsychologist(psychologist);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addPsychologist(Psychologist psychologist) {
        databaseService.createNewPsychologist(psychologist, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                psychologistAdapter.add(psychologist);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Add psychologist failed", e);
            }
        });
    }
}
