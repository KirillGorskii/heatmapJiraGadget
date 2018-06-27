package com.epam.jira.plugins.heatmap.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.preferences.ExtendedPreferences;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.sal.api.transaction.TransactionCallback;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


@Path("gadget/heatmap")
@Scanned
public class HitmapDataProviderService {
    private List<String> projects;
    @ComponentImport
    private final SearchService searchService;
    @ComponentImport
    private final UserManager manager;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;
    @ComponentImport
    private final CustomFieldManager customFieldManager;

    private String jiraUrl;

    @Inject
    public HitmapDataProviderService(CustomFieldManager customFieldManager, UserManager userManager, TransactionTemplate transactionTemplate, SearchService searchService) {
        this.manager = userManager;
        this.transactionTemplate = transactionTemplate;
        this.searchService = searchService;
        this.customFieldManager = customFieldManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@QueryParam("projects") String projects, @QueryParam("labels") String labels, @QueryParam("blocker") String blocker, @QueryParam("critical") String critical,
                        @QueryParam("major") String major, @QueryParam("minor") String minor, @QueryParam("red") String red, @QueryParam("amber") String amber, @Context HttpServletRequest request) {
        ConfigDTO configDto = new ConfigDTO();
        configDto.addProjects(projects);
        configDto.setLabels(labels);
        configDto.setBlocker(Integer.parseInt(blocker));
        configDto.setCritical(Integer.parseInt(critical));
        configDto.setMajor(Integer.parseInt(major));
        configDto.setMinor(Integer.parseInt(minor));
        configDto.setRed(Integer.parseInt(red));
        configDto.setAmber(Integer.parseInt(amber));
        jiraUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        List<ProjectDto> result = getPropertiesUser(request, configDto);
        if (result == null || result.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction() {
                    return new Gson().toJson(result);
                }
            })).build();
        }
    }

    private List<ProjectDto> getPropertiesUser(HttpServletRequest request, ConfigDTO configDTO) {
        String username = manager.getRemoteUsername(request);
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(username);
        List<ProjectDto> results = new ArrayList<>();
        for (String project : configDTO.getProjects()) {
            ProjectDto dto = new ProjectDto(project);
            dto.setLink(collectLinkToProject(project));
            List<Issue> issues = getListOfIsses(project, configDTO, applicationUser);
            for (Issue issue : issues) {
                calculateRateScore(configDTO, issue, dto);
            }
            calculateRateScoreBaseOnOverallData(dto);
            setColour(dto, configDTO);
            if(dto.getRisk_score()==0){
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

    private void calculateRateScore(ConfigDTO dto, Issue issue, ProjectDto projectDto) {
        Timestamp now = Timestamp.from(Instant.now());
        String issuePriority = issue.getPriority().getName();
        incrementPriorityCounter(issuePriority, projectDto);
        int hoursBetweenDueDateAndCreated = getHoursForProirity(issuePriority, dto);
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

    private int getHoursForProirity(String issuePriority, ConfigDTO dto) {
        if (issuePriority.equalsIgnoreCase("blocker")) {
            return dto.getBlocker();
        }
        if (issuePriority.equalsIgnoreCase("critical")) {
            return dto.getCritical();
        }
        if (issuePriority.equalsIgnoreCase("major")) {
            return dto.getMajor();
        }
        if (issuePriority.equalsIgnoreCase("minor")){
            return dto.getMinor();
        }
        return 0;
    }

    private String collectLinkToProject(String project) {
        StringBuilder builder = new StringBuilder();
        return builder.append(jiraUrl).append("/issues/?jql=project%20%3D%20").append(project)
                .append("%20and%20priority%20in%20(Blocker%2C%20Critical%2C%20Major)%20and%20status%20in%20(Open%2C%20Reopened)").toString();
    }

    private List<Issue> getListOfIsses(String projectKey, ConfigDTO configDTO, ApplicationUser applicationUser) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);

        Query query = null;
        try {
            query = parser.parseQuery("project = '" + projectKey + "' AND priority IN (Blocker, Critical, Major) AND status in (Open, Reopened) and labels in ("+ configDTO.labels+")");
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

    private ConfigDTO setPrefInConfiguration(ExtendedPreferences preferences) {
        ConfigDTO configDto = new ConfigDTO();
        configDto.addProjects(preferences.getText("projects"));
        configDto.setLabels(preferences.getText("labels"));
        configDto.setBlocker(Integer.parseInt(preferences.getText("blocker")));
        configDto.setCritical(Integer.parseInt(preferences.getText("critical")));
        configDto.setMajor(Integer.parseInt(preferences.getText("major")));
        configDto.setRed(Integer.parseInt(preferences.getText("red")));
        configDto.setAmber(Integer.parseInt(preferences.getText("amber")));
        return configDto;
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

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
        }

        @XmlElement

        private int minor = 0;
        @XmlElement
        private int red = 0;
        @XmlElement
        private int amber = 0;

        public List<String> getProjects() {
            return projects;
        }

        public void addProjects(String project) {
            if (project.contains(",")) {
                projects.addAll(Arrays.asList(project.split(",")));
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
