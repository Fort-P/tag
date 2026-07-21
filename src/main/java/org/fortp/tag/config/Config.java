package org.fortp.tag.config;

public class Config {
    private int configVersion;
    private int warningSeconds;
    private int breakawaySeconds;
    private int campingSeconds;
    private int maxGameDurationSeconds;
    private int campingChunkRadius;
    private int scoreIntervalSeconds;
    private int basePointsPerInterval;
    private int tagBounty;
    private int forfeitPenalty;
    private double pityIncrement;
    private double mobilityLockoutDistanceSqr;

    public Config() {
        this.configVersion = ConfigManager.CURRENT_CONFIG_VERSION;
        this.warningSeconds = 300;
        this.breakawaySeconds = 15;
        this.campingSeconds = 60;
        this.maxGameDurationSeconds = 600;
        this.campingChunkRadius = 1;
        this.scoreIntervalSeconds = 60;
        this.basePointsPerInterval = 10;
        this.tagBounty = 50;
        this.forfeitPenalty = -50;
        this.pityIncrement = 0.5;
        this.mobilityLockoutDistanceSqr = 16384.0;
    }

    // Getters
    public int getConfigVersion() { return configVersion; }
    public int getWarningSeconds() { return warningSeconds; }
    public int getBreakawaySeconds() { return breakawaySeconds; }
    public int getCampingSeconds() { return campingSeconds; }
    public int getMaxGameDurationSeconds() { return maxGameDurationSeconds; }
    public int getCampingChunkRadius() { return campingChunkRadius; }
    public int getScoreIntervalSeconds() { return scoreIntervalSeconds; }
    public int getBasePointsPerInterval() { return basePointsPerInterval; }
    public int getTagBounty() { return tagBounty; }
    public int getForfeitPenalty() { return forfeitPenalty; }
    public double getPityIncrement() { return pityIncrement; }
    public double getMobilityLockoutDistanceSqr() { return mobilityLockoutDistanceSqr; }

    // Setters
    public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }
    public void setWarningSeconds(int warningSeconds) { this.warningSeconds = warningSeconds; }
    public void setBreakawaySeconds(int breakawaySeconds) { this.breakawaySeconds = breakawaySeconds; }
    public void setCampingSeconds(int campingSeconds) { this.campingSeconds = campingSeconds; }
    public void setMaxGameDurationSeconds(int maxGameDurationSeconds) { this.maxGameDurationSeconds = maxGameDurationSeconds; }
    public void setCampingChunkRadius(int campingChunkRadius) { this.campingChunkRadius = campingChunkRadius; }
    public void setScoreIntervalSeconds(int scoreIntervalSeconds) { this.scoreIntervalSeconds = scoreIntervalSeconds; }
    public void setBasePointsPerInterval(int basePointsPerInterval) { this.basePointsPerInterval = basePointsPerInterval; }
    public void setTagBounty(int tagBounty) { this.tagBounty = tagBounty; }
    public void setForfeitPenalty(int forfeitPenalty) { this.forfeitPenalty = forfeitPenalty; }
    public void setPityIncrement(double pityIncrement) { this.pityIncrement = pityIncrement; }
    public void setMobilityLockoutDistanceSqr(double mobilityLockoutDistanceSqr) { this.mobilityLockoutDistanceSqr = mobilityLockoutDistanceSqr; }
}
