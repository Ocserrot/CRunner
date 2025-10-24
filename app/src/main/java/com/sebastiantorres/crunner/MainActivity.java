package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome, tvChallengeName, tvChallengeDetails;
    private Button btnStartChallenge, btnViewRanking, btnLogout;
    private SeekBar seekBarDistance;
    private TextView tvDistance;
    private int selectedDistance = 5; // km

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupUI();
        loadDailyChallenge();
        initializeDistanceSelector();
    }
    private void initializeDistanceSelector() {
        seekBarDistance = findViewById(R.id.seekBarDistance);
        tvDistance = findViewById(R.id.tvDistance);

        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Mínimo 3km, máximo 20km
                selectedDistance = Math.max(3, progress);
                tvDistance.setText("Distancia: " + selectedDistance + " km");

                // Actualizar detalles del desafío
                updateChallengeDetails(selectedDistance);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateChallengeDetails(int distanceKm) {
        // Calcular tiempo límite basado en distancia (8 min/km promedio)
        int timeLimitMinutes = distanceKm * 8;

        TextView tvChallengeDetails = findViewById(R.id.tvChallengeDetails);
        tvChallengeDetails.setText(String.format(
                "Distancia: %dkm - Tiempo límite: %dmin",
                distanceKm, timeLimitMinutes
        ));
    }

    // Modificar el método que inicia el desafío
    private void startCustomChallenge() {
        Intent intent = new Intent(MainActivity.this, RunningActivity.class);
        intent.putExtra("distance", selectedDistance * 1000); // Convertir a metros
        intent.putExtra("timeLimit", selectedDistance * 8 * 60); // Convertir a segundos
        startActivity(intent);
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvChallengeName = findViewById(R.id.tvChallengeName);
        tvChallengeDetails = findViewById(R.id.tvChallengeDetails);
        btnStartChallenge = findViewById(R.id.btnStartChallenge);
        btnViewRanking = findViewById(R.id.btnViewRanking);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getEmail().split("@")[0];
            tvWelcome.setText("Bienvenido, " + username + "!");
        }

        btnStartChallenge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RunningActivity.class));
            }
        });

        btnViewRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RankingActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void loadDailyChallenge() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("challenges")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot challenge = queryDocumentSnapshots.getDocuments().get(0);
                        displayChallenge(challenge);
                    } else {
                        createDefaultChallenge(today);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error cargando desafío", e);
                    createDefaultChallenge(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                });
    }

    private void displayChallenge(DocumentSnapshot challenge) {
        String name = challenge.getString("name");
        Double distance = challenge.getDouble("distance");
        Long timeLimit = challenge.getLong("timeLimit");

        if (name != null) {
            tvChallengeName.setText(name);
        }

        if (distance != null && timeLimit != null) {
            String details = String.format("Distancia: %.1fkm - Tiempo límite: %dmin",
                    distance / 1000, timeLimit / 60);
            tvChallengeDetails.setText(details);
        }
    }

    private void createDefaultChallenge(String date) {
        List<HashMap<String, Object>> routePoints = new ArrayList<>();
        HashMap<String, Object> point1 = new HashMap<>();
        point1.put("lat", 40.7128);
        point1.put("lng", -74.0060);
        HashMap<String, Object> point2 = new HashMap<>();
        point2.put("lat", 40.7218);
        point2.put("lng", -74.0160);
        routePoints.add(point1);
        routePoints.add(point2);

        HashMap<String, Object> challengeData = new HashMap<>();
        challengeData.put("type", "daily");
        challengeData.put("date", date);
        challengeData.put("difficulty", "medium");
        challengeData.put("name", "Desafío Diario de 5km");
        challengeData.put("distance", 5000);
        challengeData.put("timeLimit", 1800);
        challengeData.put("routePoints", routePoints);

        db.collection("challenges")
                .add(challengeData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("MainActivity", "Desafío por defecto creado: " + documentReference.getId());
                    loadDailyChallenge();
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error creando desafío por defecto", e);
                });
    }
}