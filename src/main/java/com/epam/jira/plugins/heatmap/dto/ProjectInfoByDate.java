package com.epam.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

public class ProjectInfoByDate implements RateScoreStatistic{

    private LocalDate dateOfRiskScore;
    private int riskScore = 1;
    private int major = 0;
    private int critical = 0;
    private int blocker = 0;

    ArrayList<IssueInfo> issues = new ArrayList<>();

    public ProjectInfoByDate(LocalDate date) {
        this.dateOfRiskScore = date;
    }

    public LocalDate getDateOfRiskScore() {
        return dateOfRiskScore;
    }

    public void setDateOfRiskScore(LocalDate dateOfRiskScore) {
        this.dateOfRiskScore = dateOfRiskScore;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
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

    @Override
    public void incrementRateScore() {
        riskScore++;
    }

    @Override
    public void incrementBlocker() {
        blocker++;
    }

    @Override
    public void incrementCritical() {
        critical++;
    }

    @Override
    public void incrementMajor() {
        major++;
    }

    @Override
    public Timestamp getRateScore() {
        return Timestamp.from(Instant.from(dateOfRiskScore));
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
}
