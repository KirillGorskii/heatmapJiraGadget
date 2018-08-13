function getInfoForHeatmapView(drawFunction){
     var gadgets = AJS.$(this)[0].gadgets;
     AJS.$.ajax({
            type: 'GET',
            url: '/rest/heatmap-dj/1.0/gadget/heatmap/main-chart',
            data:{
                projects: gadgets.Prefs().getString("projects"),
                labels: gadgets.Prefs().getString("labels"),
                majorPriorityName: gadgets.Prefs().getString("majorPriorityName"),
                cellsNumber: gadgets.Prefs().getString("cellsNumber"),
                highPriorityName: gadgets.Prefs().getString("highPriorityName"),
                highestPriorityName: gadgets.Prefs().getString("highestPriorityName"),
                blocker: gadgets.Prefs().getString("blocker"),
                critical: gadgets.Prefs().getString("critical"),
                major: gadgets.Prefs().getString("major"),
                red: gadgets.Prefs().getString("red"),
                amber: gadgets.Prefs().getString("amber")
            },
            success: function(data){
                drawFunction(data);
            }
     });
}

function getDataForDetailView(drawFunction){
    AJS.$.ajax({
        type: 'GET',
        url: '/rest/heatmap-dj/1.0/gadget/heatmap/project-statistic',
        data:{
            projects: gadgets.Prefs().getString("projects"),
            labels: gadgets.Prefs().getString("labels"),
            majorPriorityName: gadgets.Prefs().getString("majorPriorityName"),
            cellsNumber: gadgets.Prefs().getString("cellsNumber"),
            highPriorityName: gadgets.Prefs().getString("highPriorityName"),
            highestPriorityName: gadgets.Prefs().getString("highestPriorityName"),
            blocker: gadgets.Prefs().getString("blocker"),
            critical: gadgets.Prefs().getString("critical"),
            major: gadgets.Prefs().getString("major"),
            red: gadgets.Prefs().getString("red"),
            amber: gadgets.Prefs().getString("amber"),
            projectName: gadgets.Prefs().getString("projectName"),
            startDate: gadgets.Prefs().getString("startDate")
        },
        success: function(data){
            drawFunction(data);
        }
    });
}