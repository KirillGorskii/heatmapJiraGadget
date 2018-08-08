package com.dowJones.jira.plugins.heatmap.dto;

import com.atlassian.jira.user.ApplicationUser;

public class IssueInfo {
    private String issuePriority;
    private int calculatedRateScore;
    private String issueKey;
    private String color;
    private int issueExpiration;
    private String assignee;
    private String summary;

    public String getSummary() {
        return summary;
    }

    public IssueInfo setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public int getCalculatedRateScore() {
        return calculatedRateScore;
    }

    public IssueInfo setCalculatedRateScore(int calculatedRateScore) {
        this.calculatedRateScore = calculatedRateScore;
        return this;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public IssueInfo setIssueKey(String issueKey) {
        this.issueKey = issueKey;
        return this;
    }

    public String getColor() {
        return color;
    }

    public void setColour() {
        if(calculatedRateScore >=5){
            color = "red";
        } else if(calculatedRateScore >= 2){
            color = "amber";
        } else {
            color = "green";
        }
    }

    public String getIssuePriority() {

        return issuePriority;
    }

    public IssueInfo setIssuePriority(String issuePriority) {
        this.issuePriority = issuePriority;
        return this;
    }


    public int getIssueExpiration() {
        return issueExpiration;
    }

    public IssueInfo setIssueExpiration(int issueExpiration) {
        this.issueExpiration = issueExpiration;
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public IssueInfo setAssignee(ApplicationUser assignee) {
        if(assignee==null){
            this.assignee="unassigned";
        } else {
            this.assignee = assignee.getDisplayName();
        }
        return this;
    }


}
