package com.epam.jira.plugins.heatmap.rest;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
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
import com.atlassian.sal.api.user.UserManager;
import com.epam.jira.plugins.heatmap.calcusations.RiskScoreCalculator;
import com.epam.jira.plugins.heatmap.dto.ConfigPOJO;
import com.epam.jira.plugins.heatmap.dto.ProjectInfo;
import com.epam.jira.plugins.heatmap.dto.ProjectStatisticInRange;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Path("gadget/heatmap")
@Scanned
public class HitmapDataProviderService {
    @ComponentImport
    private final SearchService searchService;
    @ComponentImport
    private final UserManager manager;

    @Inject
    public HitmapDataProviderService(UserManager userManager, SearchService searchService) {
        this.manager = userManager;
        this.searchService = searchService;
    }

    @GET
    @Path("/main-chart")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        ConfigPOJO.setConfigPOJO(request);
        List<ProjectInfo> result = getPropertiesUser(request);
        if (result.isEmpty()) {
            return Response.noContent().build();
        } else {
            String jsonString = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonString = mapper.writeValueAsString(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Response.ok(jsonString).build();
        }
    }

    @GET
    @Path("/project-statistic")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectStatistic(@Context HttpServletRequest request) {
        ConfigPOJO.setConfigPOJO(request);
        ProjectStatisticInRange result = getProjectInfo(request);
        String jsonString = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            jsonString = mapper.writeValueAsString(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.ok(jsonString).build();

    }

    private ProjectStatisticInRange getProjectInfo(HttpServletRequest request) {
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(manager.getRemoteUsername(request));
        String projectName = request.getParameter("projectName");
        ProjectStatisticInRange projectStatistic = new ProjectStatisticInRange(projectName);
        LocalDate startCalculationDate = LocalDate.parse(request.getParameter("startDate").replace("-", "/"), DateTimeFormatter.ofPattern("yyyy/M/dd"));
        int difsInDays = (int) Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), startCalculationDate));
        for (int i = difsInDays; i >= 0; i--) {
            List<Issue> issues = getListOfIsses(projectName, applicationUser, LocalDate.now().minusDays(i));
            LocalDate calculationDate = LocalDate.now().minusDays(i);
            projectStatistic.calculateRiskScore(issues, calculationDate);
        }

        return projectStatistic;
    }


    private List<ProjectInfo> getPropertiesUser(HttpServletRequest request) {
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(manager.getRemoteUsername(request));
        List<ProjectInfo> results = new ArrayList<>();
        RiskScoreCalculator calculator = new RiskScoreCalculator();
        for (String project : ConfigPOJO.getProjects()) {
            List<Issue> issues = getListOfIsses(project, applicationUser);
            ProjectInfo dto = calculator.calculateRiskScore(project, issues);
            setColour(dto);
            if (dto.getRiskScore() == 0) {
                dto.incrementRiskScore();
            }
            results.add(dto);
        }
        setMinimumValueToRender(results);
        return results;
    }

    private void setMinimumValueToRender(List<ProjectInfo> results) {
        int summ = results.stream().mapToInt(ProjectInfo::getRiskScore).sum();
        int minValue = results.stream().mapToInt(ProjectInfo::getSquareSize).min().getAsInt();
        if (((float) minValue / summ) <= ((float) 1 / ConfigPOJO.getCellsNumber())) {
            recalculateSquresSizes(results);
        }

    }

    private void recalculateSquresSizes(List<ProjectInfo> results) {
        int summ = results.stream().mapToInt(ProjectInfo::getSquareSize).sum();
        int count = 0;
        for (ProjectInfo projectInfo : results) {
            int calculateValue = (projectInfo.getSquareSize() * ConfigPOJO.getCellsNumber()) / summ;
            if (calculateValue < 1) {
                calculateValue++;
                count++;
            }
            projectInfo.setSquareSize(calculateValue);
        }
        if (count > 0 && results.size() > 1) {
            correctionsInCalculations(results, count);
        }
    }


    private void correctionsInCalculations(List<ProjectInfo> results, int count) {
        int summ = results.stream().mapToInt(ProjectInfo::getSquareSize).sum();
        float overallWeight = 0;
        for (ProjectInfo projectInfo : results) {
            int squareSize = projectInfo.getSquareSize();
            if (squareSize != 1) {
                overallWeight += (float) squareSize / (summ - squareSize);
            }
        }
        for (ProjectInfo projectInfo : results.stream().sorted(Comparator.comparing(ProjectInfo::getSquareSize)).collect(Collectors.toList())) {
            int squareSize = projectInfo.getSquareSize();
            if (squareSize != 1 && overallWeight > 0) {
                int valueToDecrement = Math.round((count * ((float) squareSize / (summ - squareSize))) / overallWeight);
                overallWeight -= valueToDecrement;
                count--;
                projectInfo.setSquareSize(squareSize - valueToDecrement);
            }
        }
    }

    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser, LocalDate date) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("project = '").append(projectKey).append("' AND priority IN (").append(ConfigPOJO.getHighestPriorityName()).append(",")
                    .append(ConfigPOJO.getHighPriorityName()).append(",").append(ConfigPOJO.getMiddlePriorityName()).append(")");
            if (date != null) {
                String searchDate = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                builder.append(" AND status was not in (Closed, Resolved) DURING ('").append(searchDate).append("', '").append(searchDate).append("')");
            } else {
                builder.append(" AND status not in (Closed, Resolved) ");
            }
            if (ConfigPOJO.getLabels() != null && ConfigPOJO.getLabels().length() > 0 && !ConfigPOJO.getLabels().equals("-")) {
                builder.append(" AND labels in (").append(ConfigPOJO.getLabels()).append(")");
            }

            query = parser.parseQuery(builder.toString());
        } catch (JqlParseException e) {
            e.printStackTrace();
        }

        List<Issue> issues;
        try {
            SearchResults results = searchService.search(applicationUser, query, PagerFilter.getUnlimitedFilter());
            issues = results.getIssues();
        } catch (SearchException e) {
            e.printStackTrace();
            issues = new ArrayList<>();
        }
        return issues;
    }

    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser) {
        return getListOfIsses(projectKey, applicationUser, null);
    }

    private void setColour(ProjectInfo dto) {
        if (dto.getRiskScore() > ConfigPOJO.getRed()) {
            dto.setColor(ConfigPOJO.RED_COLOR);
        } else if (dto.getRiskScore() > ConfigPOJO.getAmber()) {
            dto.setColor(ConfigPOJO.AMBER_COLOR);
        } else {
            dto.setColor(ConfigPOJO.GREEN_COLOR);
        }
    }


}
