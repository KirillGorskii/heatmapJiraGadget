package com.epam.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;

public interface RateScoreStatistic {

    void incrementRiskScore();

    void incrementRiskScore(int valueToIncrement);

    void incrementBlocker();

    void incrementCritical();

    void incrementMajor();

    Timestamp getRiscScoreDate();

    void incrementPriorityCounter(Issue name);

    int getBlocker();

    int getCritical();

    int getMajor();
}
