package com.epam.jira.plugins.heatmap.dto;


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

    public void addProjectInfoByDate(ProjectInfoByDate projectInfoByDate){
        projectInfoByDates.add(projectInfoByDate);
    }

}
