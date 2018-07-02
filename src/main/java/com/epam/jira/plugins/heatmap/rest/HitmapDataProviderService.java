package com.epam.jira.plugins.heatmap.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.epam.jira.plugins.heatmap.dto.ProjectDto;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;


@Path("gadget/heatmap")
@Scanned
public class HitmapDataProviderService {
    @ComponentImport
    private final SearchService searchService;
    @ComponentImport
    private final UserManager manager;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    private static final String highestPriorityName = "blocker";
    private static final String highPriorityName = "critical";
    private static final String middlePriorityName = "major";
    private ConfigDTO configDto;
    private String jiraUrl;

    @Inject
    public HitmapDataProviderService(UserManager userManager, TransactionTemplate transactionTemplate, SearchService searchService) {
        this.manager = userManager;
        this.transactionTemplate = transactionTemplate;
        this.searchService = searchService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        setConfigDto(request);
        jiraUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        List<ProjectDto> result = getPropertiesUser(request);
        if (result == null || result.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(transactionTemplate.execute(() -> new Gson().toJson(result))).build();
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

    private List<ProjectDto> getPropertiesUser(HttpServletRequest request) {
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(manager.getRemoteUsername(request));
        List<ProjectDto> results = new ArrayList<>();
        for (String project : configDto.getProjects()) {
            ProjectDto dto = new ProjectDto(project);
            dto.setLink(collectLinkToProject(project));
            List<Issue> issues = getListOfIsses(project, applicationUser);
            for (Issue issue : issues) {
                calculateRateScore(issue, dto);
            }
            calculateRateScoreBaseOnOverallData(dto);
            setColour(dto, configDto);
            if (dto.getRisk_score() == 0) {
                dto.incrementRateScore(1);
            }
            results.add(dto);
        }
        return results;
    }

    private void setColour(ProjectDto dto, ConfigDTO configDTO) {
        if (dto.getRisk_score() > configDTO.getRed()) {
            dto.setColor("#AF2947");
        } else if (dto.getRisk_score() > configDTO.getAmber()) {
            dto.setColor("#F0B400");
        } else {
            dto.setColor("#7F9943");
        }
    }

    private void calculateRateScoreBaseOnOverallData(ProjectDto dto) {
        dto.incrementRateScore( dto.getBlocker() * 10);
        dto.incrementRateScore(dto.getCritical());
        dto.incrementRateScore(dto.getMajor() / 20);
    }

    private void calculateRateScore(Issue issue, ProjectDto projectDto) {
        Timestamp now = Timestamp.from(Instant.now());
        int hoursBetweenDueDateAndCreated = getHoursForProirity(issue);
        long createdTime = issue.getCreated().getTime();
        if (hoursBetweenDueDateAndCreated == 0) {
            hoursBetweenDueDateAndCreated++;
        }
        long dueDate = createdTime + (hoursBetweenDueDateAndCreated * 60 * 60 * 1000);
        if (now.getTime() > dueDate) {
            incrementPriorityCounter(issue.getPriority().getName(), projectDto);
            projectDto.incrementRateScore(1);
        }
    }

    private void incrementPriorityCounter(String issuePriority, ProjectDto dto) {
        if (issuePriority.equalsIgnoreCase(highestPriorityName)) {
            dto.increntBlocker();
        }
        if (issuePriority.equalsIgnoreCase(highPriorityName)) {
            dto.incrementCritical();
        }
        if (issuePriority.equalsIgnoreCase(middlePriorityName)) {
            dto.incrementMajor();
        }
    }

    private int getHoursForProirity(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        int hours = 0;
        hours = configDto.getSlaTimeForPriority(issuePriority);

        if (hours == 0) {
            Timestamp due = issue.getDueDate();
            if(due!=null){
                long milesecondsBetweenDueAndCreated = due.getTime() - issue.getCreated().getTime();
                hours = Math.toIntExact(milesecondsBetweenDueAndCreated / (60 * 60 * 1000));
            } else{
                return configDto.getStandardSlaTimeForPriority(issuePriority);
            }
        }
        return hours;
    }

    private String collectLinkToProject(String project) {
        StringBuilder builder = new StringBuilder();
        builder.append(jiraUrl).append("/issues/?jql=project%20%3D%20").append(project)
                .append("%20and%20priority%20in%20(Blocker%2C%20Critical%2C%20Major)%20and%20status%20not%20in%20(Closed%2C%20Resolved)");
        if (configDto.getLabels() != null &&configDto.getLabels().length()>0&&!configDto.getLabels().equals("-")) {
            builder.append("%20and%20labels%20in%20(").append(configDto.getLabels()).append(")");
        }
        return builder.toString();
    }

    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("project = '").append(projectKey).append("' AND priority IN (Blocker, Critical, Major) AND status not in (Closed, Resolved)");
            if (configDto.getLabels() != null &&configDto.getLabels().length()>0&&!configDto.getLabels().equals("-")) {
                builder.append("and labels in (").append(configDto.getLabels()).append(")");
            }
            query = parser.parseQuery(builder.toString());
        } catch (JqlParseException e) {
            e.printStackTrace();
        }
        List<Issue> issues = null;
        try {
            SearchResults results = searchService.search(applicationUser, query, PagerFilter.getUnlimitedFilter());
            issues = results.getIssues();
        } catch (SearchException e) {
            e.printStackTrace();
            issues = new ArrayList<>();
        }
        return issues;
    }
    private void setConfigDto(HttpServletRequest request){
        Map<String, String> queryMap = collectPropertiesFromQueryString(request.getQueryString());
        configDto = new ConfigDTO();
        configDto.addProjects(queryMap.get("projects"));
        configDto.setLabels(queryMap.get("labels"));

        String blocker = queryMap.get(highestPriorityName);
        if (blocker != null && !blocker.isEmpty()) {
            configDto.setBlocker(Integer.parseInt(blocker));
        }
        String critical = queryMap.get(highPriorityName);
        if (critical != null && !critical.isEmpty()) {
            configDto.setCritical(Integer.parseInt(critical));
        }
        String major = queryMap.get(middlePriorityName);
        if (major != null && !major.isEmpty()) {
            configDto.setMajor(Integer.parseInt(major));
        }

        String red = queryMap.get("red");
        if (red != null && !red.isEmpty()) {
            configDto.setRed(Integer.parseInt(red));
        }
        String amber = queryMap.get("amber");
        if (amber != null && !amber.isEmpty()) {
            configDto.setAmber(Integer.parseInt(amber));
        }
    }
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class ConfigDTO {
        private List<String> projects = new LinkedList<>();

        @XmlElement
        private String project;
        @XmlElement
        private String labels;
        @XmlElement
        private int blocker = 0;
        @XmlElement
        private int critical = 0;
        @XmlElement
        private int major = 0;
        @XmlElement
        private int red = 10;
        @XmlElement
        private int amber = 1;

        private static final int standardBlockerSLA = 3*24*3600;
        private static final int standardCriticalSLA = 5*24*3600;
        private static final int standardMajorSLA =  25*24*3600;

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
    }

    public static final class SLA{

    }
}
