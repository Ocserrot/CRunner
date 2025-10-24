package com.sebastiantorres.crunner;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private List<RankingUser> rankingList;

    public RankingAdapter(List<RankingUser> rankingList) {
        this.rankingList = rankingList;
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        RankingUser user = rankingList.get(position);
        holder.bind(user);

        // Destacar top 3
        if (user.getRank() == 1) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFD700")); // Oro
        } else if (user.getRank() == 2) {
            holder.itemView.setBackgroundColor(Color.parseColor("#C0C0C0")); // Plata
        } else if (user.getRank() == 3) {
            holder.itemView.setBackgroundColor(Color.parseColor("#CD7F32")); // Bronce
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return rankingList.size();
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRank, tvName, tvPoints, tvStreak;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            tvStreak = itemView.findViewById(R.id.tvStreak);
        }

        public void bind(RankingUser user) {
            tvRank.setText("#" + user.getRank());
            tvName.setText(user.getUserName());
            tvPoints.setText(user.getPoints() + " pts");
            tvStreak.setText("Racha: " + user.getStreak() + " días");
        }
    }
}