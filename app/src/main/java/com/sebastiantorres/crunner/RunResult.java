package com.sebastiantorres.crunner;

import java.io.Serializable;

public class RunResult implements Serializable {
    private long completionTime;
    private double distance;
    private String tier;
    private int pointsEarned;
    private long challengeTimeLimit;

    public RunResult() {}

    public RunResult(long completionTime, double distance, long challengeTimeLimit) {
        this.completionTime = completionTime;
        this.distance = distance;
        this.challengeTimeLimit = challengeTimeLimit;
    }

    // Getters y Setters
    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public long getChallengeTimeLimit() { return challengeTimeLimit; }
    public void setChallengeTimeLimit(long challengeTimeLimit) { this.challengeTimeLimit = challengeTimeLimit; }
}