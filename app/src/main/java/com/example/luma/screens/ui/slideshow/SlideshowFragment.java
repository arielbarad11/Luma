package com.example.luma.screens.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.luma.R;

public class SlideshowFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // טעינת ה-XML בצורה ישירה ופשוטה
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        // אתחול ה-ViewModel (אם קיים קובץ SlideshowViewModel)
        try {
            SlideshowViewModel slideshowViewModel =
                    new ViewModelProvider(this).get(SlideshowViewModel.class);

            final TextView textView = root.findViewById(R.id.text_slideshow);
            if (textView != null) {
                slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return root;
    }
}