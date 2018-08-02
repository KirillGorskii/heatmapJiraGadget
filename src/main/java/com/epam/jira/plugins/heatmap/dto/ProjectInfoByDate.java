package com.epam.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ProjectInfoByDate implements RateScoreStatistic{

    private LocalDate dateOfRiskScore;
    private int riskScore = 0;
    private int major = 0;
    private int critical = 0;
    private int blocker = 0;

    ArrayList<IssueInfo> issues = new ArrayList<>();

    public List<IssueInfo> getIssues() {
        return issues;
    }

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
    public void incrementRiskScore() {
        riskScore++;
    }

    @Override
    public void incrementRiskScore(int valueToIncrement) {
        riskScore+=valueToIncrement;
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
    public Timestamp getRiscScoreDate() {
        return Timestamp.from(Instant.from(dateOfRiskScore.atStartOfDay().toInstant(ZoneOffset.UTC)));
    }

    @Override
    public void incrementPriorityCounter(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        IssueInfo issueInfo = new IssueInfo(issue.getKey()) ;
        issueInfo.setIssuePriority(issuePriority);
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighestPriorityName())) {
            incrementBlocker();
            issueInfo.setCalculatedRateScore(1 + 10);
        }
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighPriorityName())) {
            incrementCritical();
            issueInfo.setCalculatedRateScore(1 + 1);
        }
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getMiddlePriorityName())) {
            incrementMajor();
            issueInfo.setCalculatedRateScore(1);
        }
        issueInfo.setColour();
        issues.add(issueInfo);
    }
}
