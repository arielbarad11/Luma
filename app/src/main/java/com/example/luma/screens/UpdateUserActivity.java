package com.example.luma.screens;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.dialogs.ProfileImageDialog;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.example.luma.utils.Validator;
import com.google.android.material.button.MaterialButton;

public class UpdateUserActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "UpdateUserActivity";

    private EditText etUserFirstName, etUserEmail, etUserPassword;
    private TextView tvUserDisplayEmail;

    private String selectedUid;
    private User selectedUser;
    private User currentUser;
    private boolean isCurrentUser = false;

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_user);

        // ===== Edge to Edge Padding =====
        View mainLayout = findViewById(R.id.tv_UpdateUser);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // ===== Launchers init =====
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        handleImageBitmap(bitmap);
                    }
                }
        );

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(
                                    getContentResolver().openInputStream(uri)
                            );
                            if (bitmap != null) {
                                handleImageBitmap(bitmap);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // ===== current user check =====
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        // ===== selected user check =====
        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) {
            selectedUid = currentUser.getId();
        }

        isCurrentUser = selectedUid.equals(currentUser.getId());

        // ===== authorization check =====
        if (!isCurrentUser && !currentUser.isAdmin()) {
            Toast.makeText(this, "אין לך הרשאה לצפות בפרופיל זה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Selected user UID: " + selectedUid);

        // ===== init views =====
        etUserFirstName = findViewById(R.id.et_user_first_name);
        etUserEmail = findViewById(R.id.et_user_email);
        etUserPassword = findViewById(R.id.et_user_password);
        tvUserDisplayEmail = findViewById(R.id.tv_user_display_email);
        Button btnUpdateProfile = findViewById(R.id.btn_edit_profile);

        if (btnUpdateProfile != null) {
            btnUpdateProfile.setOnClickListener(this);
        }

        // ===== כפתור תמונת פרופיל =====
        MaterialButton btnChangeImage = findViewById(R.id.btn_change_image);
        if (btnChangeImage != null) {
            btnChangeImage.setOnClickListener(v -> openProfileImageDialog());
        }

        showUserProfile();
    }

    private void openProfileImageDialog() {
        boolean hasImage = selectedUser != null
                && selectedUser.getProfileImage() != null
                && !selectedUser.getProfileImage().isEmpty();

        new ProfileImageDialog(this, hasImage, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                cameraLauncher.launch(null);
            }

            @Override
            public void onGallery() {
                photoPickerLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );
            }

            @Override
            public void onDelete() {
                if (selectedUser != null) {
                    selectedUser.setProfileImage(null);
                }
                Toast.makeText(UpdateUserActivity.this, "התמונה נמחקה", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_profile) {
            updateUserProfile();
        }
    }

    private void showUserProfile() {
        databaseService.getUser(selectedUid, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user == null) return;
                selectedUser = user;

                etUserFirstName.setText(user.getFirstName());
                etUserEmail.setText(user.getEmail());
                etUserPassword.setText(user.getPassword());
                tvUserDisplayEmail.setText(user.getEmail());

                // שדות נעולים אם זה לא המשתמש עצמו
                etUserEmail.setEnabled(isCurrentUser);
                etUserPassword.setEnabled(isCurrentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load user", e);
            }
        });
    }

    private void updateUserProfile() {
        if (selectedUser == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = etUserFirstName.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        if (!isValid(firstName, email, password)) {
            return;
        }

        if (!isCurrentUser && !currentUser.isAdmin()) {
            Toast.makeText(this, "אין לך הרשאה לעדכן משתמש זה", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedUser.setFirstName(firstName);
        selectedUser.setEmail(email);
        selectedUser.setPassword(password);

        updateUserInDatabase(selectedUser);
    }

    private void updateUserInDatabase(User user) {
        databaseService.updateUser(user.getId(), u -> {
            if (u == null) return null;
            u.setFirstName(user.getFirstName());
            u.setEmail(user.getEmail());
            u.setPassword(user.getPassword());
            return u;
        }, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                Toast.makeText(UpdateUserActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                showUserProfile();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateUserActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValid(String firstName, String email, String password) {
        if (!Validator.isNameValid(firstName)) {
            etUserFirstName.setError("שם לא תקין");
            return false;
        }
        if (!Validator.isEmailValid(email)) {
            etUserEmail.setError("אימייל לא תקין");
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etUserPassword.setError("סיסמה לא תקינה");
            return false;
        }
        return true;
    }

    private void handleImageBitmap(Bitmap bitmap) {
        // כאן תוסיפי את הלוגיקה של מה לעשות עם התמונה (למשל להציג ב-ImageView או להעלות לשרת)
        Toast.makeText(this, "התמונה התקבלה", Toast.LENGTH_SHORT).show();
    }
}