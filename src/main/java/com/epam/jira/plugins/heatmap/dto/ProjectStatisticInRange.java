package com.epam.jira.plugins.heatmap.dto;


import com.atlassian.jira.issue.Issue;
import com.epam.jira.plugins.heatmap.calcusations.RiskScoreCalculator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProjectStatisticInRange {
    private String projectName;
    private ArrayList<ProjectInfoByDate> projectInfoByDates = new ArrayList<>();

    public ProjectStatisticInRange(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<ProjectInfoByDate> getProjectInfoByDates() {
        return projectInfoByDates;
    }


    public void calculateRiskScore(List<Issue> issues, LocalDate calculationDate){
        RiskScoreCalculator calculator = new RiskScoreCalculator();
        projectInfoByDates.add(calculator.calculateRiskScoreStatistic(issues, calculationDate));
    }

}
