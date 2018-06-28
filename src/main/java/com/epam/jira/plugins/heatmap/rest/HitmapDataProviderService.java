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
import javax.ws.rs.QueryParam;
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
import java.util.stream.Collectors;


@Path("gadget/heatmap")
@Scanned
public class HitmapDataProviderService {
    @ComponentImport
    private final SearchService searchService;
    @ComponentImport
    private final UserManager manager;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;

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
        Map<String, String> queryMap = collectPropertiesFromQueryString(request.getQueryString());
        configDto = new ConfigDTO();
        configDto.addProjects(queryMap.get("projects"));
        configDto.setLabels(queryMap.get("labels"));

        String blocker = queryMap.get("blocker");
        if (blocker != null && !blocker.isEmpty()) {
            configDto.setBlocker(Integer.parseInt(blocker));
        }
        String critical =  queryMap.get("critical");
        if (critical != null && !critical.isEmpty()) {
            configDto.setCritical(Integer.parseInt(critical));
        }
        String major = queryMap.get("major");
        if (major != null && !major.isEmpty()) {
            configDto.setMajor(Integer.parseInt(major));
        }

        String minor = queryMap.get("minor");
        if (minor != null && !minor.isEmpty()) {
            configDto.setMinor(Integer.parseInt(minor));
        }
        String red = queryMap.get("red");
        if (red != null && !red.isEmpty()) {
            configDto.setRed(Integer.parseInt(red));
        }
        String amber = queryMap.get("amber");
        if (amber != null && !amber.isEmpty()) {
            configDto.setAmber(Integer.parseInt(amber));
        }
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
        if(splittedString.length!=1) {
            resultMap.put(splittedString[0], splittedString[1]);
        } else {
            resultMap.put(splittedString[0], null);
        }
    }

    private List<ProjectDto> getPropertiesUser(HttpServletRequest request) {
        String username = manager.getRemoteUsername(request);
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(username);
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
        int blockerCount = dto.getBlocker();
        if (blockerCount > 0) {
            dto.incrementRateScore(10);
        }
        if (dto.getCritical() > 0) {
            dto.incrementRateScore(dto.getCritical());
        }
        if (dto.getMajor() > 0) {
            dto.incrementRateScore(dto.getMajor() / 20);
        }
        dto.incrementRateScore(dto.getCritical());
    }

    private void calculateRateScore(Issue issue, ProjectDto projectDto) {
        Timestamp now = Timestamp.from(Instant.now());
        incrementPriorityCounter(issue.getPriority().getName(), projectDto);
        int hoursBetweenDueDateAndCreated = getHoursForProirity(issue);
        if (hoursBetweenDueDateAndCreated == 0) {
            hoursBetweenDueDateAndCreated++;
        }
        LocalDateTime dueDate = issue.getCreated().toLocalDateTime().plusHours(hoursBetweenDueDateAndCreated);
        if (now.after(Timestamp.valueOf(dueDate))) {
            long milesecondsBetweenNowAndCreated = Timestamp.from(Instant.now()).getTime() - issue.getCreated().getTime();
            int hoursBetweenNowAndCreated = Math.toIntExact(milesecondsBetweenNowAndCreated / (60 * 60 * 1000));
            int countToIncrement = (hoursBetweenNowAndCreated - hoursBetweenDueDateAndCreated) / hoursBetweenDueDateAndCreated;
            projectDto.incrementRateScore(countToIncrement);
        }
    }

    private void incrementPriorityCounter(String issuePriority, ProjectDto dto) {
        if (issuePriority.equalsIgnoreCase("blocker")) {
            dto.increntBlocer();
        }
        if (issuePriority.equalsIgnoreCase("critical")) {
            dto.incrementCritical();
        }
        if (issuePriority.equalsIgnoreCase("major")) {
            dto.incrementMajor();
        }
        if (issuePriority.equalsIgnoreCase("minor")) {
            dto.incrementMinor();
        }
    }

    private int getHoursForProirity(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        int hours = 0;
        if (issuePriority.equalsIgnoreCase("blocker")) {
            hours = configDto.getBlocker();
        }
        if (issuePriority.equalsIgnoreCase("critical")) {
            hours = configDto.getCritical();
        }
        if (issuePriority.equalsIgnoreCase("major")) {
            hours = configDto.getMajor();
        }
        if (issuePriority.equalsIgnoreCase("minor")) {
            hours = configDto.getMinor();
        }
        if (hours==0){
            long milesecondsBetweenDueAndCreated = issue.getDueDate().getTime() - issue.getCreated().getTime();
            hours = Math.toIntExact(milesecondsBetweenDueAndCreated / (60 * 60 * 1000));
        }
        return hours;
    }

    private String collectLinkToProject(String project) {
        StringBuilder builder = new StringBuilder();
        return builder.append(jiraUrl).append("/issues/?jql=project%20%3D%20").append(project)
                .append("%20and%20priority%20in%20(Blocker%2C%20Critical%2C%20Major)%20and%20status%20not%20in%20(Closed%2C%20Resolved)%20and%20labels%20in%20(")
                .append(configDto.getLabels()).append(")").toString();
    }

    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = null;
        try {
            query = parser.parseQuery("project = '" + projectKey + "' AND priority IN (Blocker, Critical, Major) AND status not in (Closed, Resolved) and labels in (" + configDto.labels + ")");
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
        private int minor = 0;
        @XmlElement
        private int red = 1;
        @XmlElement
        private int amber = 1;

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
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

    }
}
