package com.sebastiantorres.crunner;

import static com.google.ar.sceneform.rendering.Color.*;

import android.annotation.SuppressLint;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.ar.sceneform.rendering.Color;
import com.google.maps.android.PolyUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CustomRaceActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText etSearch;
    private ImageButton btnCurrentLocation;
    private CardView cardRouteInfo;
    private TextView tvDistance, tvDuration, tvSuggestedTime, tvInstructions;
    private RadioGroup radioTimeOptions;
    private RadioButton radioSuggested, radioCustom;
    private LinearLayout layoutCustomTime;
    private NumberPicker npHours, npMinutes, npSeconds;
    private Button btnStartRace;

    private LatLng selectedDestination = null;
    private double calculatedDistance = 0;
    private LatLng currentLocation;
    private Polyline routePolyline;
    private Marker destinationMarker;

    private double routeDistance = 0.0; // metros
    private int walkingDuration = 0; // minutos
    private int runningDuration = 0; // minutos

    private static final String DIRECTIONS_API_KEY = "TU_API_KEY_DIRECTIONS"; // Misma que Maps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_race);

        NumberPicker npHours = findViewById(R.id.npHours);
        NumberPicker npMinutes = findViewById(R.id.npMinutes);
        NumberPicker npSeconds = findViewById(R.id.npSeconds);

        // Set the min and max values programmatically
        npHours.setMinValue(0);
        npHours.setMaxValue(5);

        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);

        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        initializeViews();
        setupMap();
        setupLocationClient();
        setupNumberPickers();

    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        cardRouteInfo = findViewById(R.id.cardRouteInfo);
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);
        tvSuggestedTime = findViewById(R.id.tvSuggestedTime);
        tvInstructions = findViewById(R.id.tvInstructions);
        radioTimeOptions = findViewById(R.id.radioTimeOptions);
        radioSuggested = findViewById(R.id.radioSuggested);
        radioCustom = findViewById(R.id.radioCustom);
        layoutCustomTime = findViewById(R.id.layoutCustomTime);
        npHours = findViewById(R.id.npHours);
        npMinutes = findViewById(R.id.npMinutes);
        npSeconds = findViewById(R.id.npSeconds);
        btnStartRace = findViewById(R.id.btnStartRace);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Buscar dirección
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        // Ir a ubicación actual
        btnCurrentLocation.setOnClickListener(v -> {
            if (currentLocation != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
            } else {
                Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show();
                getCurrentLocation();
            }
        });

        // Selector de tiempo
        radioTimeOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCustom) {
                layoutCustomTime.setVisibility(View.VISIBLE);
            } else {
                layoutCustomTime.setVisibility(View.GONE);
            }
        });

        // Iniciar la wea de carrera
        btnStartRace.setOnClickListener(v -> {
            // --- MODIFICA LA LÓGICA AQUÍ ---
            if (selectedDestination != null && currentLocation != null) {
                Intent intent = new Intent(CustomRaceActivity.this, RunningActivity.class);
                intent.putExtra("raceType", "custom"); // Identifica el tipo de carrera
                intent.putExtra("originLat", currentLocation.latitude);
                intent.putExtra("originLng", currentLocation.longitude);
                intent.putExtra("destLat", selectedDestination.latitude);
                intent.putExtra("destLng", selectedDestination.longitude);
                intent.putExtra("distance", (int) calculatedDistance); // Envía la distancia en metros

                // Obtén el límite de tiempo si lo hay
                long timeLimit = 0; // en segundos
                AssistStructure.ViewNode cbTimeGoal = null;
                if (cbTimeGoal.isChecked()) {
                    timeLimit = (npHours.getValue() * 3600) + (npMinutes.getValue() * 60) + npSeconds.getValue();
                }
                intent.putExtra("timeLimit", timeLimit);

                startActivity(intent);
            } else {
                Toast.makeText(this, "No se seleccionó un destino válido.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNumberPickers() {
        npHours.setMinValue(0);
        npHours.setMaxValue(5);
        npHours.setValue(0);

        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npMinutes.setValue(30);

        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);
        npSeconds.setValue(0);

        // Formatear display
        npHours.setFormatter(value -> String.format("%02d", value));
        npMinutes.setFormatter(value -> String.format("%02d", value));
        npSeconds.setFormatter(value -> String.format("%02d", value));
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

        // Configuraciones del mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Usamos nuestro botón
        mMap.getUiSettings().setCompassEnabled(true);

        // Listener para clicks en el mapa
        mMap.setOnMapClickListener(this);

        // Obtener ubicación actual
        getCurrentLocation();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!LocationPermissionHelper.hasLocationPermission(this)) {
            LocationPermissionHelper.requestLocationPermission(this);
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));

                        // Añadir marcador de ubicación actual
                        mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Tu ubicación")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onMapClick(LatLng destination) {
        // 1. Limpiar marcadores y rutas anteriores
        mMap.clear();

        // 2. Añadir un marcador en el punto de destino
        mMap.addMarker(new MarkerOptions().position(destination).title("Destino"));

        // 3. Obtener la ubicación actual del usuario como origen
        if (currentLocation != null) {
            // --- SOLUCIÓN ---
            // 'currentLocation' ya es el origen que necesitas.
            LatLng origin = currentLocation;

            selectedDestination = destination; // Guarda el destino

            // Colocar un marcador en el origen también es útil
            // (Asegúrate de volver a poner el marcador de tu ubicación actual que se borró con mMap.clear())
            mMap.addMarker(new MarkerOptions()
                    .position(origin)
                    .title("Tu ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            // 4. Calcular y dibujar la ruta
            fetchRoute(origin, destination);

            // 5. Ocultar las instrucciones iniciales
            TextView tvInstructions = findViewById(R.id.tvInstructions);
            if (tvInstructions != null) {
                tvInstructions.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "No se puede obtener la ubicación actual. Activa el GPS.", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchRoute(LatLng origin, LatLng destination) {
        String apiKey = "AIzaSyBEeTi2tCfLeIHCrgz_jIJ6FEF2sBaph5Q"; // ¡IMPORTANTE! Mueve esto a un lugar seguro, no lo dejes en el código.
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + origin.latitude + "," + origin.longitude + "&destination="
                + destination.latitude + "," + destination.longitude
                + "&mode=walking" // Modo caminata, ideal para correr
                + "&key=" + apiKey;

        // Ejecutar la petición de red en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                // Este es un ejemplo muy básico de petición de red.
                // Es MEJOR usar Volley, Retrofit, o OkHttp.
                URL requestUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Scanner s = new Scanner(in).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";

                // Procesar la respuesta JSON
                JSONObject json = new JSONObject(result);
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = overviewPolyline.getString("points");

                    // Decodificar la polilínea y dibujarla en el mapa (en el hilo principal)
                    List<LatLng> decodedPath = PolyUtil.decode(encodedPolyline);
                    runOnUiThread(() -> {
                        mMap.addPolyline(new PolylineOptions().addAll(decodedPath).width(12f).color(android.graphics.Color.BLUE));

                        // Actualizar la UI con la información de la ruta
                        updateRouteInfo(route);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(CustomRaceActivity.this, "Error al obtener la ruta.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Método auxiliar para actualizar la tarjeta de información
    private void updateRouteInfo(JSONObject route) {
        try {
            JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
            String distance = leg.getJSONObject("distance").getString("text");
            String duration = leg.getJSONObject("duration").getString("text");

            // Calcula el tiempo sugerido para correr (ej: a 6 min/km)
            double distanceInMeters = leg.getJSONObject("distance").getDouble("value");
            int suggestedTimeInMinutes = (int) ((distanceInMeters / 1000.0) * 6);

            calculatedDistance = leg.getJSONObject("distance").getDouble("value"); // Distancia en metros

            // Muestra la información en la UI
            TextView tvDistance = findViewById(R.id.tvDistance);
            TextView tvDuration = findViewById(R.id.tvDuration);
            TextView tvSuggestedTime = findViewById(R.id.tvSuggestedTime);

            tvDistance.setText(distance);
            tvDuration.setText(duration);
            tvSuggestedTime.setText(suggestedTimeInMinutes + " min");

            // Mostrar la tarjeta y habilitar el botón de inicio
            findViewById(R.id.cardRouteInfo).setVisibility(View.VISIBLE);
            findViewById(R.id.btnStartRace).setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void searchLocation(String query) {
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Ingresa una dirección", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());

                // Mover mapa a la ubicación
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

                // Simular click en el mapa
                onMapClick(location);

            } else {
                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error buscando dirección", Toast.LENGTH_SHORT).show();
            Log.e("CustomRace", "Error en geocoding", e);
        }
    }

    private void calculateRoute(LatLng origin, LatLng destination) {
        new CalculateRouteTask().execute(origin, destination);
    }

    private class CalculateRouteTask extends AsyncTask<LatLng, Void, String> {

        @Override
        protected String doInBackground(LatLng... latLngs) {
            try {
                LatLng origin = latLngs[0];
                LatLng destination = latLngs[1];

                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin.latitude + "," + origin.longitude +
                        "&destination=" + destination.latitude + "," + destination.longitude +
                        "&mode=walking" + // Modo caminando para rutas peatonales
                        "&key=" + DIRECTIONS_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();

            } catch (Exception e) {
                Log.e("CustomRace", "Error calculating route", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                parseRouteData(result);
            } else {
                Toast.makeText(CustomRaceActivity.this, "Error calculando ruta", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void parseRouteData(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            JSONArray routes = json.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);

                // Obtener distancia y duración
                JSONObject distance = leg.getJSONObject("distance");
                JSONObject duration = leg.getJSONObject("duration");

                routeDistance = distance.getDouble("value"); // metros
                walkingDuration = duration.getInt("value") / 60; // minutos

                // Calcular duración corriendo (asumiendo 3x más rápido que caminando)
                runningDuration = Math.max(1, walkingDuration / 3);

                // Obtener polyline
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String points = overviewPolyline.getString("points");
                List<LatLng> decodedPath = PolyUtil.decode(points);

                // Dibujar ruta en el mapa
                drawRoute(decodedPath);

                // Mostrar información
                updateRouteInfo();

            } else {
                Toast.makeText(this, "No se encontró ruta", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("CustomRace", "Error parsing route data", e);
            Toast.makeText(this, "Error procesando ruta", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(List<LatLng> path) {
        // Remover polyline anterior
        if (routePolyline != null) {
            routePolyline.remove();
        }

        // Dibujar nueva ruta
        routePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(path)
                .width(12f)
                .color(0xFF4285F4) // Azul Google
                .geodesic(true));

        // Ajustar zoom para mostrar toda la ruta
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(currentLocation);
        builder.include(selectedDestination);
        for (LatLng point : path) {
            builder.include(point);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private void updateRouteInfo() {
        double distanceKm = routeDistance / 1000;

        tvDistance.setText(String.format("%.1f km", distanceKm));
        tvDuration.setText(String.format("%d min", walkingDuration));
        tvSuggestedTime.setText(String.format("%d min", runningDuration));

        // Configurar tiempo personalizado por defecto
        int suggestedMinutes = runningDuration;
        npHours.setValue(suggestedMinutes / 60);
        npMinutes.setValue(suggestedMinutes % 60);
        npSeconds.setValue(0);

        // Mostrar panel de información
        cardRouteInfo.setVisibility(View.VISIBLE);
        btnStartRace.setEnabled(true);
    }

    private void startCustomRace() {
        if (selectedDestination == null || currentLocation == null) {
            Toast.makeText(this, "Selecciona un destino primero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener tiempo seleccionado
        int selectedTimeSeconds = getSelectedTimeInSeconds();

        // Crear intent para la carrera
        Intent intent = new Intent(this, RunningActivity.class);
        intent.putExtra("mode", "custom");
        intent.putExtra("originLat", currentLocation.latitude);
        intent.putExtra("originLng", currentLocation.longitude);
        intent.putExtra("destLat", selectedDestination.latitude);
        intent.putExtra("destLng", selectedDestination.longitude);
        intent.putExtra("distance", (int) routeDistance);
        intent.putExtra("timeLimit", selectedTimeSeconds);
        intent.putExtra("suggestedTime", runningDuration * 60); // segundos

        startActivity(intent);
    }

    private int getSelectedTimeInSeconds() {
        if (radioSuggested.isChecked()) {
            return runningDuration * 60; // segundos
        } else {
            int hours = npHours.getValue();
            int minutes = npMinutes.getValue();
            int seconds = npSeconds.getValue();
            return (hours * 3600) + (minutes * 60) + seconds;
        }
    }

    private void setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }
}