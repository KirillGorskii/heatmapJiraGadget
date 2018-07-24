package com.epam.jira.plugins.heatmap.dto;

import com.atlassian.jira.issue.Issue;

import java.sql.Timestamp;

public interface RateScoreStatistic {

    void incrementRateScore();

    void incrementBlocker();

    void incrementCritical();

    void incrementMajor();

    Timestamp getRateScore();

    void incrementPriorityCounter(Issue name);
}
