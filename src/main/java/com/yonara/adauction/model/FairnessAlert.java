package com.yonara.adauction.model;

/**
 * A single fairness violation detected by FairnessAuditor.
 * Persisted to the fairness_alerts table and published to the
 * fairness-alerts Kafka topic by FairnessAlertService.
 */
public class FairnessAlert {

    private String checkType;
    private String adCategory;
    private double disparityScore;
    private String flaggedGroup;

    public FairnessAlert() {}

    public FairnessAlert(String checkType, String adCategory,
                          double disparityScore, String flaggedGroup) {
        this.checkType = checkType;
        this.adCategory = adCategory;
        this.disparityScore = disparityScore;
        this.flaggedGroup = flaggedGroup;
    }

    public String getCheckType() { return checkType; }
    public String getAdCategory() { return adCategory; }
    public double getDisparityScore() { return disparityScore; }
    public String getFlaggedGroup() { return flaggedGroup; }

    public void setCheckType(String checkType) { this.checkType = checkType; }
    public void setAdCategory(String adCategory) { this.adCategory = adCategory; }
    public void setDisparityScore(double disparityScore) { this.disparityScore = disparityScore; }
    public void setFlaggedGroup(String flaggedGroup) { this.flaggedGroup = flaggedGroup; }

    @Override
    public String toString() {
        return "FairnessAlert{checkType='" + checkType + "', adCategory='" + adCategory +
            "', disparityScore=" + disparityScore + ", flaggedGroup='" + flaggedGroup + "'}";
    }
}