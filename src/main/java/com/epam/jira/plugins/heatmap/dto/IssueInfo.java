package com.epam.jira.plugins.heatmap.dto;

public class IssueInfo {
    private String issuePriority;
    private int calculatedRateScore;
    private String issueKey;
    private String color;

    IssueInfo(String name){
        issueKey = name;
    }

    public int getCalculatedRateScore() {
        return calculatedRateScore;
    }

    public void setCalculatedRateScore(int calculatedRateScore) {
        this.calculatedRateScore = calculatedRateScore;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getColor() {
        return color;
    }

    public void setColour() {
        if(calculatedRateScore >=5){
            color = ConfigPOJO.RED_COLOR;
        } else if(calculatedRateScore >= 2){
            color = ConfigPOJO.AMBER_COLOR;
        } else {
            color = ConfigPOJO.GREEN_COLOR;
        }
    }

    public String getIssuePriority() {

        return issuePriority;
    }

    public void setIssuePriority(String issuePriority) {
        this.issuePriority = issuePriority;
    }
}
