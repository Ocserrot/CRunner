package com.sebastiantorres.crunner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button btnDaily, btnWeekly, btnMonthly, btnBack;
    private TextView tvUserRank, tvUserName, tvUserPoints;
    private RecyclerView rvRanking;
    private RankingAdapter rankingAdapter;

    private List<RankingUser> rankingList = new ArrayList<>();
    private String currentFilter = "daily";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        loadRankingData();
    }

    private void initializeViews() {
        btnDaily = findViewById(R.id.btnDaily);
        btnWeekly = findViewById(R.id.btnWeekly);
        btnMonthly = findViewById(R.id.btnMonthly);
        btnBack = findViewById(R.id.btnBack);
        tvUserRank = findViewById(R.id.tvUserRank);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPoints = findViewById(R.id.tvUserPoints);
        rvRanking = findViewById(R.id.rvRanking);
    }

    private void setupRecyclerView() {
        rankingAdapter = new RankingAdapter(rankingList);
        rvRanking.setLayoutManager(new LinearLayoutManager(this));
        rvRanking.setAdapter(rankingAdapter);
    }

    private void setupClickListeners() {
        btnDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "daily";
                updateButtonStates();
                loadRankingData();
            }
        });

        btnWeekly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "weekly";
                updateButtonStates();
                loadRankingData();
            }
        });

        btnMonthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "monthly";
                updateButtonStates();
                loadRankingData();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateButtonStates() {
        btnDaily.setBackgroundColor(getResources().getColor(
                currentFilter.equals("daily") ? android.R.color.holo_blue_light : android.R.color.darker_gray));
        btnWeekly.setBackgroundColor(getResources().getColor(
                currentFilter.equals("weekly") ? android.R.color.holo_blue_light : android.R.color.darker_gray));
        btnMonthly.setBackgroundColor(getResources().getColor(
                currentFilter.equals("monthly") ? android.R.color.holo_blue_light : android.R.color.darker_gray));
    }

    private void loadRankingData() {
        rankingList.clear();

        // Consulta para obtener usuarios ordenados por puntos
        db.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int rank = 1;
                    String currentUserId = mAuth.getCurrentUser() != null ?
                            mAuth.getCurrentUser().getUid() : "";

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RankingUser user = new RankingUser();
                        user.setRank(rank);
                        user.setUserId(document.getId());
                        user.setUserName(document.getString("displayName"));
                        user.setPoints(document.getLong("totalPoints") != null ?
                                document.getLong("totalPoints").intValue() : 0);
                        user.setStreak(document.getLong("currentStreak") != null ?
                                document.getLong("currentStreak").intValue() : 0);

                        rankingList.add(user);

                        // Si es el usuario actual, actualizar UI
                        if (document.getId().equals(currentUserId)) {
                            updateCurrentUserUI(user);
                        }

                        rank++;
                    }

                    rankingAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RankingActivity.this,
                            "Error cargando ranking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("RankingActivity", "Error loading ranking", e);
                });
    }

    private void updateCurrentUserUI(RankingUser user) {
        tvUserRank.setText("#" + user.getRank());
        tvUserName.setText(user.getUserName());
        tvUserPoints.setText(user.getPoints() + " pts");
    }
}