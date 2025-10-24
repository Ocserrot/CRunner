package com.sebastiantorres.crunner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IntervalAdapter extends RecyclerView.Adapter<IntervalAdapter.IntervalViewHolder> {

    private List<TrainingInterval> intervals;
    private SimpleDateFormat timeFormat;

    public IntervalAdapter(List<TrainingInterval> intervals) {
        this.intervals = intervals;
        this.timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public IntervalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interval, parent, false);
        return new IntervalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IntervalViewHolder holder, int position) {
        TrainingInterval interval = intervals.get(position);
        holder.bind(interval);
    }

    @Override
    public int getItemCount() {
        return intervals.size();
    }

    static class IntervalViewHolder extends RecyclerView.ViewHolder {
        private TextView tvIntervalNumber, tvTime, tvSpeed, tvDistance, tvPace;

        public IntervalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIntervalNumber = itemView.findViewById(R.id.tvIntervalNumber);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSpeed = itemView.findViewById(R.id.tvSpeed);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvPace = itemView.findViewById(R.id.tvPace);
        }

        public void bind(TrainingInterval interval) {
            tvIntervalNumber.setText("Intervalo " + interval.getIntervalNumber());
            tvTime.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(interval.getTimestamp()));
            tvSpeed.setText(String.format("%.1f km/h", interval.getAverageSpeed()));
            tvDistance.setText(String.format("%.2f km", interval.getDistance()));
            tvPace.setText(String.format("%.1f min/km", interval.getPace()));
        }
    }
}