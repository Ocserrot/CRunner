package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class DistanceRaceActivity extends AppCompatActivity {

    private RadioGroup radioRaceMode;
    private RadioButton radioCircular, radioFreeRun, radioOutBack;

    private Button btn1km, btn3km, btn5km, btn10km, btn21km, btn42km, btnCustom;
    private LinearLayout layoutCustomDistance;
    private EditText etCustomDistance;
    private Button btnSetCustom;
    private TextView tvSelectedDistance, tvRaceSummary, tvPace;

    private CheckBox cbTimeGoal;
    private LinearLayout layoutTimeGoal;
    private NumberPicker npGoalHours, npGoalMinutes, npGoalSeconds;

    private Button btnStartDistanceRace;

    private int selectedDistance = 1000; // metros (1km por defecto)
    private String selectedMode = "circular";
    private boolean hasTimeGoal = false;
    private int goalTimeSeconds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_race);

        initializeViews();
        setupClickListeners();
        setupNumberPickers();
        updateRaceSummary();
    }

    private void initializeViews() {
        // Modos de carrera
        radioRaceMode = findViewById(R.id.radioRaceMode);
        radioCircular = findViewById(R.id.radioCircular);
        radioFreeRun = findViewById(R.id.radioFreeRun);
        radioOutBack = findViewById(R.id.radioOutBack);

        // Botones de distancia
        btn1km = findViewById(R.id.btn1km);
        btn3km = findViewById(R.id.btn3km);
        btn5km = findViewById(R.id.btn5km);
        btn10km = findViewById(R.id.btn10km);
        btn21km = findViewById(R.id.btn21km);
        btn42km = findViewById(R.id.btn42km);
        btnCustom = findViewById(R.id.btnCustom);

        // Distancia personalizada
        layoutCustomDistance = findViewById(R.id.layoutCustomDistance);
        etCustomDistance = findViewById(R.id.etCustomDistance);
        btnSetCustom = findViewById(R.id.btnSetCustom);
        tvSelectedDistance = findViewById(R.id.tvSelectedDistance);

        // Tiempo objetivo
        cbTimeGoal = findViewById(R.id.cbTimeGoal);
        layoutTimeGoal = findViewById(R.id.layoutTimeGoal);
        npGoalHours = findViewById(R.id.npGoalHours);
        npGoalMinutes = findViewById(R.id.npGoalMinutes);
        npGoalSeconds = findViewById(R.id.npGoalSeconds);
        tvPace = findViewById(R.id.tvPace);

        // Resumen e inicio
        tvRaceSummary = findViewById(R.id.tvRaceSummary);
        btnStartDistanceRace = findViewById(R.id.btnStartDistanceRace);
    }

    private void setupClickListeners() {
        // Listeners de modo de carrera
        radioRaceMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCircular) {
                selectedMode = "circular";
            } else if (checkedId == R.id.radioFreeRun) {
                selectedMode = "free";
            } else if (checkedId == R.id.radioOutBack) {
                selectedMode = "outback";
            }
            updateRaceSummary();
        });

        // Listeners de distancias predefinidas
        View.OnClickListener distanceListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDistanceButtons();
                layoutCustomDistance.setVisibility(View.GONE);

                if (v.getId() == R.id.btn1km) {
                    selectedDistance = 1000;
                    btn1km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btn3km) {
                    selectedDistance = 3000;
                    btn3km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btn5km) {
                    selectedDistance = 5000;
                    btn5km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btn10km) {
                    selectedDistance = 10000;
                    btn10km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btn21km) {
                    selectedDistance = 21000;
                    btn21km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btn42km) {
                    selectedDistance = 42195;
                    btn42km.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                } else if (v.getId() == R.id.btnCustom) {
                    layoutCustomDistance.setVisibility(View.VISIBLE);
                    btnCustom.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_blue_dark));
                    return;
                }

                updateDistanceDisplay();
                updateRaceSummary();
                calculatePace();
            }
        };

        btn1km.setOnClickListener(distanceListener);
        btn3km.setOnClickListener(distanceListener);
        btn5km.setOnClickListener(distanceListener);
        btn10km.setOnClickListener(distanceListener);
        btn21km.setOnClickListener(distanceListener);
        btn42km.setOnClickListener(distanceListener);
        btnCustom.setOnClickListener(distanceListener);

        // Distancia personalizada
        btnSetCustom.setOnClickListener(v -> {
            String customDistanceStr = etCustomDistance.getText().toString();
            if (!TextUtils.isEmpty(customDistanceStr)) {
                try {
                    double customKm = Double.parseDouble(customDistanceStr);
                    if (customKm > 0 && customKm <= 100) {
                        selectedDistance = (int) (customKm * 1000);
                        updateDistanceDisplay();
                        updateRaceSummary();
                        calculatePace();
                        layoutCustomDistance.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Ingresa una distancia entre 0.1 y 100 km", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Ingresa un número válido", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ingresa una distancia", Toast.LENGTH_SHORT).show();
            }
        });

        // Tiempo objetivo
        cbTimeGoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasTimeGoal = isChecked;
            layoutTimeGoal.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            tvPace.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                calculatePace();
            } else {
                goalTimeSeconds = 0;
                updateRaceSummary();
            }
        });

        // Iniciar carrera
        btnStartDistanceRace.setOnClickListener(v -> startDistanceRace());
    }

    private void setupNumberPickers() {
        // Configurar number pickers de tiempo objetivo PROGRAMÁTICAMENTE
        setupNumberPicker(npGoalHours, 0, 5, 0);
        setupNumberPicker(npGoalMinutes, 0, 59, 30);
        setupNumberPicker(npGoalSeconds, 0, 59, 0);

        // Listeners para number pickers de tiempo
        NumberPicker.OnValueChangeListener timeChangeListener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                calculatePace();
            }
        };

        npGoalHours.setOnValueChangedListener(timeChangeListener);
        npGoalMinutes.setOnValueChangedListener(timeChangeListener);
        npGoalSeconds.setOnValueChangedListener(timeChangeListener);
    }

    private void setupNumberPicker(NumberPicker numberPicker, int min, int max, int value) {
        numberPicker.setMinValue(min);
        numberPicker.setMaxValue(max);
        numberPicker.setValue(value);
        numberPicker.setWrapSelectorWheel(false);

        // Formatear display para mostrar siempre 2 dígitos
        numberPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
    }

    private void resetDistanceButtons() {
        int defaultColor = android.graphics.Color.parseColor("#FF757575"); // Color gris por defecto

        btn1km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btn3km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btn5km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btn10km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btn21km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btn42km.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        btnCustom.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
    }

    private void updateDistanceDisplay() {
        if (selectedDistance < 1000) {
            tvSelectedDistance.setText(String.format("Distancia seleccionada: %d m", selectedDistance));
        } else {
            double distanceKm = selectedDistance / 1000.0;
            tvSelectedDistance.setText(String.format("Distancia seleccionada: %.1f km", distanceKm));
        }
    }

    private void calculatePace() {
        if (!hasTimeGoal) {
            tvPace.setVisibility(View.GONE);
            return;
        }

        goalTimeSeconds = (npGoalHours.getValue() * 3600) +
                (npGoalMinutes.getValue() * 60) +
                npGoalSeconds.getValue();

        if (goalTimeSeconds > 0 && selectedDistance > 0) {
            double paceSecondsPerKm = (goalTimeSeconds * 1000.0) / selectedDistance;
            int minutes = (int) (paceSecondsPerKm / 60);
            int seconds = (int) (paceSecondsPerKm % 60);

            tvPace.setText(String.format("Ritmo necesario: %d:%02d min/km", minutes, seconds));
            tvPace.setVisibility(View.VISIBLE);
        } else {
            tvPace.setText("Ritmo necesario: --:-- min/km");
        }

        updateRaceSummary();
    }

    private void updateRaceSummary() {
        String modeText = "";
        switch (selectedMode) {
            case "circular":
                modeText = "Ruta Circular";
                break;
            case "free":
                modeText = "Run Free";
                break;
            case "outback":
                modeText = "Ida y Vuelta";
                break;
        }

        String distanceText;
        if (selectedDistance < 1000) {
            distanceText = String.format("%d m", selectedDistance);
        } else {
            distanceText = String.format("%.1f km", selectedDistance / 1000.0);
        }

        String timeText = "Sin objetivo";
        if (hasTimeGoal && goalTimeSeconds > 0) {
            int hours = goalTimeSeconds / 3600;
            int minutes = (goalTimeSeconds % 3600) / 60;
            int seconds = goalTimeSeconds % 60;

            if (hours > 0) {
                timeText = String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                timeText = String.format("%d:%02d", minutes, seconds);
            }
        }

        String summary = String.format(Locale.getDefault(),
                "Modo: %s\nDistancia: %s\nTiempo: %s",
                modeText, distanceText, timeText);

        tvRaceSummary.setText(summary);
    }

    private void startDistanceRace() {
        // Validar distancia personalizada si está activa
        if (layoutCustomDistance.getVisibility() == View.VISIBLE &&
                TextUtils.isEmpty(etCustomDistance.getText().toString())) {
            Toast.makeText(this, "Establece una distancia personalizada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calcular tiempo límite (si no hay objetivo, usar tiempo estimado)
        int timeLimit;
        if (hasTimeGoal && goalTimeSeconds > 0) {
            timeLimit = goalTimeSeconds;
        } else {
            // Tiempo estimado basado en 6 min/km
            timeLimit = (int) ((selectedDistance / 1000.0) * 6 * 60);
        }

        // Crear intent para la carrera
        Intent intent = new Intent(this, RunningActivity.class);
        intent.putExtra("mode", "distance");
        intent.putExtra("distance", selectedDistance);
        intent.putExtra("timeLimit", timeLimit);
        intent.putExtra("raceMode", selectedMode);
        intent.putExtra("hasTimeGoal", hasTimeGoal);
        intent.putExtra("goalTime", goalTimeSeconds);

        startActivity(intent);
    }
}