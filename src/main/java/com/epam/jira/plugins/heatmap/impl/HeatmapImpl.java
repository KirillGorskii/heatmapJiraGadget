package com.epam.jira.plugins.heatmap.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.epam.jira.plugins.heatmap.api.Heatmap;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({Heatmap.class})
@Named("Heatmap")
public class HeatmapImpl implements Heatmap {
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @Inject
    public HeatmapImpl(final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getName() {
        if (null != applicationProperties) {
            return "Heatmap:" + applicationProperties.getDisplayName();
        }

        return "Heatmap";
    }
}