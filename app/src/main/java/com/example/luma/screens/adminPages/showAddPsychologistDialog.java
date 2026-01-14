package com.example.luma.screens.adminPages;
import com.example.luma.R;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import com.example.luma.models.Psychologist;

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
                psychologist.setName(etName.getText().toString());
                psychologist.setEmail(etEmail.getText().toString());
                psychologist.setCity(etCity.getText().toString());
                psychologist.setSessionPrice(
                        Integer.parseInt(etPrice.getText().toString())
                );

                showConfirmAddPsychologistDialog(psychologist);
            })
            .setNegativeButton("Cancel", null)
            .show();
}

private void showConfirmAddPsychologistDialog(Psychologist psychologist) {
}
