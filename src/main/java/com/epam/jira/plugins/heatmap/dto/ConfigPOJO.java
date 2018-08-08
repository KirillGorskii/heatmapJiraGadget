package com.epam.jira.plugins.heatmap.dto;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public  class ConfigPOJO {
    private static List<String> projects = new LinkedList<>();

    private static String labels;
    private static int blocker = 0;
    private static int critical = 0;
    private static int major = 0;
    private static int red = 10;
    private static int amber = 1;
    private static int cellsNumber = 50;
    private static String issueType = "vulnerability";

    public static final String RED_COLOR = "#AF2947";
    public static final String AMBER_COLOR = "#F0B400";
    public static final String GREEN_COLOR = "#7F9943";


    private static String highestPriorityName = "blocker";
    private static String highPriorityName = "critical";
    private static String middlePriorityName = "major";

    private static final int STANDARD_BLOCKER_SLA = 3 * 24 * 3600;
    private static final int STANDARD_CRITICAL_SLA = 5 * 24 * 3600;
    private static final int STANDARD_MAJOR_SLA = 25 * 24 * 3600;

    public static String getHighestPriorityName() {
        return highestPriorityName;
    }

    public static String getHighPriorityName() {
        return highPriorityName;
    }

    public static String getMiddlePriorityName() {
        return middlePriorityName;
    }

    public static List<String> getProjects() {
        return projects;
    }

    public static int getCellsNumber() {
        return cellsNumber;
    }

    private ConfigPOJO(){

    }

    private static void addProjects(String project) {
        if (project.contains(",")) {
            projects = Arrays.asList(project.replaceAll(" ", "").replaceAll("\\+", "").split(","));

        } else {
            projects = new ArrayList<>();
            projects.add(project);
        }
    }

    public static String getLabels() {
        return labels;
    }

    public static int getBlocker() {
        return blocker;
    }

    public static int getCritical() {
        return critical;
    }

    public static int getMajor() {
        return major;
    }

    public static int getRed() {
        return red;
    }

    public static int getAmber() {
        return amber;
    }

    public static int getStandardSlaTimeForPriority(String issuePriority) {
        if (issuePriority.equalsIgnoreCase(highestPriorityName)) {
            return STANDARD_BLOCKER_SLA;
        }
        if (issuePriority.equalsIgnoreCase(highPriorityName)) {
            return STANDARD_CRITICAL_SLA;
        }
        if (issuePriority.equalsIgnoreCase(middlePriorityName)) {
            return STANDARD_MAJOR_SLA;
        }
        return 0;
    }

    public static int getSlaTimeForPriority(String issuePriority) {
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

    public static void setConfigPOJO(HttpServletRequest request) {
        Map<String, String> queryMap = collectPropertiesFromQueryString(request.getQueryString());
        addProjects(queryMap.get("projects"));
        labels = queryMap.get("labels");
        if (queryMap.get("highestPriorityName") != null) {
            highestPriorityName = queryMap.get("highestPriorityName");
        }
        if (queryMap.get("highPriorityName") != null) {
            highPriorityName = queryMap.get("highPriorityName");
        }
        if (queryMap.get("majorPriorityName") != null) {
            middlePriorityName = queryMap.get("majorPriorityName");
        }
        String block = queryMap.get(highestPriorityName);
        if (block != null && !block.isEmpty()) {
            blocker = Integer.parseInt(block);
        }
        String crit = queryMap.get(highestPriorityName);
        if (crit != null && !crit.isEmpty()) {
            critical = Integer.parseInt(crit);
        }
        String mj = queryMap.get(middlePriorityName);
        if (mj != null && !mj.isEmpty()) {
            major = Integer.parseInt(mj);
        }
        String red = queryMap.get("red");
        if (red != null && !red.isEmpty()) {
            ConfigPOJO.red = Integer.parseInt(red);
        }
        String amber = queryMap.get("amber");
        if (amber != null && !amber.isEmpty()) {
            ConfigPOJO.amber = Integer.parseInt(amber);
        }
        String cellsNumber = queryMap.get("cellsNumber");
        if(cellsNumber != null && !cellsNumber.isEmpty()){
            ConfigPOJO.cellsNumber = Integer.parseInt(cellsNumber);
        }
        String issueType = queryMap.get("issueType");
        if(issueType != null ){
            ConfigPOJO.issueType = issueType;
        }
    }

    private static Map<String, String> collectPropertiesFromQueryString(String queryString) {
        Map<String, String> resultMap = new HashMap<>();
        Arrays.stream(queryString.split("&")).map(s -> s.replace("%2C", ",")).forEach(s -> addPropertieToMap(s, resultMap));
        return resultMap;
    }

    private static void addPropertieToMap(String s, Map<String, String> resultMap) {
        String[] splittedString = s.split("=");
        if (splittedString.length != 1) {
            resultMap.put(splittedString[0], splittedString[1]);
        } else {
            resultMap.put(splittedString[0], null);
        }
    }
}
