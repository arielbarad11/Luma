package com.example.luma.screens.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.luma.R; // ודאי שהייבוא הזה קיים

public class GalleryFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // טעינת העיצוב בצורה פשוטה ללא Binding כדי למנוע שגיאות כרגע
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // אם יש לך טקסט בעיצוב שאת רוצה לשנות:
        TextView textView = root.findViewById(R.id.text_gallery);
        if (textView != null) {
            textView.setText("מסך גלריה (בבנייה)");
        }

        return root;
    }
}