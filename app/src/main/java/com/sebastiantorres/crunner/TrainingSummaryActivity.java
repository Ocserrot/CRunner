package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;


public class TrainingSummaryActivity extends AppCompatActivity {

    private TextView tvTotalTime, tvTotalDistance, tvAverageSpeed, tvAveragePace;
    private RecyclerView rvSummaryIntervals;
    private Button btnSaveSummary, btnNewTraining;

    private List<TrainingInterval> intervals;
    private int totalTime;
    private double totalDistance;
    private double averageSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_summary);

        initializeViews();
        loadTrainingData();
        displaySummary();
        setupClickListeners();
    }

    private void initializeViews() {
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        tvAverageSpeed = findViewById(R.id.tvAverageSpeed);
        tvAveragePace = findViewById(R.id.tvAveragePace);
        rvSummaryIntervals = findViewById(R.id.rvSummaryIntervals);
        btnSaveSummary = findViewById(R.id.btnSaveSummary);
        btnNewTraining = findViewById(R.id.btnNewTraining);
    }

    private void loadTrainingData() {
        Intent intent = getIntent();

        // Cargar lista de intervalos desde JSON
        String intervalsJson = intent.getStringExtra("intervals");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<TrainingInterval>>(){}.getType();
        intervals = gson.fromJson(intervalsJson, listType);

        totalTime = intent.getIntExtra("totalTime", 0);
        totalDistance = intent.getDoubleExtra("totalDistance", 0.0);
        averageSpeed = intent.getDoubleExtra("averageSpeed", 0.0);
    }

    private void displaySummary() {
        // Mostrar resumen general
        displayGeneralSummary();

        // Mostrar lista de intervalos
        displayIntervalsList();

        // Mostrar análisis de rendimiento
        displayPerformanceAnalysis();
    }

    private void displayGeneralSummary() {
        // Tiempo total
        int minutes = totalTime / 60;
        int seconds = totalTime % 60;
        tvTotalTime.setText(String.format("%d:%02d", minutes, seconds));

        // Distancia total
        tvTotalDistance.setText(String.format("%.2f km", totalDistance));

        // Velocidad promedio
        tvAverageSpeed.setText(String.format("%.1f km/h", averageSpeed));

        // Ritmo promedio
        double averagePace = totalDistance > 0 ? (totalTime / 60.0) / totalDistance : 0;
        tvAveragePace.setText(String.format("%.1f min/km", averagePace));
    }

    private void displayIntervalsList() {
        IntervalAdapter adapter = new IntervalAdapter(intervals);
        rvSummaryIntervals.setLayoutManager(new LinearLayoutManager(this));
        rvSummaryIntervals.setAdapter(adapter);
    }

    private void displayPerformanceAnalysis() {
        // Aquí podrías añadir más análisis como:
        // - Mejor intervalo
        // - Peor intervalo
        // - Consistencia
        // - Progresión
        TextView tvAnalysis = findViewById(R.id.tvAnalysis);
        if (tvAnalysis != null && intervals != null && !intervals.isEmpty()) {
            TrainingInterval bestInterval = findBestInterval();
            TrainingInterval worstInterval = findWorstInterval();
            double consistency = calculateConsistency();

            String analysis = String.format(Locale.getDefault(),
                    "Mejor intervalo: #%d (%.1f km/h)\n" +
                            "Peor intervalo: #%d (%.1f km/h)\n" +
                            "Consistencia: %.1f%%",
                    bestInterval.getIntervalNumber(), bestInterval.getAverageSpeed(),
                    worstInterval.getIntervalNumber(), worstInterval.getAverageSpeed(),
                    consistency);

            tvAnalysis.setText(analysis);
        }
    }

    private TrainingInterval findBestInterval() {
        TrainingInterval best = intervals.get(0);
        for (TrainingInterval interval : intervals) {
            if (interval.getAverageSpeed() > best.getAverageSpeed()) {
                best = interval;
            }
        }
        return best;
    }

    private TrainingInterval findWorstInterval() {
        TrainingInterval worst = intervals.get(0);
        for (TrainingInterval interval : intervals) {
            if (interval.getAverageSpeed() < worst.getAverageSpeed()) {
                worst = interval;
            }
        }
        return worst;
    }

    private double calculateConsistency() {
        if (intervals.size() < 2) return 100.0;

        double avgSpeed = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            avgSpeed = intervals.stream()
                    .mapToDouble(TrainingInterval::getAverageSpeed)
                    .average()
                    .orElse(0.0);
        }

        double sumSquaredDifferences = 0.0;
        for (TrainingInterval interval : intervals) {
            double diff = interval.getAverageSpeed() - avgSpeed;
            sumSquaredDifferences += diff * diff;
        }

        double standardDeviation = Math.sqrt(sumSquaredDifferences / intervals.size());
        double coefficientOfVariation = (standardDeviation / avgSpeed) * 100;

        return Math.max(0, 100 - coefficientOfVariation);
    }

    private void setupClickListeners() {
        btnSaveSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrainingSession();
            }
        });

        btnNewTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewTraining();
            }
        });
    }

    private void saveTrainingSession() {
        // Aquí iría la lógica para guardar en Firestore
        // Por ahora, solo mostramos un mensaje
        btnSaveSummary.setText("Guardado ✓");
        btnSaveSummary.setEnabled(false);
        btnSaveSummary.setBackgroundColor(getColor(android.R.color.darker_gray));

        // Simular guardado exitoso
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(TrainingSummaryActivity.this,
                    "Entrenamiento guardado en tu historial", Toast.LENGTH_SHORT).show();
        }, 500);
    }

    private void startNewTraining() {
        Intent intent = new Intent(this, TrainingModeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}