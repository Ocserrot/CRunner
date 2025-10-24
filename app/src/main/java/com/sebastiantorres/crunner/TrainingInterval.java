package com.sebastiantorres.crunner;

import java.util.Date;

public class TrainingInterval {
    private int intervalNumber;
    private Date timestamp;
    private double averageSpeed; // km/h
    private double distance; // km
    private double pace; // min/km
    private double calories;

    public TrainingInterval() {
        // Constructor vacío necesario para Firebase/GSON
    }

    public TrainingInterval(int intervalNumber, Date timestamp) {
        this.intervalNumber = intervalNumber;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public int getIntervalNumber() {
        return intervalNumber;
    }

    public void setIntervalNumber(int intervalNumber) {
        this.intervalNumber = intervalNumber;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getPace() {
        return pace;
    }

    public void setPace(double pace) {
        this.pace = pace;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    // Método para calcular calorías estimadas (fórmula simplificada)
    public double calculateCalories(double weightKg) {
        // Fórmula aproximada: 1 kcal por kg de peso por km recorrido
        return weightKg * distance * 1.0;
    }

    @Override
    public String toString() {
        return String.format("Intervalo %d: %.1f km/h, %.2f km, %.1f min/km",
                intervalNumber, averageSpeed, distance, pace);
    }
}