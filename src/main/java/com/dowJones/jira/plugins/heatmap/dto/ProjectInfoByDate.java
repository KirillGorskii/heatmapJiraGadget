package com.dowJones.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;
import java.time.Duration;
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
        IssueInfo issueInfo = collectMainStatisticFromIssue(issue);
        issueInfo.setIssuePriority(issuePriority);
        int days = getDays(issue);
        int valueToIncerment;
        issueInfo.setIssueExpiration(days);
        if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighestPriorityName())) {
            valueToIncerment = days;
            incrementBlocker();
            issueInfo.setCalculatedRateScore(valueToIncerment + 10);
        } else if (issuePriority.equalsIgnoreCase(ConfigPOJO.getHighPriorityName())) {
            valueToIncerment = (int)(0.5*days);
            incrementCritical();
            issueInfo.setCalculatedRateScore(valueToIncerment + 1);
        } else if (issuePriority.equalsIgnoreCase(ConfigPOJO.getMiddlePriorityName())) {
            valueToIncerment = (int)(0.1*days);
            incrementMajor();
            issueInfo.setCalculatedRateScore(valueToIncerment);
        }
        issueInfo.setColour();
        issues.add(issueInfo);
    }

    private IssueInfo collectMainStatisticFromIssue(Issue issue){
         return new IssueInfo().setIssueKey(issue.getKey()).setAssignee(issue.getAssignee()).setSummary(issue.getSummary());
    }

    private int getDays(Issue issue){
        Timestamp due = issue.getDueDate();

        if (due != null) {
            return (int) Duration.between(due.toInstant(),dateOfRiskScore.atStartOfDay().toInstant(ZoneOffset.UTC)).toDays();
        } else {
            long dueDate = issue.getCreated().getTime() + ConfigPOJO.getStandardSlaTimeForPriority(issue.getPriority().getName())* 60 * 60 * 1000;
            return (int) Duration.between(Instant.ofEpochMilli(dueDate),dateOfRiskScore.atStartOfDay().toInstant(ZoneOffset.UTC)).toDays();
        }
    }
}
