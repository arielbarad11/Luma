package com.example.luma.screens.simulators;

import android.animation.ArgbEvaluator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
    private View breathingContainer;
    private TextView tvInstruction;
    private MaterialButton btnStart, btnStop;
    private boolean isRunning = false;
    private AnimatorSet fullCycle;

    private final int COLOR_DEEP_BLUE = Color.parseColor("#005396");
    private final int COLOR_LIGHT_BLUE = Color.parseColor("#4A90E2");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing_simulation);

        breathingCircle = findViewById(R.id.breathing_circle);
        breathingContainer = findViewById(R.id.breathing_container);
        tvInstruction = findViewById(R.id.tv_instruction);
        btnStart = findViewById(R.id.btn_start_breathing);
        btnStop = findViewById(R.id.btn_stop_breathing);

        if (breathingCircle != null) {
            breathingCircle.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            breathingCircle.setClipToOutline(true);
        }

        btnStart.setOnClickListener(v -> startExercise());
        if (btnStop != null) {
            btnStop.setVisibility(View.GONE);
            btnStop.setOnClickListener(v -> stopExercise());
        }
    }

    private void startExercise() {
        isRunning = true;
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        startBreathingExercise();
    }

    private void stopExercise() {
        isRunning = false;
        if (fullCycle != null) {
            fullCycle.cancel();
        }
        btnStop.setVisibility(View.GONE);
        btnStart.setVisibility(View.VISIBLE);
        tvInstruction.setText("מוכנים?");
        breathingContainer.setScaleX(1.0f);
        breathingContainer.setScaleY(1.0f);
        updateCircleColor(COLOR_DEEP_BLUE);
    }

    // פונקציה ליצירת אפקט "פמפום" (Pulse) במעברים
    private void playPulseEffect() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(breathingContainer, "scaleX", breathingContainer.getScaleX(), breathingContainer.getScaleX() + 0.1f, breathingContainer.getScaleX());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(breathingContainer, "scaleY", breathingContainer.getScaleY(), breathingContainer.getScaleY() + 0.1f, breathingContainer.getScaleY());
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.setDuration(200);
        pulse.start();
    }

    private void startBreathingExercise() {
        // זמנים לפי טכניקת ער"ן 4-4-6
        final long inhaleDuration = 4000; // 4 שניות שאיפה
        final long holdDuration = 4000;   // 4 שניות החזקה
        final long exhaleDuration = 6000; // 6 שניות נשיפה

        // --- 1. שאיפה (Inhale) ---
        ObjectAnimator inhaleX = ObjectAnimator.ofFloat(breathingContainer, "scaleX", 1.0f, 1.8f);
        ObjectAnimator inhaleY = ObjectAnimator.ofFloat(breathingContainer, "scaleY", 1.0f, 1.8f);
        ValueAnimator inhaleColor = ValueAnimator.ofObject(new ArgbEvaluator(), COLOR_DEEP_BLUE, COLOR_LIGHT_BLUE);
        inhaleColor.addUpdateListener(anim -> updateCircleColor((int) anim.getAnimatedValue()));

        AnimatorSet inhaleSet = new AnimatorSet();
        inhaleSet.playTogether(inhaleX, inhaleY, inhaleColor);
        inhaleSet.setDuration(inhaleDuration);
        inhaleSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("שאפו אוויר דרך האף...");
                playPulseEffect(); // פמפום בהתחלה
            }
        });

        // --- 2. החזקה (Hold) ---
        ValueAnimator holdAnim = ValueAnimator.ofFloat(1.8f, 1.8f);
        holdAnim.setDuration(holdDuration);
        holdAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("החזיקו את האוויר...");
                playPulseEffect(); // פמפום במעבר להחזקה
            }
        });

        // --- 3. נשיפה (Exhale) ---
        ObjectAnimator exhaleX = ObjectAnimator.ofFloat(breathingContainer, "scaleX", 1.8f, 1.0f);
        ObjectAnimator exhaleY = ObjectAnimator.ofFloat(breathingContainer, "scaleY", 1.8f, 1.0f);
        ValueAnimator exhaleColor = ValueAnimator.ofObject(new ArgbEvaluator(), COLOR_LIGHT_BLUE, COLOR_DEEP_BLUE);
        exhaleColor.addUpdateListener(anim -> updateCircleColor((int) anim.getAnimatedValue()));

        AnimatorSet exhaleSet = new AnimatorSet();
        exhaleSet.playTogether(exhaleX, exhaleY, exhaleColor);
        exhaleSet.setDuration(exhaleDuration);
        exhaleSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                tvInstruction.setText("נשפו לאט דרך הפה...");
                playPulseEffect(); // פמפום במעבר לנשיפה
            }
        });

        // חיבור הכל ללולאה אחת
        fullCycle = new AnimatorSet();
        fullCycle.playSequentially(inhaleSet, holdAnim, exhaleSet);
        fullCycle.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isRunning) fullCycle.start();
            }
        });

        fullCycle.start();
    }

    private void updateCircleColor(int color) {
        if (breathingCircle.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) breathingCircle.getBackground();
            shape.setColor(color);
            breathingCircle.invalidate();
        }
    }
}