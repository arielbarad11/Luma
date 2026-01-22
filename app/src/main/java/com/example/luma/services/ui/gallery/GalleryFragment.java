package com.example.luma.services.ui.gallery; // ודאי שזה תואם לנתיב התיקייה שלך

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.luma.R;

public class GalleryFragment extends Fragment {

    // הסרנו את ה-Binding המסובך כדי לפתור את השגיאות מיד

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // טעינת ה-XML בצורה פשוטה וישירה
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        // מציאת הטקסט בתוך ה-XML
        TextView textView = root.findViewById(R.id.text_gallery);

        if (textView != null) {
            textView.setText("מסך גלריה - בבנייה");
        }

        return root;
    }
}