package com.sebastiantorres.crunner;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import android.util.Log;

public class ResultsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView ivMedal;
    private TextView tvTier, tvTierDescription, tvFinalTime, tvFinalDistance,
            tvAverageSpeed, tvPointsEarned;
    private Button btnSaveResult, btnViewRanking, btnNewChallenge;

    private RunResult runResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Obtener datos del intent
        Intent intent = getIntent();
        runResult = (RunResult) intent.getSerializableExtra("runResult");

        if (runResult == null) {
            finish();
            return;
        }

        initializeViews();
        displayResults();
        setupClickListeners();
    }

    private void initializeViews() {
        ivMedal = findViewById(R.id.ivMedal);
        tvTier = findViewById(R.id.tvTier);
        tvTierDescription = findViewById(R.id.tvTierDescription);
        tvFinalTime = findViewById(R.id.tvFinalTime);
        tvFinalDistance = findViewById(R.id.tvFinalDistance);
        tvAverageSpeed = findViewById(R.id.tvAverageSpeed);
        tvPointsEarned = findViewById(R.id.tvPointsEarned);
        btnSaveResult = findViewById(R.id.btnSaveResult);
        btnViewRanking = findViewById(R.id.btnViewRanking);
        btnNewChallenge = findViewById(R.id.btnNewChallenge);
    }

    private void displayResults() {
        // Determinar tier basado en el tiempo
        String tier = calculateTier(runResult.getCompletionTime(), runResult.getChallengeTimeLimit());
        int points = calculatePoints(tier);

        // Configurar UI según el tier
        setupTierUI(tier, points);

        // Mostrar estadísticas
        tvFinalTime.setText(String.format("Tiempo: %s", formatTime(runResult.getCompletionTime())));
        tvFinalDistance.setText(String.format("Distancia: %.2fkm", runResult.getDistance() / 1000));

        double averageSpeed = (runResult.getDistance() / 1000.0) / (runResult.getCompletionTime() / 3600.0);
        tvAverageSpeed.setText(String.format("Velocidad: %.1f km/h", averageSpeed));
        tvPointsEarned.setText(String.format("Puntos: %d", points));

        runResult.setTier(tier);
        runResult.setPointsEarned(points);
    }

    private String calculateTier(long completionTime, long timeLimit) {
        double timeRatio = (double) completionTime / timeLimit;

        if (timeRatio <= 0.6) {
            return "platinum";
        } else if (timeRatio <= 0.75) {
            return "gold";
        } else if (timeRatio <= 0.9) {
            return "silver";
        } else {
            return "bronze";
        }
    }

    private int calculatePoints(String tier) {
        switch (tier) {
            case "platinum": return 100;
            case "gold": return 85;
            case "silver": return 70;
            case "bronze": return 50;
            default: return 30;
        }
    }

    private void setupTierUI(String tier, int points) {
        switch (tier) {
            case "platinum":
                ivMedal.setImageResource(R.drawable.medal_platinum);
                tvTier.setText("PLATINO");
                tvTier.setTextColor(Color.parseColor("#E5E4E2"));
                tvTierDescription.setText("¡Rendimiento excepcional!");
                break;
            case "gold":
                ivMedal.setImageResource(R.drawable.medal_gold);
                tvTier.setText("ORO");
                tvTier.setTextColor(Color.parseColor("#FFD700"));
                tvTierDescription.setText("¡Excelente trabajo!");
                break;
            case "silver":
                ivMedal.setImageResource(R.drawable.medal_silver);
                tvTier.setText("PLATA");
                tvTier.setTextColor(Color.parseColor("#C0C0C0"));
                tvTierDescription.setText("¡Muy buena carrera!");
                break;
            case "bronze":
                ivMedal.setImageResource(R.drawable.medal_bronze);
                tvTier.setText("BRONCE");
                tvTier.setTextColor(Color.parseColor("#CD7F32"));
                tvTierDescription.setText("¡Buen esfuerzo!");
                break;
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / (1000 * 60)) % 60;
        long hours = (milliseconds / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    private void setupClickListeners() {
        btnSaveResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRunResult();
            }
        });

        btnViewRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultsActivity.this, RankingActivity.class));
            }
        });

        btnNewChallenge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultsActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void saveRunResult() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        HashMap<String, Object> attemptData = new HashMap<>();
        attemptData.put("userId", userId);
        attemptData.put("challengeDate", today);
        attemptData.put("completionTime", runResult.getCompletionTime());
        attemptData.put("distance", runResult.getDistance());
        attemptData.put("tier", runResult.getTier());
        attemptData.put("points", runResult.getPointsEarned());
        attemptData.put("completionDate", FieldValue.serverTimestamp());
        attemptData.put("averageSpeed", (runResult.getDistance() / 1000.0) / (runResult.getCompletionTime() / 3600.0));

        // Guardar en Firestore
        db.collection("user_attempts")
                .add(attemptData)
                .addOnSuccessListener(documentReference -> {
                    // Actualizar puntos totales del usuario
                    updateUserTotalPoints(userId, runResult.getPointsEarned());

                    Toast.makeText(ResultsActivity.this, "Resultado guardado exitosamente!", Toast.LENGTH_SHORT).show();
                    btnSaveResult.setEnabled(false);
                    btnSaveResult.setText("Resultado Guardado");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ResultsActivity.this, "Error guardando resultado: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUserTotalPoints(String userId, int pointsEarned) {
        db.collection("users")
                .document(userId)
                .update("totalPoints", FieldValue.increment(pointsEarned),
                        "currentStreak", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d("ResultsActivity", "Puntos del usuario actualizados");
                })
                .addOnFailureListener(e -> {
                    Log.e("ResultsActivity", "Error actualizando puntos del usuario", e);
                });
    }
}