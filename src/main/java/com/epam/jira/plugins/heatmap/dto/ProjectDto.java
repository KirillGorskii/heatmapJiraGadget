package com.epam.jira.plugins.heatmap.dto;

public class ProjectDto {
    private String projectName;
    private String color;
    private int risk_score=0;
    private String link;
    private int critical=0;
    private int blocker=0;
    private int major=0;
    private int minor=0;

    public ProjectDto(String projectName){
        this.projectName = projectName;
        //green as default
        this.color = "#9EDE00";
    }
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getRisk_score() {
        return risk_score;
    }

    public void setRisk_score(int risk_score) {
        this.risk_score = risk_score;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void incrementRateScore(int value){
        risk_score+=value;
    }

    public void increntBlocer() {
        blocker++;
    }

    public void incrementCritical() {
        critical++;
    }

    public void incrementMajor() {
        major++;
    }

    public void incrementMinor() {
        minor++;
    }
}
