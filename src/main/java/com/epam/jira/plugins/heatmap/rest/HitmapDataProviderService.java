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
import com.atlassian.sal.api.user.UserManager;
import com.epam.jira.plugins.heatmap.calcusations.RiskScoreCalculator;
import com.epam.jira.plugins.heatmap.dto.ConfigPOJO;
import com.epam.jira.plugins.heatmap.dto.ProjectPOJO;
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

    private ConfigPOJO configPOJO;

    @Inject
    public HitmapDataProviderService(UserManager userManager, SearchService searchService) {
        this.manager = userManager;
        this.searchService = searchService;
    }

    @GET
    @Path("/main-chart")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        configPOJO = new ConfigPOJO(request);
        List<ProjectPOJO> result = getPropertiesUser(request);
        if (result == null || result.isEmpty()) {
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


    private List<ProjectPOJO> getPropertiesUser(HttpServletRequest request) {
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(manager.getRemoteUsername(request));
        List<ProjectPOJO> results = new ArrayList<>();
        RiskScoreCalculator calculator = new RiskScoreCalculator(configPOJO);
        for (String project : configPOJO.getProjects()) {
            List<Issue> issues = getListOfIsses(project, applicationUser);
            ProjectPOJO dto = calculator.calculateRiskScore(project, issues);
            setColour(dto, configPOJO);
            if (dto.getRisk_score() == 0) {
                dto.incrementRateScore();
            }
            results.add(dto);
        }
        setMinimumValueToRender(results);
        return results;
    }

    private void setMinimumValueToRender(List<ProjectPOJO> results) {
        int summ = results.stream().mapToInt(ProjectPOJO::getRisk_score).sum();
        int minValue = results.stream().mapToInt(ProjectPOJO::getSquareSize).min().getAsInt();
        if (((float) minValue / summ) <= ((float) 1 / configPOJO.getCellsNumber())) {
            recalculateSquresSizes(results);
        }

    }

    private void recalculateSquresSizes(List<ProjectPOJO> results) {
        int summ = results.stream().mapToInt(ProjectPOJO::getSquareSize).sum();
        int count = 0;
        for (ProjectPOJO projectPOJO : results) {
            int calculateValue = (projectPOJO.getSquareSize() * configPOJO.getCellsNumber()) / summ;
            if (calculateValue < 1) {
                calculateValue++;
                count++;
            }
            projectPOJO.setSquareSize(calculateValue);
        }
        if (count > 0 && results.size() > 1) {
            correctionsInCalculations(results, count);
        }
    }


    private void correctionsInCalculations(List<ProjectPOJO> results, int count) {
        int summ = results.stream().mapToInt(ProjectPOJO::getSquareSize).sum();
        float overallWeight = 0;
        for (ProjectPOJO projectPOJO : results) {
            int squareSize = projectPOJO.getSquareSize();
            if (squareSize != 1) {
                overallWeight += (float) squareSize / (summ - squareSize);
            }
        }
        for (ProjectPOJO projectPOJO : results.stream().sorted((o1, o2) -> Integer.compare(o1.getSquareSize(), o2.getSquareSize())).collect(Collectors.toList())) {
            int squareSize = projectPOJO.getSquareSize();
            if (squareSize != 1 && overallWeight > 0) {
                int valueToDecrement = Math.round((count * ((float) squareSize / (summ - squareSize))) / overallWeight);
                overallWeight -= valueToDecrement;
                count--;
                projectPOJO.setSquareSize(squareSize - valueToDecrement);
            }
        }
    }
    private List<Issue> getListOfIsses(String projectKey, ApplicationUser applicationUser) {
        JqlQueryParser parser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("project = '").append(projectKey).append("' AND priority IN (").append(configPOJO.getHighestPriorityName()).append(",")
                    .append(configPOJO.getHighPriorityName()).append(",").append(configPOJO.getMiddlePriorityName()).append(") AND status not in (Closed, Resolved)");
            if (configPOJO.getLabels() != null && configPOJO.getLabels().length() > 0 && !configPOJO.getLabels().equals("-")) {
                builder.append("and labels in (").append(configPOJO.getLabels()).append(")");
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

    private void setColour(ProjectPOJO dto, ConfigPOJO configPOJO) {
        if (dto.getRisk_score() > configPOJO.getRed()) {
            dto.setColor("#AF2947");
        } else if (dto.getRisk_score() > configPOJO.getAmber()) {
            dto.setColor("#F0B400");
        } else {
            dto.setColor("#7F9943");
        }
    }





}
