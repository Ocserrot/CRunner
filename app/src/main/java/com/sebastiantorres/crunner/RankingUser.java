package com.sebastiantorres.crunner;

public class RankingUser {
    private int rank;
    private String userId;
    private String userName;
    private int points;
    private int streak;

    public RankingUser() {}

    public RankingUser(int rank, String userId, String userName, int points, int streak) {
        this.rank = rank;
        this.userId = userId;
        this.userName = userName;
        this.points = points;
        this.streak = streak;
    }

    // Getters y Setters
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}