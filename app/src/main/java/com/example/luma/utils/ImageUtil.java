package com.example.luma.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;

public class ImageUtil {
    private static final String BASE64_PREFIX = "data:image/jpeg;base64,";

    public static @Nullable String convertTo64Base(@NotNull final ImageView imageView) {
        if (imageView.getDrawable() == null || !(imageView.getDrawable() instanceof BitmapDrawable)) {
            return null;
        }
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BASE64_PREFIX + Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // הוספת המתודה החסרה
    public static @Nullable Bitmap convertToBitmap(@Nullable String base64Code) {
        if (base64Code == null || base64Code.isEmpty()) return null;
        try {
            String pureBase64 = base64Code.contains(",") ? base64Code.substring(base64Code.indexOf(",") + 1) : base64Code;
            byte[] decodedString = Base64.decode(pureBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static void loadImage(@Nullable final String base64Code, @NotNull final ImageView imageView) {
        Bitmap bitmap = convertToBitmap(base64Code);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }
}