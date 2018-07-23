package com.epam.jira.plugins.heatmap.dto;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ConfigPOJO {
    private List<String> projects = new LinkedList<>();


    private String project;

    private String labels;
    private int blocker = 0;
    private int critical = 0;
    private int major = 0;
    private int red = 10;
    private int amber = 1;
    private int cellsNumber = 50;

    public int getCellsNumber() {
        return cellsNumber;
    }

    public void setCellsNumber(int cellsNumber) {
        this.cellsNumber = cellsNumber;
    }

    private String highestPriorityName = "blocker";
    private String highPriorityName = "critical";
    private String middlePriorityName = "major";

    private static final int standardBlockerSLA = 3 * 24 * 3600;
    private static final int standardCriticalSLA = 5 * 24 * 3600;
    private static final int standardMajorSLA = 25 * 24 * 3600;

    public String getHighestPriorityName() {
        return highestPriorityName;
    }

    public void setHighestPriorityName(String highestPriorityName) {
        this.highestPriorityName = highestPriorityName;
    }

    public String getHighPriorityName() {
        return highPriorityName;
    }

    public void setHighPriorityName(String highPriorityName) {
        this.highPriorityName = highPriorityName;
    }

    public String getMiddlePriorityName() {
        return middlePriorityName;
    }

    public void setMiddlePriorityName(String middlePriorityName) {
        this.middlePriorityName = middlePriorityName;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void addProjects(String project) {
        if (project.contains(",")) {
            projects.addAll(Arrays.asList(project.replaceAll(" ", "").replaceAll("\\+", "").split(",")));

        } else {
            projects.add(project);
        }
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public int getBlocker() {
        return blocker;
    }

    public void setBlocker(int blocker) {
        this.blocker = blocker;
    }

    public int getCritical() {
        return critical;
    }

    public void setCritical(int critical) {
        this.critical = critical;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getAmber() {
        return amber;
    }

    public void setAmber(int amber) {
        this.amber = amber;
    }

    public int getStandardSlaTimeForPriority(String issuePriority) {
        if (issuePriority.equalsIgnoreCase(highestPriorityName)) {
            return standardBlockerSLA;
        }
        if (issuePriority.equalsIgnoreCase(highPriorityName)) {
            return standardCriticalSLA;
        }
        if (issuePriority.equalsIgnoreCase(middlePriorityName)) {
            return standardMajorSLA;
        }
        return 586;
    }

    public int getSlaTimeForPriority(String issuePriority) {
        if (issuePriority.equalsIgnoreCase(highestPriorityName)) {
            return blocker;
        }
        if (issuePriority.equalsIgnoreCase(highPriorityName)) {
            return critical;
        }
        if (issuePriority.equalsIgnoreCase(middlePriorityName)) {
            return major;
        }

        return 0;
    }

    public ConfigPOJO(HttpServletRequest request) {
        Map<String, String> queryMap = collectPropertiesFromQueryString(request.getQueryString());
        addProjects(queryMap.get("projects"));
        setLabels(queryMap.get("labels"));
        if (queryMap.get("highestPriorityName") != null) {
            setHighestPriorityName(queryMap.get("highestPriorityName"));
        }
        if (queryMap.get("highPriorityName") != null) {
            setHighPriorityName(queryMap.get("highPriorityName"));
        }
        if (queryMap.get("majorPriorityName") != null) {
            setMiddlePriorityName(queryMap.get("majorPriorityName"));
        }
        String block = queryMap.get(highestPriorityName);
        if (block != null && !block.isEmpty()) {
            blocker = Integer.parseInt(block);
        }
        String crit = queryMap.get(highestPriorityName);
        if (crit != null && !crit.isEmpty()) {
            this.critical = Integer.parseInt(crit);
        }
        String mj = queryMap.get(middlePriorityName);
        if (mj != null && !mj.isEmpty()) {
            major = Integer.parseInt(mj);
        }
        String red = queryMap.get("red");
        if (red != null && !red.isEmpty()) {
            this.red = Integer.parseInt(red);
        }
        String amber = queryMap.get("amber");
        if (amber != null && !amber.isEmpty()) {
            this.amber = Integer.parseInt(amber);
        }
        String cellsNumber = queryMap.get("cellsNumber");
        if(cellsNumber != null && !cellsNumber.isEmpty()){
            this.cellsNumber = Integer.parseInt(cellsNumber);
        }
    }


    private Map<String, String> collectPropertiesFromQueryString(String queryString) {
        Map<String, String> resultMap = new HashMap<>();
        Arrays.stream(queryString.split("&")).map(s -> s.replace("%2C", ",")).forEach(s -> addPropertieToMap(s, resultMap));
        return resultMap;
    }

    private void addPropertieToMap(String s, Map<String, String> resultMap) {
        String[] splittedString = s.split("=");
        if (splittedString.length != 1) {
            resultMap.put(splittedString[0], splittedString[1]);
        } else {
            resultMap.put(splittedString[0], null);
        }
    }
}
