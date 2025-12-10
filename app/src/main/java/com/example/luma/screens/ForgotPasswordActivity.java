package com.example.luma.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.Validator;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail, etNewPassword;
    private Button btnUpdatePassword;
    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // UI Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init services
        databaseService = DatabaseService.getInstance();

        // Init UI
        etEmail = findViewById(R.id.et_email);
        etNewPassword = findViewById(R.id.et_new_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);

        btnUpdatePassword.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_update_password) {
            updatePassword();
        }
    }

    private void updatePassword() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        // Validations
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (!Validator.isPasswordValid(newPassword)) {
            etNewPassword.setError("Password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        // Get user by email
        databaseService.getUserByEmail(email, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {

                // If no user found
                if (user == null) {
                    etEmail.setError("Email not found");
                    etEmail.requestFocus();
                    Toast.makeText(ForgotPasswordActivity.this,
                            "No account found with this email",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // Update password
                user.setPassword(newPassword);

                databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void result) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password updated successfully!",
                                Toast.LENGTH_SHORT
                        ).show();
                        Intent mainIntent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainIntent);
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Failed to update password. Try again.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Error retrieving user",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}
