package com.example.luma.services.ui.home; // תיקון הנתיב ל-services

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.luma.R;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // טעינת העיצוב בצורה ישירה - ודאי שקיים קובץ fragment_home.xml ב-layout
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            // אתחול ה-ViewModel
            HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

            // מציאת הטקסט
            final TextView textView = root.findViewById(R.id.text_home);

            if (textView != null) {
                homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
            }
        } catch (Exception e) {
            // אם ה-ViewModel עושה בעיות, האפליקציה לפחות לא תקרוס
            e.printStackTrace();
        }

        return root;
    }
}