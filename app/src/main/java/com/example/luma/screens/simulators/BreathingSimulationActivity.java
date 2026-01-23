package com.example.luma.screens.simulators;

import android.animation.ArgbEvaluator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.luma.R;
import com.google.android.material.button.MaterialButton;

public class BreathingSimulationActivity extends AppCompatActivity {

    private View breathingCircle;
    private TextView tvInstruction;
    private MaterialButton btnStart;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_simulation);

        // אתחול הרכיבים
        breathingCircle = findViewById(R.id.breathing_circle);
        tvInstruction = findViewById(R.id.tv_instruction);
        btnStart = findViewById(R.id.btn_start_breathing);
        ImageButton btnClose = findViewById(R.id.btn_close);

        // כפתור חזרה
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // הגדרת פעולה לכפתור ההתחלה
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                if (!isRunning) {
                    startBreathingExercise();
                    btnStart.animate().alpha(0f).setDuration(500).withEndAction(() -> btnStart.setVisibility(View.GONE));
                    isRunning = true;
                }
            });
        }
    }

    private void startBreathingExercise() {
        // 1. אנימציית גודל (Scale)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(breathingCircle, "scaleX", 1.0f, 2.2f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", 1.0f, 2.2f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        // 2. שדרוג: אנימציית צבע (מחליף צבע מכחול לירוק מרגיע בשאיפה)
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                Color.parseColor("#A3C1DA"), Color.parseColor("#A3DAB4"));
        colorAnim.addUpdateListener(animator -> breathingCircle.getBackground().setTint((int) animator.getAnimatedValue()));
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);

        // משך זמן: 4 שניות לכל כיוון (קצב נשימה אידיאלי להרגעה)
        long duration = 4000;
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        colorAnim.setDuration(duration);

        // עדכון טקסט לפי התקדמות האנימציה
        scaleX.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            if (progress > 1.9f) {
                tvInstruction.setText("להוציא אוויר לאט...");
            } else if (progress < 1.2f) {
                tvInstruction.setText("לשאוף אוויר עמוק...");
            }
        });

        // הפעלת כל האנימציות יחד
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, colorAnim);
        animatorSet.start();
    }
}