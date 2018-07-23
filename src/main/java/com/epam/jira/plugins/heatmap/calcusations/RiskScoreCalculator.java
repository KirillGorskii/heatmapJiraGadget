package com.epam.jira.plugins.heatmap.calcusations;

import com.atlassian.jira.issue.Issue;
import com.epam.jira.plugins.heatmap.dto.ConfigPOJO;
import com.epam.jira.plugins.heatmap.dto.ProjectPOJO;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class RiskScoreCalculator {

    private ConfigPOJO configPOJO;

    public RiskScoreCalculator(ConfigPOJO configPOJO){
        this.configPOJO = configPOJO;
    }

    public ProjectPOJO calculateRiskScore(String projectName, List<Issue> issues){
        ProjectPOJO dto = new ProjectPOJO(projectName);
        for (Issue issue : issues) {
            calculateRateScore(issue, dto);
        }
        calculateRateScoreBaseOnOverallData(dto);

        return dto;
    }

    private void calculateRateScoreBaseOnOverallData(ProjectPOJO dto) {
        dto.incrementRateScore(dto.getBlocker() * 10);
        dto.incrementRateScore(dto.getCritical());
        dto.incrementRateScore(dto.getMajor() / 20);
    }

    private void calculateRateScore(Issue issue, ProjectPOJO projectPOJO) {
        Timestamp now = Timestamp.from(Instant.now());
        int hoursBetweenDueDateAndCreated = getHoursForProirity(issue);
        long createdTime = issue.getCreated().getTime();
        if (hoursBetweenDueDateAndCreated == 0) {
            hoursBetweenDueDateAndCreated++;
        }
        long dueDate = createdTime + (hoursBetweenDueDateAndCreated * 60 * 60 * 1000);
        if (now.getTime() > dueDate) {
            incrementPriorityCounter(issue.getPriority().getName(), projectPOJO);
            projectPOJO.incrementRateScore();
        }
    }

    private void incrementPriorityCounter(String issuePriority, ProjectPOJO dto) {
        if (issuePriority.equalsIgnoreCase(configPOJO.getHighestPriorityName())) {
            dto.increntBlocker();
        }
        if (issuePriority.equalsIgnoreCase(configPOJO.getHighPriorityName())) {
            dto.incrementCritical();
        }
        if (issuePriority.equalsIgnoreCase(configPOJO.getMiddlePriorityName())) {
            dto.incrementMajor();
        }
    }

    private int getHoursForProirity(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        int hours = 0;
        hours = configPOJO.getSlaTimeForPriority(issuePriority);

        if (hours == 0) {
            Timestamp due = issue.getDueDate();
            if (due != null) {
                long milesecondsBetweenDueAndCreated = due.getTime() - issue.getCreated().getTime();
                hours = Math.toIntExact(milesecondsBetweenDueAndCreated / (60 * 60 * 1000));
            } else {
                return configPOJO.getStandardSlaTimeForPriority(issuePriority);
            }
        }
        return hours;
    }


}
