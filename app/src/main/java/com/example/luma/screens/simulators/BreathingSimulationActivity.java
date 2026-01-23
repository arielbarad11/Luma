package com.example.luma.screens.simulators;

import android.animation.ArgbEvaluator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
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

    // צבעי המותג החדשים
    private final int COLOR_DEEP_BLUE = Color.parseColor("#005396");
    private final int COLOR_LIGHT_BLUE = Color.parseColor("#4A90E2");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_simulation);

        breathingCircle = findViewById(R.id.breathing_circle);
        tvInstruction = findViewById(R.id.tv_instruction);
        btnStart = findViewById(R.id.btn_start_breathing);
        ImageButton btnClose = findViewById(R.id.btn_close);

        // פתרון בעיית הריבוע: הכרחת ה-View להיחתך לפי הרקע (העיגול)
        if (breathingCircle != null) {
            breathingCircle.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            breathingCircle.setClipToOutline(true);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

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
        final long inhaleDuration = 4000;
        final long holdDuration = 2000;
        final long exhaleDuration = 6000;

        // --- 1. שאיפה (Inhale) ---
        ObjectAnimator inhaleX = ObjectAnimator.ofFloat(breathingCircle, "scaleX", 1.0f, 2.0f);
        ObjectAnimator inhaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", 1.0f, 2.0f);

        ValueAnimator inhaleColor = ValueAnimator.ofObject(new ArgbEvaluator(), COLOR_DEEP_BLUE, COLOR_LIGHT_BLUE);
        inhaleColor.addUpdateListener(anim -> {
            updateCircleColor((int) anim.getAnimatedValue());
        });

        AnimatorSet inhaleSet = new AnimatorSet();
        inhaleSet.playTogether(inhaleX, inhaleY, inhaleColor);
        inhaleSet.setDuration(inhaleDuration);
        inhaleSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("לשאוף עמוק...");
            }
        });

        // --- 2. החזקה (Hold) ---
        ValueAnimator holdAnim = ValueAnimator.ofFloat(2.0f, 2.0f);
        holdAnim.setDuration(holdDuration);
        holdAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("להחזיק את הנשימה...");
            }
        });

        // --- 3. נשיפה (Exhale) ---
        ObjectAnimator exhaleX = ObjectAnimator.ofFloat(breathingCircle, "scaleX", 2.0f, 1.0f);
        ObjectAnimator exhaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", 2.0f, 1.0f);

        ValueAnimator exhaleColor = ValueAnimator.ofObject(new ArgbEvaluator(), COLOR_LIGHT_BLUE, COLOR_DEEP_BLUE);
        exhaleColor.addUpdateListener(anim -> {
            updateCircleColor((int) anim.getAnimatedValue());
        });

        AnimatorSet exhaleSet = new AnimatorSet();
        exhaleSet.playTogether(exhaleX, exhaleY, exhaleColor);
        exhaleSet.setDuration(exhaleDuration);
        exhaleSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("להוציא אוויר לאט...");
            }
        });

        AnimatorSet fullCycle = new AnimatorSet();
        fullCycle.playSequentially(inhaleSet, holdAnim, exhaleSet);
        fullCycle.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isRunning) fullCycle.start();
            }
        });

        fullCycle.start();
    }

    // פונקציית עזר לעדכון צבע השכבה הפנימית של העיגול בלי להרוס את הצורה
    private void updateCircleColor(int color) {
        if (breathingCircle.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) breathingCircle.getBackground();
            shape.setColor(color);
            breathingCircle.invalidate(); // הכרחת ציור מחדש כדי למנוע "גליץ'" לריבוע
        }
    }
}