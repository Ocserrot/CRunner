package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenuActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvWelcome;
    private Button btnLogout;

    private CardView cardCustomRace, cardDistanceRace, cardTrainingMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupUserInfo();
        setupClickListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        cardCustomRace = findViewById(R.id.cardCustomRace);
        cardDistanceRace = findViewById(R.id.cardDistanceRace);
        cardTrainingMode = findViewById(R.id.cardTrainingMode);
    }

    private void setupUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getEmail().split("@")[0];
            tvWelcome.setText("Bienvenido, " + username + "!");
        }
    }

    private void setupClickListeners() {
        // Opción 1: Carrera Personalizada
        cardCustomRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCustomRace();
            }
        });

        // Opción 2: Carrera por Kilómetros
        cardDistanceRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDistanceRace();
            }
        });

        // Opción 3: Modo Entrenamiento
        cardTrainingMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrainingMode();
            }
        });

        // Cerrar Sesión
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainMenuActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void startCustomRace() {
        // TODO: Implementar Activity de Carrera Personalizada
        Intent intent = new Intent(MainMenuActivity.this, CustomRaceActivity.class);
        startActivity(intent);

        Toast.makeText(this, "Carrera Personalizada - Próximamente", Toast.LENGTH_SHORT).show();
    }

    private void startDistanceRace() {
        // TODO: Implementar Activity de Carrera por Kilómetros
        Intent intent = new Intent(MainMenuActivity.this, DistanceRaceActivity.class);
        startActivity(intent);

        Toast.makeText(this, "Carrera por Kilómetros - Próximamente", Toast.LENGTH_SHORT).show();
    }

    private void startTrainingMode() {
        // TODO: Implementar Activity de Modo Entrenamiento
        Intent intent = new Intent(MainMenuActivity.this, TrainingModeActivity.class);
        startActivity(intent);

        Toast.makeText(this, "Modo Entrenamiento - Próximamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        // Prevenir que vuelva al login al presionar back
        moveTaskToBack(true);
    }
}