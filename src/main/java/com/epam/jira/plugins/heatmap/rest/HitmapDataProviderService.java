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
import com.epam.jira.plugins.heatmap.dto.ConfigDTO;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        configDto = new ConfigDTO(request);
        jiraUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        List<ProjectDto> result = getPropertiesUser(request);
        if (result == null || result.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(transactionTemplate.execute(() -> new Gson().toJson(result))).build();
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
                dto.incrementRateScore();
            }
            results.add(dto);
        }
        setMinimumValueToRender(results);
        return results;
    }

    private void setMinimumValueToRender(List<ProjectDto> results) {
        int summ = results.stream().mapToInt(ProjectDto::getSquareSize).sum();
        int minValue = results.stream().mapToInt(ProjectDto::getSquareSize).min().getAsInt();
        if (minValue <( (minValue * 200) / summ)) {
            recalculateSquresSizes(results);
        }

    }

    private void recalculateSquresSizes(List<ProjectDto> results) {
        int summ = results.stream().mapToInt(ProjectDto::getSquareSize).sum();
        int count = 0;
        for (ProjectDto projectDto : results) {
            int calculateValue = (projectDto.getSquareSize() * 200) / summ;
            if (calculateValue < 1) {
                calculateValue++;
                count++;
            }
            projectDto.setSquareSize(calculateValue);
        }
        if (count > 0 && results.size() > 1) {
            correctionsInCalculations(results, count);
        }
    }


    private void correctionsInCalculations(List<ProjectDto> results, int count) {
        int summ = results.stream().mapToInt(ProjectDto::getSquareSize).sum();
        float overallWeight = 0;
        for (ProjectDto projectDto : results) {
            int squareSize = projectDto.getSquareSize();
            if (squareSize != 1) {
                overallWeight += (float) squareSize / (summ - squareSize);
            }
        }
        for (ProjectDto projectDto : results.stream().sorted((o1, o2) -> Integer.compare(o1.getSquareSize(), o2.getSquareSize())).collect(Collectors.toList())) {
            int squareSize = projectDto.getSquareSize();
            if (squareSize != 1 && overallWeight > 0) {
                int valueToDecrement = Math.round((count * squareSize) / overallWeight);
                overallWeight -= overallWeight;
                count--;
                projectDto.setSquareSize(squareSize - valueToDecrement);
            }
        }
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
        dto.incrementRateScore(dto.getBlocker() * 10);
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
            projectDto.incrementRateScore();
        }
    }

    private void incrementPriorityCounter(String issuePriority, ProjectDto dto) {
        if (issuePriority.equalsIgnoreCase(configDto.getHighestPriorityName())) {
            dto.increntBlocker();
        }
        if (issuePriority.equalsIgnoreCase(configDto.getHighPriorityName())) {
            dto.incrementCritical();
        }
        if (issuePriority.equalsIgnoreCase(configDto.getMiddlePriorityName())) {
            dto.incrementMajor();
        }
    }

    private int getHoursForProirity(Issue issue) {
        String issuePriority = issue.getPriority().getName();
        int hours = 0;
        hours = configDto.getSlaTimeForPriority(issuePriority);

        if (hours == 0) {
            Timestamp due = issue.getDueDate();
            if (due != null) {
                long milesecondsBetweenDueAndCreated = due.getTime() - issue.getCreated().getTime();
                hours = Math.toIntExact(milesecondsBetweenDueAndCreated / (60 * 60 * 1000));
            } else {
                return configDto.getStandardSlaTimeForPriority(issuePriority);
            }
        }
        return hours;
    }

    private String collectLinkToProject(String project) {
        StringBuilder builder = new StringBuilder();
        builder.append(jiraUrl).append("/issues/?jql=project%20%3D%20").append(project)
                .append("%20and%20priority%20in%20(")
                .append(configDto.getHighestPriorityName()).append("%2C%20").append(configDto.getHighPriorityName())
                .append("%2C%20").append(configDto.getMiddlePriorityName()).append(")%20and%20status%20not%20in%20(Closed%2C%20Resolved)");
        if (configDto.getLabels() != null && configDto.getLabels().length() > 0 && !configDto.getLabels().equals("-")) {
            builder.append("%20and%20labels%20in%20(").append(configDto.getLabels()).append(")");
        }
        return builder.toString();
    }

    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("project = '").append(projectKey).append("' AND priority IN (").append(configDto.getHighestPriorityName()).append(",")
                    .append(configDto.getHighPriorityName()).append(",").append(configDto.getMiddlePriorityName()).append(") AND status not in (Closed, Resolved)");
            if (configDto.getLabels() != null && configDto.getLabels().length() > 0 && !configDto.getLabels().equals("-")) {
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

}
