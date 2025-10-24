package com.sebastiantorres.crunner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrainingSessionActivity extends AppCompatActivity {

    private TextView tvTimer, tvCurrentInterval, tvCurrentSpeed, tvCurrentDistance, tvCurrentPace;
    private Button btnPauseResume, btnFinishTraining;
    private RecyclerView rvIntervals;

    private Handler timerHandler = new Handler();
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean isRunning = false;

    private int totalTime;
    private int intervalDuration;
    private int currentInterval = 1;

    private List<TrainingInterval> intervals = new ArrayList<>();
    private IntervalAdapter intervalAdapter;

    private FusedLocationProviderClient fusedLocationClient;
    private double totalDistance = 0.0;
    private Location previousLocation;

    // Métricas a medir
    private boolean measureSpeed, measureDistance, measurePace, measureCalories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_session);

        initializeViews();
        setupRecyclerView();
        getTrainingParameters();
        setupLocationClient();
        startTraining();
    }

    private void initializeViews() {
        tvTimer = findViewById(R.id.tvTimer);
        tvCurrentInterval = findViewById(R.id.tvCurrentInterval);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvCurrentDistance = findViewById(R.id.tvCurrentDistance);
        tvCurrentPace = findViewById(R.id.tvCurrentPace);

        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnFinishTraining = findViewById(R.id.btnFinishTraining);
        rvIntervals = findViewById(R.id.rvIntervals);

        btnPauseResume.setOnClickListener(v -> togglePauseResume());
        btnFinishTraining.setOnClickListener(v -> finishTraining());
    }

    private void setupRecyclerView() {
        intervalAdapter = new IntervalAdapter(intervals);
        rvIntervals.setLayoutManager(new LinearLayoutManager(this));
        rvIntervals.setAdapter(intervalAdapter);
    }

    private void getTrainingParameters() {
        Intent intent = getIntent();
        totalTime = intent.getIntExtra("totalTime", 600);
        intervalDuration = intent.getIntExtra("interval", 1);

        measureSpeed = intent.getBooleanExtra("metrics_speed", true);
        measureDistance = intent.getBooleanExtra("metrics_distance", true);
        measurePace = intent.getBooleanExtra("metrics_pace", true);
        measureCalories = intent.getBooleanExtra("metrics_calories", false);

        // Ocultar métricas no seleccionadas - CORREGIDO
        if (!measureSpeed) {
            View layoutSpeed = findViewById(R.id.layoutSpeed);
            if (layoutSpeed != null) layoutSpeed.setVisibility(View.GONE);
        }
        if (!measureDistance) {
            View layoutDistance = findViewById(R.id.layoutDistance);
            if (layoutDistance != null) layoutDistance.setVisibility(View.GONE);
        }
        if (!measurePace) {
            View layoutPace = findViewById(R.id.layoutPace);
            if (layoutPace != null) layoutPace.setVisibility(View.GONE);
        }
    }

    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void startTraining() {
        isRunning = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 1000);
        timerHandler.postDelayed(intervalRunnable, intervalDuration * 60 * 1000);

        Toast.makeText(this, "¡Entrenamiento iniciado!", Toast.LENGTH_SHORT).show();
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimerUI();

                // Verificar si el tiempo total ha terminado
                if (elapsedTime >= totalTime * 1000) {
                    finishTraining();
                    return;
                }

                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private Runnable intervalRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                recordIntervalData();
                currentInterval++;
                tvCurrentInterval.setText("Intervalo: " + currentInterval);

                // Programar próximo intervalo
                timerHandler.postDelayed(this, intervalDuration * 60 * 1000);
            }
        }
    };

    private void updateTimerUI() {
        long remainingTime = Math.max(0, (totalTime * 1000) - elapsedTime);
        long seconds = (remainingTime / 1000) % 60;
        long minutes = (remainingTime / (1000 * 60)) % 60;

        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @SuppressLint("MissingPermission")
    private void recordIntervalData() {
        // Aquí iría la lógica para obtener la ubicación actual y calcular métricas
        // Por ahora, usaremos datos de ejemplo

        TrainingInterval interval = new TrainingInterval();
        interval.setIntervalNumber(currentInterval);
        interval.setTimestamp(new Date());

        // Datos de ejemplo (en una implementación real, estos vendrían del GPS)
        interval.setAverageSpeed(10.0 + (Math.random() * 5)); // 10-15 km/h
        interval.setDistance(0.5 + (Math.random() * 0.5)); // 0.5-1.0 km
        interval.setPace(4.0 + (Math.random() * 2)); // 4-6 min/km

        intervals.add(interval);
        intervalAdapter.notifyItemInserted(intervals.size() - 1);

        // Actualizar métricas actuales
        updateCurrentMetrics(interval);
    }

    private void updateCurrentMetrics(TrainingInterval interval) {
        if (measureSpeed && tvCurrentSpeed != null) {
            tvCurrentSpeed.setText(String.format("%.1f km/h", interval.getAverageSpeed()));
        }
        if (measureDistance && tvCurrentDistance != null) {
            tvCurrentDistance.setText(String.format("%.2f km", interval.getDistance()));
        }
        if (measurePace && tvCurrentPace != null) {
            tvCurrentPace.setText(String.format("%.1f min/km", interval.getPace()));
        }
    }

    private void togglePauseResume() {
        isRunning = !isRunning;

        if (isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime;
            timerHandler.postDelayed(timerRunnable, 1000);
            timerHandler.postDelayed(intervalRunnable, intervalDuration * 60 * 1000);
            btnPauseResume.setText("Pausar");
            Toast.makeText(this, "Entrenamiento reanudado", Toast.LENGTH_SHORT).show();
        } else {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.removeCallbacks(intervalRunnable);
            btnPauseResume.setText("Reanudar");
            Toast.makeText(this, "Entrenamiento pausado", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishTraining() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.removeCallbacks(intervalRunnable);

        // Guardar datos del entrenamiento
        saveTrainingData();

        // Mostrar resumen
        showTrainingSummary();
    }

    private void saveTrainingData() {
        // Aquí iría la lógica para guardar en Firestore o SharedPreferences
        // Por ahora, solo mostramos un mensaje
        Toast.makeText(this, "Datos del entrenamiento guardados", Toast.LENGTH_SHORT).show();
    }

    private void showTrainingSummary() {
        Intent intent = new Intent(this, TrainingSummaryActivity.class);

        // Convertir lista de intervalos a JSON para pasar entre actividades
        Gson gson = new Gson();
        String intervalsJson = gson.toJson(intervals);

        intent.putExtra("intervals", intervalsJson);
        intent.putExtra("totalTime", totalTime);
        intent.putExtra("totalDistance", calculateTotalDistance());
        intent.putExtra("averageSpeed", calculateAverageSpeed());

        startActivity(intent);
        finish();
    }

    private double calculateTotalDistance() {
        double total = 0.0;
        for (TrainingInterval interval : intervals) {
            total += interval.getDistance();
        }
        return total;
    }

    private double calculateAverageSpeed() {
        if (intervals.isEmpty()) return 0.0;

        double total = 0.0;
        for (TrainingInterval interval : intervals) {
            total += interval.getAverageSpeed();
        }
        return total / intervals.size();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.removeCallbacks(intervalRunnable);
    }
}