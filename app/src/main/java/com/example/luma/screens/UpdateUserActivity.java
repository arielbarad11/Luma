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
import com.example.luma.screens.dialogs.FullImageDialog;
import com.example.luma.screens.dialogs.ProfileImageDialog;
import com.example.luma.services.DatabaseService;
import com.example.luma.utils.SharedPreferencesUtil;
import com.example.luma.utils.Validator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * UpdateUserActivity - מסך לעדכון פרטי המשתמש ותמונת הפרופיל.
 * המסך תומך בעריכת המשתמש המחובר או בעריכת משתמש אחר על ידי מנהל (Admin).
 */
public class UpdateUserActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "UpdateUserActivity";

    // רכיבי ממשק המשתמש
    private EditText etUserFirstName, etUserEmail, etUserPassword;
    private TextView tvUserDisplayEmail;
    private ShapeableImageView imgUserProfile;

    private String selectedUid; // המזהה של המשתמש שאת פרטיו אנחנו מציגים
    private User selectedUser;  // אובייקט המשתמש שנטען מהשרת
    private User currentUser;   // המשתמש שמחובר כרגע לאפליקציה
    private boolean isCurrentUser = false; // דגל הבודק האם המשתמש עורך את עצמו

    // Launchers לטיפול בתוצאות של בחירת תמונה או צילום
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_user);

        // הגדרת Padding אוטומטי למניעת חפיפה עם שולי המסך
        View mainLayout = findViewById(R.id.tv_UpdateUser);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // אתחול ה-Launcher לצילום תמונה חדשה מהמצלמה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        handleImageBitmap(bitmap); // טיפול בתמונה שהתקבלה
                    }
                }
        );

        // אתחול ה-Launcher לבחירת תמונה קיימת מהגלריה (Photo Picker)
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            // המרת ה-URI של התמונה ל-Bitmap
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

        // בדיקת המשתמש המחובר מה-SharedPreferences
        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        // קבלת ה-UID של המשתמש לעריכה מה-Intent (אם הגיע ממסך ניהול משתמשים)
        selectedUid = getIntent().getStringExtra("USER_UID");
        if (selectedUid == null) {
            selectedUid = currentUser.getId(); // אם לא נשלח UID, המשתמש עורך את עצמו
        }

        isCurrentUser = selectedUid.equals(currentUser.getId());

        // אבטחה: רק אדמין או המשתמש עצמו רשאים לצפות/לערוך את הפרופיל
        if (!isCurrentUser && !currentUser.isAdmin()) {
            Toast.makeText(this, "אין לך הרשאה לצפות בפרופיל זה", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // אתחול רכיבי ה-UI
        etUserFirstName = findViewById(R.id.et_user_first_name);
        etUserEmail = findViewById(R.id.et_user_email);
        etUserPassword = findViewById(R.id.et_user_password);
        tvUserDisplayEmail = findViewById(R.id.tv_user_display_email);
        imgUserProfile = findViewById(R.id.img_user_profile);
        Button btnUpdateProfile = findViewById(R.id.btn_edit_profile);

        if (btnUpdateProfile != null) {
            btnUpdateProfile.setOnClickListener(this);
        }

        // לחיצה על התמונה - פתיחת דיאלוג להצגתה במסך מלא
        if (imgUserProfile != null) {
            imgUserProfile.setOnClickListener(v -> {
                if (imgUserProfile.getDrawable() != null) {
                    new FullImageDialog(UpdateUserActivity.this, imgUserProfile.getDrawable()).show();
                }
            });
        }

        // כפתור לשינוי תמונת הפרופיל - פותח דיאלוג בחירה (מצלמה/גלריה/מחיקה)
        MaterialButton btnChangeImage = findViewById(R.id.btn_change_image);
        if (btnChangeImage != null) {
            btnChangeImage.setOnClickListener(v -> openProfileImageDialog());
        }

        // טעינת נתוני המשתמש מה-Firebase
        showUserProfile();
    }

    /**
     * פתיחת דיאלוג לבחירת מקור התמונה (מצלמה או גלריה).
     */
    private void openProfileImageDialog() {
        boolean hasImage = selectedUser != null
                && selectedUser.getProfileImage() != null
                && !selectedUser.getProfileImage().isEmpty();

        new ProfileImageDialog(this, hasImage, new ProfileImageDialog.ImagePickerListener() {
            @Override
            public void onCamera() {
                cameraLauncher.launch(null); // הפעלת המצלמה
            }

            @Override
            public void onGallery() {
                // הפעלת בחירת תמונה מהגלריה
                photoPickerLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );
            }

            @Override
            public void onDelete() {
                // מחיקת תמונת הפרופיל
                if (selectedUser != null) {
                    selectedUser.setProfileImage(null);
                }
                imgUserProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                Toast.makeText(UpdateUserActivity.this, "התמונה נמחקה", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_profile) {
            updateUserProfile(); // קריאה לעדכון הנתונים בשרת
        }
    }

    /**
     * טעינת נתוני המשתמש מה-Database והצגתם בשדות הטופס.
     */
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

                // אבטחה: אימייל וסיסמה ניתן לערוך רק עבור המשתמש המחובר עצמו
                etUserEmail.setEnabled(isCurrentUser);
                etUserPassword.setEnabled(isCurrentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Failed to load user", e);
            }
        });
    }

    /**
     * איסוף הנתונים מהטופס, ביצוע וולידציה ושליחה לעדכון בשרת.
     */
    private void updateUserProfile() {
        if (selectedUser == null) return;

        String firstName = etUserFirstName.getText().toString();
        String email = etUserEmail.getText().toString();
        String password = etUserPassword.getText().toString();

        // בדיקת תקינות הקלט
        if (!isValid(firstName, email, password)) return;

        // עדכון האובייקט המקומי
        selectedUser.setFirstName(firstName);
        selectedUser.setEmail(email);
        selectedUser.setPassword(password);

        // שליחה לעדכון במסד הנתונים
        updateUserInDatabase(selectedUser);
    }

    /**
     * פונקציה המבצעת את העדכון בפועל ב-Firebase.
     */
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
                Toast.makeText(UpdateUserActivity.this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                showUserProfile(); // טעינה מחדש של הנתונים המעודכנים
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UpdateUserActivity.this, "עדכון הפרופיל נכשל", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * וולידציה (אימות) של פרטי המשתמש לפני שליחה.
     */
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

    /**
     * טיפול בתמונה שהתקבלה מהמצלמה או מהגלריה והצגתה ב-ImageView.
     */
    private void handleImageBitmap(Bitmap bitmap) {
        if (imgUserProfile != null) {
            imgUserProfile.setImageBitmap(bitmap);
        }
        // הערה: כאן ניתן להוסיף לוגיקה של העלאת התמונה ל-Storage של Firebase
        Toast.makeText(this, "התמונה התקבלה", Toast.LENGTH_SHORT).show();
    }
}