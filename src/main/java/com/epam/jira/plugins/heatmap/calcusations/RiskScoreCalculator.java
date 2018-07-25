package com.epam.jira.plugins.heatmap.calcusations;

import com.atlassian.jira.issue.Issue;
import com.epam.jira.plugins.heatmap.dto.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public class RiskScoreCalculator {



    public ProjectInfo calculateRiskScore(String projectName, List<Issue> issues){
        ProjectInfo dto = new ProjectInfo(projectName);
        for (Issue issue : issues) {
            calculateRateScore(issue, dto);
        }
        calculateRateScoreBaseOnOverallData(dto);
        return dto;
    }

    private void calculateRateScoreBaseOnOverallData(RateScoreStatistic dto) {
        dto.incrementRiskScore(dto.getBlocker() * 10);
        dto.incrementRiskScore(dto.getCritical());
        dto.incrementRiskScore(dto.getMajor() / 20);
    }

    private void calculateRateScore(Issue issue, RateScoreStatistic projectPOJO) {
        Timestamp now = projectPOJO.getRiscScoreDate();
        int hoursBetweenDueDateAndCreated = getHoursForProirity(issue);
        long createdTime = issue.getCreated().getTime();
        if (hoursBetweenDueDateAndCreated == 0) {
            hoursBetweenDueDateAndCreated++;
        }
        long dueDate = createdTime + (hoursBetweenDueDateAndCreated * 60 * 60 * 1000);
        if (now.getTime() > dueDate) {
            projectPOJO.incrementPriorityCounter(issue);
            projectPOJO.incrementRiskScore();
        }
    }


    private int getHoursForProirity(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        int hours;
        hours = ConfigPOJO.getSlaTimeForPriority(issuePriority);

        if (hours == 0) {
            Timestamp due = issue.getDueDate();
            if (due != null) {
                long milesecondsBetweenDueAndCreated = due.getTime() - issue.getCreated().getTime();
                hours = Math.toIntExact(milesecondsBetweenDueAndCreated / (60 * 60 * 1000));
            } else {
                return ConfigPOJO.getStandardSlaTimeForPriority(issuePriority);
            }
        }
        return hours;
    }

    public ProjectInfoByDate calculateRiskScoreStatistic(List<Issue> issues,  LocalDate date) {
        ProjectInfoByDate projectInfoByDate = new ProjectInfoByDate(date);
        for(Issue issue: issues){
            calculateRateScore(issue, projectInfoByDate);
        }
        calculateRateScoreBaseOnOverallData(projectInfoByDate);
        return projectInfoByDate;
    }
}
