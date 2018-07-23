package com.epam.jira.plugins.heatmap.dto;

import java.time.LocalDate;
import java.util.List;

public class ProjectStatisticPOJO {
    private String projectName;
    private List<ProjectInfoByDate> projectInfoByDates;


    class ProjectInfoByDate{
        private LocalDate dateOfRiskScore;
        private int riskScore = 1;
        private int majorIssuesCount = 0;
        private int criticalIssuesCount = 0;
        private int blockerIssuesCount = 0;

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

        public int getMajorIssuesCount() {
            return majorIssuesCount;
        }

        public void setMajorIssuesCount(int majorIssuesCount) {
            this.majorIssuesCount = majorIssuesCount;
        }

        public int getCriticalIssuesCount() {
            return criticalIssuesCount;
        }

        public void setCriticalIssuesCount(int criticalIssuesCount) {
            this.criticalIssuesCount = criticalIssuesCount;
        }

        public int getBlockerIssuesCount() {
            return blockerIssuesCount;
        }

        public void setBlockerIssuesCount(int blockerIssuesCount) {
            this.blockerIssuesCount = blockerIssuesCount;
        }
    }
}
