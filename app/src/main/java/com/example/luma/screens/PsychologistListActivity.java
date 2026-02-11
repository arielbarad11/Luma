package com.example.luma.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.adapters.PsychologistAdapter;
import com.example.luma.models.Psychologist;
import com.example.luma.services.DatabaseService;

import java.util.List;

public class PsychologistListActivity extends BaseActivity {

    private static final String TAG = "UserPsychologistList";

    private PsychologistAdapter psychologistAdapter;
    private TextView tvPsychologistCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_psychologist_list);

        RecyclerView rvPsychologists = findViewById(R.id.rv_psychologist_list);
        tvPsychologistCount = findViewById(R.id.tv_item_psychologist_count);
        rvPsychologists.setLayoutManager(new LinearLayoutManager(this));


        psychologistAdapter = new PsychologistAdapter(
                new PsychologistAdapter.OnClickListener() {

                    // ❌ אין לחיצה רגילה
                    @Override
                    public void onClick(Psychologist psychologist) {
                        // לא עושים כלום
                    }

                    // ❌ אין לחיצה ארוכה
                    @Override
                    public void onLongClick(Psychologist psychologist) {
                        // לא עושים כלום
                    }

                    // ✅ צור קשר – מייל
                    @Override
                    public void onEmailCLick(Psychologist psychologist) {
                        sendEmail(psychologist);
                    }
                }
        );

        rvPsychologists.setAdapter(psychologistAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        databaseService.getPsychologistList(
                new DatabaseService.DatabaseCallback<>() {

                    @Override
                    public void onCompleted(List<Psychologist> psychologists) {
                        psychologistAdapter.setList(psychologists);
                        updatePsychologistCount();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Log.e(TAG, "טעינת הפסיכולוגים נכשלה", e);
                    }
                }
        );
    }

    private void updatePsychologistCount() {
        int count = psychologistAdapter.getItemCount();
        tvPsychologistCount.setText("מספר הפסיכולוגים: " + count);
    }

    // =======================
    // שליחת מייל – צור קשר
    // =======================
    private void sendEmail(Psychologist psychologist) {
        String email = psychologist.getEmail();
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "שאלה בנושא תיאום פגישה, אפליקציית Luma");

        try {
            startActivity(Intent.createChooser(intent, "שלח אימייל באמצעות..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "לא נמצאה אפליקציית מייל מותקנת", Toast.LENGTH_SHORT).show();
        }
    }
}
