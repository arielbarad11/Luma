package com.example.luma.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luma.R;
import com.example.luma.models.User;
import com.example.luma.screens.LoginActivity;

import java.util.Objects;

public class LogoutHelper {
    public static void logout(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_exit);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);

        User user = SharedPreferencesUtil.getUser(context);

        TextView txtTitle = dialog.findViewById(R.id.txt_DialogExit_title), txtMessage = dialog.findViewById(R.id.txt_DialogExit_message);
        Button btnConfirm = dialog.findViewById(R.id.btn_DialogExit_confirm), btnCancel = dialog.findViewById(R.id.btn_DialogExit_cancel);

        txtTitle.setText("התנתקות");
        txtMessage.setText("האם ברצונך להתנתק?");

        btnConfirm.setOnClickListener(v -> {
            String userEmail = "";
            if (Objects.requireNonNull(user).getEmail() != null) {
                userEmail = user.getEmail();
            }

            SharedPreferencesUtil.signOutUser(context);
            Toast.makeText(context, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, LoginActivity.class);
            intent.putExtra("userEmail", userEmail);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
