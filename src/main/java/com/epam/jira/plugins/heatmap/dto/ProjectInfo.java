package com.epam.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;
import java.time.Instant;

public class ProjectInfo implements RateScoreStatistic {
    private String projectName;
    private String color;
    private int riskScore =0;
    private String link;
    private int critical=0;
    private int blocker=0;
    private int major=0;
    private int minor=0;
    private int squareSize =0;

    public ProjectInfo(String projectName){
        this.projectName = projectName;
        //green as default
        this.color = "#9EDE00";
    }
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
        this.squareSize = riskScore;
    }

    public String getLink() {
        return link;
    }

    public void setSquareSize(int squareSize) {
        this.squareSize = squareSize;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getCritical() {
        return critical;
    }

    public void setCritical(int critical) {
        this.critical = critical;
    }

    public int getBlocker() {
        return blocker;
    }

    public void setBlocker(int blocker) {
        this.blocker = blocker;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void incrementRiskScore(int value){
        riskScore+=value;
        squareSize+=value;
    }

    public void incrementRiskScore(){
        riskScore++;
        squareSize++;
    }

    public void incrementBlocker() {
        blocker++;
    }

    public void incrementCritical() {
        critical++;
    }

    public void incrementMajor() {
        major++;
    }

    @Override
    public Timestamp getRiscScoreDate() {
        return Timestamp.from(Instant.now());
    }

    @Override
    public void incrementPriorityCounter(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighestPriorityName())) {
            incrementBlocker();
        }
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighPriorityName())) {
            incrementCritical();
        }
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getMiddlePriorityName())) {
            incrementMajor();
        }
    }

    public void incrementMinor() {
        minor++;
    }

    public int getSquareSize() {
        return squareSize;
    }

    public void incrementSquareSize(int minCalculated) {
        squareSize+=minCalculated;
    }
}
