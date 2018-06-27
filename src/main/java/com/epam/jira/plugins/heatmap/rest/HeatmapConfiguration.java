package com.epam.jira.plugins.heatmap.rest;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Path("config")
@Scanned
public class HeatmapConfiguration {
    private static ConfigDTO config = new ConfigDTO();

    @ComponentImport
    private final UserManager manager;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;

    private PluginSettings settings;

    @Inject
    public HeatmapConfiguration(UserManager userManager, PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate) {
        this.manager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.transactionTemplate = transactionTemplate;
        settings = pluginSettingsFactory.createGlobalSettings();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest request) {
        String username = manager.getRemoteUsername(request);
        if (username == null || !manager.isSystemAdmin(username)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction() {
                try {
                    PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
                    String className = ConfigDTO.class.getName();
                    if(settings.get(className + ".projects")==null){
                        config.setBoards("test-project");
                    }else {
                        config.setBoards((String) settings.get(className + ".projects"));
                    }
                    if(settings.get(className + ".labels") == null) {
                        config.setLabels("test-label");
                    } else {
                        config.setLabels((String) settings.get(className + ".labels"));
                    }
                    //priorities
                    if(settings.get(className + ".blocker") == null){
                        config.setBlocker(1);
                    } else {
                        config.setBlocker(Integer.parseInt((String) settings.get(className + ".blocker")));
                    }
                    if(settings.get(className + ".critical")==null) {
                        config.setCritical(1);
                    } else {
                        config.setCritical(Integer.parseInt((String)  settings.get(className + ".critical")));
                    }
                    if(settings.get(className + ".major") == null) {
                        config.setMajor(1);
                    }else {
                        config.setMajor(Integer.parseInt((String)  settings.get(className + ".major")));
                    }
                    if(settings.get(className + ".major")==null){
                        config.setMinor(1);
                    } else {
                        config.setMinor(Integer.parseInt((String) settings.get(className + ".minor")));
                    }
                    //colours
                    if(settings.get(className + ".red")==null){
                       config.setRed(1);
                    }else {
                        config.setRed(Integer.parseInt((String)  settings.get(className + ".red")));
                    }
                    if(settings.get(className + ".amber")==null){
                        config.setAmber(1);
                    }else {
                        config.setAmber(Integer.parseInt((String) settings.get(className + ".amber")));
                    }
                    return config;
                } catch (Throwable throwable){
                    System.err.println(throwable.getMessage());
                    throw throwable;
                }
            }
        })).build();
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(final ConfigDTO config, @Context HttpServletRequest request) {
        String username = manager.getRemoteUsername(request);
        if (username == null || !manager.isSystemAdmin(username)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction() {
                PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
                pluginSettings.put(ConfigDTO.class.getName() + ".boardName", config.getBoards());
                pluginSettings.put(ConfigDTO.class.getName() + ".labelName", config.getLabels());
                pluginSettings.put(ConfigDTO.class.getName() + ".blocker", Integer.toString(config.getBlocker()));
                pluginSettings.put(ConfigDTO.class.getName() + ".critical", Integer.toString(config.getCritical()));
                pluginSettings.put(ConfigDTO.class.getName() + ".major", Integer.toString(config.getMajor()));
                pluginSettings.put(ConfigDTO.class.getName() + ".minor", Integer.toString(config.getMinor()));
                pluginSettings.put(ConfigDTO.class.getName() + ".red", Integer.toString(config.getRed()));
                pluginSettings.put(ConfigDTO.class.getName() + ".amber", Integer.toString(config.getAmber()));
                return null;
            }
        });
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(final ConfigDTO config, @Context HttpServletRequest request) {

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction() {
                PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
                pluginSettings.put(ConfigDTO.class.getName() + ".boardName", config.getBoards());
                pluginSettings.put(ConfigDTO.class.getName() + ".labelName", config.getLabels());
                pluginSettings.put(ConfigDTO.class.getName() + ".blocker", Integer.toString(config.getBlocker()));
                pluginSettings.put(ConfigDTO.class.getName() + ".critical", Integer.toString(config.getCritical()));
                pluginSettings.put(ConfigDTO.class.getName() + ".major", Integer.toString(config.getMajor()));
                pluginSettings.put(ConfigDTO.class.getName() + ".minor", Integer.toString(config.getMinor()));
                pluginSettings.put(ConfigDTO.class.getName() + ".red", Integer.toString(config.getRed()));
                pluginSettings.put(ConfigDTO.class.getName() + ".amber", Integer.toString(config.getAmber()));
                return null;
            }
        });
        return Response.noContent().build();
    }


    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class ConfigDTO {


//        @XmlElement
//        private List<String> listOfProjects;

        @XmlElement
        private String projects;
        @XmlElement
        private String labels;

        @XmlElement
        private int blocker = 1;
        @XmlElement
        private int critical = 1;
        @XmlElement
        private int  major = 1;
        @XmlElement
        private int minor = 1;

        @XmlElement
        private int red = 1;
        @XmlElement
        private int amber = 1;

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

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
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

        public String getBoards() {

            return projects;
        }

        public void setBoards(String projects) {
            this.projects = projects;
        }

    }
}
