package com.sebastiantorres.crunner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long UPDATE_INTERVAL = 3000; // 3 seconds
    private static final long FASTEST_INTERVAL = 2000; // 2 seconds

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private TextView tvTimer, tvDistance, tvSpeed;
    private Button btnStartPause, btnFinish;

    private boolean isRunning = false;
    private long startTime = 0;
    private long elapsedTime = 0;
    private double totalDistance = 0.0;
    private Handler timerHandler = new Handler();

    private List<LatLng> userPath = new ArrayList<>();
    private Polyline userPolyline;
    private List<LatLng> challengeRoute = new ArrayList<>();

    // --- FIX: Declared missing variables ---
    private double challengeDistance = 0.0;
    private Location currentLocation = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupMap();

        // This is called within onMapReady after permissions are checked
        // setupLocationClient();
    }

    private void initializeViews() {
        tvTimer = findViewById(R.id.tvTimer);
        tvDistance = findViewById(R.id.tvDistance);
        tvSpeed = findViewById(R.id.tvSpeed);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnFinish = findViewById(R.id.btnFinish);

        btnStartPause.setOnClickListener(v -> {
            if (!isRunning) {
                startRunning();
            } else {
                pauseRunning();
            }
        });

        btnFinish.setOnClickListener(v -> finishRunning());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        mMap.setMyLocationEnabled(true);
        setupLocationClient();
        getCurrentLocation();
    }

    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    currentLocation = locationResult.getLastLocation(); // --- FIX: Update current location ---
                    if (isRunning) {
                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        updateUserLocation(currentLatLng);
                    }
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            }
            startLocationUpdates(); // Start updates regardless
        });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateUserLocation(LatLng newLocation) {
        userPath.add(newLocation);

        if (userPath.size() > 1) {
            LatLng previousLocation = userPath.get(userPath.size() - 2);
            totalDistance += calculateDistance(previousLocation, newLocation);
            updateDistanceUI();
        }

        if (userPolyline != null) {
            userPolyline.setPoints(userPath);
        } else {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(userPath)
                    .width(8f)
                    .color(getResources().getColor(android.R.color.holo_blue_light));
            userPolyline = mMap.addPolyline(polylineOptions);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 16f));
    }

    private void startRunning() {
        isRunning = true;
        btnStartPause.setText("Pausar");
        btnFinish.setEnabled(true);

        startTime = System.currentTimeMillis() - elapsedTime;
        timerHandler.postDelayed(timerRunnable, 1000);
        startLocationUpdates();
        Toast.makeText(this, "¡Carrera iniciada!", Toast.LENGTH_SHORT).show();
    }

    private void pauseRunning() {
        isRunning = false;
        btnStartPause.setText("Continuar");
        timerHandler.removeCallbacks(timerRunnable);
        Toast.makeText(this, "Carrera pausada", Toast.LENGTH_SHORT).show();
    }

    private void finishRunning() {
        isRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        showResults();
        saveRunToHistory();

    }

    private void saveRunToHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Crear un objeto para guardar la carrera
        Map<String, Object> runData = new HashMap<>();
        runData.put("timestamp", FieldValue.serverTimestamp()); // Guarda la fecha y hora
        runData.put("distance", totalDistance); // Distancia en metros
        runData.put("duration", elapsedTime); // Duración en milisegundos
        // Puedes añadir el tipo de carrera, la ruta (como JSON), etc.
        // runData.put("raceType", "circular");

        db.collection("users").document(userId).collection("runs")
                .add(runData)
                .addOnSuccessListener(documentReference -> Log.d("SAVE_RUN", "Carrera guardada con ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("SAVE_RUN", "Error al guardar la carrera", e));
    }


    private void showResults() {
        RunResult runResult = new RunResult();
        runResult.setCompletionTime(elapsedTime);
        runResult.setDistance(totalDistance);
        runResult.setChallengeTimeLimit(30 * 60 * 1000); // Placeholder

        Intent intent = new Intent(RunningActivity.this, ResultsActivity.class);
        intent.putExtra("runResult", runResult);
        startActivity(intent);
        finish();
    }

    private double calculateDistance(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results);
        return results[0]; // distance in meters
    }

    private void updateDistanceUI() {
        double distanceKm = totalDistance / 1000.0;
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));

        if (elapsedTime > 0) {
            double hours = elapsedTime / 3600000.0;
            double speedKph = hours > 0 ? distanceKm / hours : 0.0;
            tvSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", speedKph));
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimerUI();
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimerUI() {
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = elapsedTime / (1000 * 60 * 60);

        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    // --- FIX: Added missing method ---
    private void drawChallengeRoute() {
        if (mMap == null || challengeRoute.isEmpty()) return;

        PolylineOptions challengePolylineOptions = new PolylineOptions()
                .addAll(challengeRoute)
                .width(12f)
                .color(getResources().getColor(android.R.color.holo_green_dark));
        mMap.addPolyline(challengePolylineOptions);

        if (!challengeRoute.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(challengeRoute.get(0), 14f));
        }
    }

    private void loadCustomRoute(Intent intent) {
        double originLat = intent.getDoubleExtra("originLat", 0);
        double originLng = intent.getDoubleExtra("originLng", 0);
        double destLat = intent.getDoubleExtra("destLat", 0);
        double destLng = intent.getDoubleExtra("destLng", 0);

        if (originLat != 0 && destLat != 0) {
            challengeRoute.clear();
            challengeRoute.add(new LatLng(originLat, originLng));
            challengeRoute.add(new LatLng(destLat, destLng));

            challengeDistance = intent.getIntExtra("distance", 5000);
            long timeLimit = intent.getLongExtra("timeLimit", 1800);

            drawChallengeRoute();
            updateChallengeInfo("Carrera Personalizada", challengeDistance, timeLimit);
        }
    }

    private void loadDistanceRace(Intent intent) {
        int distance = intent.getIntExtra("distance", 5000);
        String raceMode = intent.getStringExtra("raceMode");
        boolean hasTimeGoal = intent.getBooleanExtra("hasTimeGoal", false);
        int goalTime = intent.getIntExtra("goalTime", 0);

        challengeDistance = distance;

        if (raceMode != null) {
            switch (raceMode) {
                case "circular":
                    generateCircularRoute();
                    break;
                case "outback":
                    generateOutBackRoute();
                    break;
                case "free":
                    challengeRoute.clear();
                    break;
            }
            updateChallengeInfo("Carrera: " + getModeDisplayName(raceMode), distance, hasTimeGoal ? goalTime : calculateSuggestedTime(distance));
        }
    }

    private void generateCircularRoute() {
        challengeRoute.clear();
        if (currentLocation == null) return;

        double radius = challengeDistance / (2 * Math.PI); // radius in meters
        int points = 16;
        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            // Simplified calculation; for real-world use, a geometry library is better
            double earthRadius = 6371000; // meters
            double lat = currentLocation.getLatitude() + (radius * Math.cos(angle)) / earthRadius * (180/Math.PI);
            double lng = currentLocation.getLongitude() + (radius * Math.sin(angle)) / earthRadius * (180/Math.PI) / Math.cos(currentLocation.getLatitude() * Math.PI/180);
            challengeRoute.add(new LatLng(lat, lng));
        }
        drawChallengeRoute();
    }

    private void generateOutBackRoute() {
        challengeRoute.clear();
        if (currentLocation == null) return;

        double halfDistance = challengeDistance / 2.0;
        double earthRadius = 6371000; // meters
        // Go east for simplicity
        double lngDelta = (halfDistance / earthRadius) * (180 / Math.PI) / Math.cos(currentLocation.getLatitude() * Math.PI / 180);
        LatLng midpoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude() + lngDelta);

        challengeRoute.add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        challengeRoute.add(midpoint);

        drawChallengeRoute();
    }

    private void updateChallengeInfo(String name, double distance, long timeLimit) {
        // You'll need to add these TextViews to your activity_running.xml
        // TextView tvChallengeName = findViewById(R.id.tvChallengeName);
        // TextView tvChallengeDetails = findViewById(R.id.tvChallengeDetails);
    }

    // --- FIX: Removed duplicate methods ---
    private String getModeDisplayName(String mode) {
        if (mode == null) return "Carrera";
        switch (mode) {
            case "circular": return "Ruta Circular";
            case "free": return "Run Free";
            case "outback": return "Ida y Vuelta";
            default: return "Distancia Fija";
        }
    }

    private int calculateSuggestedTime(int distance) {
        return (int) ((distance / 1000.0) * 6 * 60); // 6 min/km pace
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
