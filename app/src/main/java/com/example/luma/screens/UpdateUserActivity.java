package com.example.luma.screens;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.example.luma.models.CrisisTime;
import com.example.luma.models.User;
import com.example.luma.screens.dialogs.FullImageDialog;
import com.example.luma.screens.dialogs.ProfileImageDialog;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.example.luma.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class UpdateUserActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "UpdateUserActivity";

    private EditText etUserFirstName, etUserEmail, etUserPassword;
    private TextView tvUserDisplayEmail;
    private ShapeableImageView imgUserProfile;
    private List<CrisisTime.CrisisOption> crisisOptions;

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

        View mainLayout = findViewById(R.id.tv_UpdateUser);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> { if (bitmap != null) handleImageBitmap(bitmap); }
        );

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(
                                    getContentResolver().openInputStream(uri));
                            if (bitmap != null) handleImageBitmap(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) { finish(); return; }

        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) selectedUid = currentUser.getId();

        isCurrentUser = selectedUid.equals(currentUser.getId());

        if (!isCurrentUser && !currentUser.isAdmin()) {
            Toast.makeText(this, "אין לך הרשאה לצפות בפרופיל זה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etUserFirstName    = findViewById(R.id.et_user_first_name);
        etUserEmail        = findViewById(R.id.et_user_email);
        etUserPassword     = findViewById(R.id.et_user_password);
        tvUserDisplayEmail = findViewById(R.id.tv_user_display_email);
        imgUserProfile     = findViewById(R.id.img_user_profile);

        Button btnUpdateProfile = findViewById(R.id.btn_edit_profile);
        if (btnUpdateProfile != null) btnUpdateProfile.setOnClickListener(this);

        if (imgUserProfile != null) {
            imgUserProfile.setOnClickListener(v -> {
                if (imgUserProfile.getDrawable() != null)
                    new FullImageDialog(UpdateUserActivity.this, imgUserProfile.getDrawable()).show();
            });
        }

        MaterialButton btnChangeImage = findViewById(R.id.btn_change_image);
        if (btnChangeImage != null) btnChangeImage.setOnClickListener(v -> openProfileImageDialog());

        setupCrisisChecklist(); // בניית הצ'קליסט לפני טעינת המשתמש
        showUserProfile();      // טעינת הנתונים ← תסמן את הצ'קבוקסים השמורים
    }

    // ─── צ'קליסט תוכנית חירום ────────────────────────────────────────────────

    private void setupCrisisChecklist() {
        crisisOptions = CrisisTime.getDefaultOptions();
        LinearLayout container = findViewById(R.id.ll_crisis_options);
        if (container == null) return;

        for (CrisisTime.CrisisOption option : crisisOptions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(option.getEmoji() + "  " + option.getLabel());
            cb.setTextSize(15f);
            cb.setTextColor(0xFF1F2937);
            cb.setPadding(0, 10, 0, 10);
            cb.setTag(option.getId()); // שומרים את ה-id ב-Tag לשליפה בהמשך

            cb.setOnCheckedChangeListener((btn, isChecked) ->
                    option.setSelected(isChecked));

            container.addView(cb);
        }
    }

    // סימון הצ'קבוקסים לפי מה שנשמר ב-Firebase (crisisTime)
    private void restoreCrisisSelections(ArrayList<String> savedIds) {
        if (savedIds == null || savedIds.isEmpty()) return;
        LinearLayout container = findViewById(R.id.ll_crisis_options);
        if (container == null) return;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof CheckBox)) continue; // דילוג על TextView של קטגוריה
            CheckBox cb = (CheckBox) child;
            String id = (String) cb.getTag();
            boolean checked = savedIds.contains(id);

            // ← חשוב: עדכון המודל לפני סימון ה-UI כדי שה-listener לא יתבלבל
            for (CrisisTime.CrisisOption opt : crisisOptions) {
                if (opt.getId().equals(id)) {
                    opt.setSelected(checked);
                    break;
                }
            }
            cb.setChecked(checked); // ← אחרי עדכון המודל
        }
    }

    // ─── טעינת פרופיל ────────────────────────────────────────────────────────

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

                etUserEmail.setEnabled(isCurrentUser);
                etUserPassword.setEnabled(isCurrentUser);

                // שחזור בחירות תוכנית החירום
                restoreCrisisSelections(user.getCrisisTime());
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load user", e);
            }
        });
    }

    // ─── עדכון פרופיל ────────────────────────────────────────────────────────

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_profile) updateUserProfile();
    }

    private void updateUserProfile() {
        if (selectedUser == null) return;

        String firstName = etUserFirstName.getText().toString();
        String email     = etUserEmail.getText().toString();
        String password  = etUserPassword.getText().toString();

        if (!isValid(firstName, email, password)) return;

        selectedUser.setFirstName(firstName);
        selectedUser.setEmail(email);
        selectedUser.setPassword(password);

        // איסוף הבחירות הנוכחיות מהצ'קליסט
        ArrayList<String> selectedIds = new ArrayList<>();
        for (CrisisTime.CrisisOption opt : CrisisTime.getSelectedOptions(crisisOptions)) {
            selectedIds.add(opt.getId());
        }
        selectedUser.setCrisisTime(selectedIds);

        updateUserInDatabase(selectedUser);
    }

    private void updateUserInDatabase(User user) {
        databaseService.updateUser(user.getId(), u -> {
            if (u == null) return null;
            u.setFirstName(user.getFirstName());
            u.setEmail(user.getEmail());
            u.setPassword(user.getPassword());
            u.setCrisisTime(user.getCrisisTime()); // ← שמירת תוכנית החירום
            return u;
        }, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                // שמירת crisisTime בנפרד ישירות לנתיב
                databaseService.updateUserCrisisTime(
                        user.getId(),
                        user.getCrisisTime(),
                        new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void r) {
                                Toast.makeText(UpdateUserActivity.this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                                showUserProfile();
                            }
                            @Override
                            public void onFailed(Exception e) {
                                Toast.makeText(UpdateUserActivity.this, "עדכון הפרופיל נכשל", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateUserActivity.this, "עדכון הפרופיל נכשל", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── וולידציה ────────────────────────────────────────────────────────────

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
            etUserPassword.setError("סיסמה חייבת להיות לפחות 6 תווים");
            return false;
        }
        return true;
    }

    // ─── תמונת פרופיל ────────────────────────────────────────────────────────

    private void openProfileImageDialog() {
        boolean hasImage = selectedUser != null
                && selectedUser.getProfileImage() != null
                && !selectedUser.getProfileImage().isEmpty();

        new ProfileImageDialog(this, hasImage, new ProfileImageDialog.ImagePickerListener() {
            @Override public void onCamera() { cameraLauncher.launch(null); }

            @Override public void onGallery() {
                photoPickerLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
            }

            @Override public void onDelete() {
                if (selectedUser != null) selectedUser.setProfileImage(null);
                imgUserProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                Toast.makeText(UpdateUserActivity.this, "התמונה נמחקה", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void handleImageBitmap(Bitmap bitmap) {
        if (imgUserProfile != null) imgUserProfile.setImageBitmap(bitmap);
        Toast.makeText(this, "התמונה התקבלה", Toast.LENGTH_SHORT).show();
    }

}