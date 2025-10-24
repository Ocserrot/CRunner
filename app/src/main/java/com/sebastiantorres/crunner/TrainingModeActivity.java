package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class TrainingModeActivity extends AppCompatActivity {

    private Spinner spinnerInterval;
    private NumberPicker npTrainingHours, npTrainingMinutes;
    private TextView tvTotalTime, tvIntervalInfo, tvTrainingSummary;
    private CheckBox cbSpeed, cbDistance, cbPace, cbCalories;
    private Button btnStartTraining;

    private int selectedInterval = 1; // minutos
    private int totalTrainingTime = 600; // 10 minutos por defecto (en segundos)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_mode);

        initializeViews();
        setupSpinners();
        setupNumberPickers();
        setupClickListeners();
        updateTrainingInfo();
    }

    private void initializeViews() {
        spinnerInterval = findViewById(R.id.spinnerInterval);
        npTrainingHours = findViewById(R.id.npTrainingHours);
        npTrainingMinutes = findViewById(R.id.npTrainingMinutes);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvIntervalInfo = findViewById(R.id.tvIntervalInfo);
        tvTrainingSummary = findViewById(R.id.tvTrainingSummary);

        cbSpeed = findViewById(R.id.cbSpeed);
        cbDistance = findViewById(R.id.cbDistance);
        cbPace = findViewById(R.id.cbPace);
        cbCalories = findViewById(R.id.cbCalories);

        btnStartTraining = findViewById(R.id.btnStartTraining);
    }

    private void setupSpinners() {
        // Crear adapter para los intervalos
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.training_intervals, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInterval.setAdapter(adapter);

        // Listener para cambios en el spinner
        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Los valores están en el array: "1 minuto", "3 minutos", etc.
                String selected = parent.getItemAtPosition(position).toString();
                selectedInterval = Integer.parseInt(selected.split(" ")[0]); // Extraer el número
                updateIntervalInfo();
                updateTrainingSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedInterval = 1;
            }
        });
    }

    private void setupNumberPickers() {
        // Configurar NumberPicker para horas (0-5 horas)
        npTrainingHours.setMinValue(0);
        npTrainingHours.setMaxValue(5);
        npTrainingHours.setValue(0);
        npTrainingHours.setWrapSelectorWheel(false);

        // Configurar NumberPicker para minutos (0-59 minutos)
        npTrainingMinutes.setMinValue(0);
        npTrainingMinutes.setMaxValue(59);
        npTrainingMinutes.setValue(10); // 10 minutos por defecto
        npTrainingMinutes.setWrapSelectorWheel(false);

        // Formateadores para mostrar 2 dígitos
        npTrainingHours.setFormatter(value -> String.format("%02d", value));
        npTrainingMinutes.setFormatter(value -> String.format("%02d", value));

        // Listeners para cambios
        NumberPicker.OnValueChangeListener timeChangeListener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateTotalTime();
                updateTrainingSummary();
            }
        };

        npTrainingHours.setOnValueChangedListener(timeChangeListener);
        npTrainingMinutes.setOnValueChangedListener(timeChangeListener);

        // Calcular tiempo total inicial
        updateTotalTime();
    }

    private void setupClickListeners() {
        // Listeners para checkboxes
        View.OnClickListener checkboxListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTrainingSummary();
            }
        };

        cbSpeed.setOnClickListener(checkboxListener);
        cbDistance.setOnClickListener(checkboxListener);
        cbPace.setOnClickListener(checkboxListener);
        cbCalories.setOnClickListener(checkboxListener);

        // Botón de inicio
        btnStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrainingSession();
            }
        });
    }

    private void updateTotalTime() {
        int hours = npTrainingHours.getValue();
        int minutes = npTrainingMinutes.getValue();

        totalTrainingTime = (hours * 3600) + (minutes * 60);

        // Validar límites (10 min - 1 hora)
        if (totalTrainingTime < 600) { // 10 minutos mínimo
            totalTrainingTime = 600;
            npTrainingHours.setValue(0);
            npTrainingMinutes.setValue(10);
        } else if (totalTrainingTime > 3600) { // 1 hora máximo
            totalTrainingTime = 3600;
            npTrainingHours.setValue(1);
            npTrainingMinutes.setValue(0);
        }

        // Actualizar display
        if (hours > 0) {
            tvTotalTime.setText(String.format("Duración total: %d h %02d min", hours, minutes));
        } else {
            tvTotalTime.setText(String.format("Duración total: %d minutos", minutes));
        }
    }

    private void updateIntervalInfo() {
        String info = String.format("Se medirá: Velocidad y Distancia cada %d %s",
                selectedInterval, selectedInterval == 1 ? "minuto" : "minutos");
        tvIntervalInfo.setText(info);
    }

    private void updateTrainingInfo() {
        updateTotalTime();
        updateIntervalInfo();
        updateTrainingSummary();
    }

    private void updateTrainingSummary() {
        // Construir resumen de métricas seleccionadas
        StringBuilder metrics = new StringBuilder();
        if (cbSpeed.isChecked()) metrics.append("Velocidad, ");
        if (cbDistance.isChecked()) metrics.append("Distancia, ");
        if (cbPace.isChecked()) metrics.append("Ritmo, ");
        if (cbCalories.isChecked()) metrics.append("Calorías, ");

        // Remover la última coma
        if (metrics.length() > 0) {
            metrics.setLength(metrics.length() - 2);
        } else {
            metrics.append("Ninguna métrica seleccionada");
        }

        // Calcular número de intervalos
        int totalIntervals = totalTrainingTime / (selectedInterval * 60);

        String summary = String.format(Locale.getDefault(),
                "Duración: %s\n" +
                        "Intervalos: %d mediciones cada %d min\n" +
                        "Métricas: %s",
                getTimeDisplay(totalTrainingTime),
                totalIntervals,
                selectedInterval,
                metrics.toString());

        tvTrainingSummary.setText(summary);
    }

    private String getTimeDisplay(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%d h %02d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }

    private void startTrainingSession() {
        // Validaciones
        if (totalTrainingTime < 600) {
            Toast.makeText(this, "La duración mínima es 10 minutos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalTrainingTime > 3600) {
            Toast.makeText(this, "La duración máxima es 1 hora", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que al menos una métrica esté seleccionada
        if (!cbSpeed.isChecked() && !cbDistance.isChecked() && !cbPace.isChecked() && !cbCalories.isChecked()) {
            Toast.makeText(this, "Selecciona al menos una métrica para medir", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preparar datos para la sesión de entrenamiento
        Intent intent = new Intent(this, TrainingSessionActivity.class);
        intent.putExtra("totalTime", totalTrainingTime);
        intent.putExtra("interval", selectedInterval);
        intent.putExtra("metrics_speed", cbSpeed.isChecked());
        intent.putExtra("metrics_distance", cbDistance.isChecked());
        intent.putExtra("metrics_pace", cbPace.isChecked());
        intent.putExtra("metrics_calories", cbCalories.isChecked());

        startActivity(intent);
    }
}