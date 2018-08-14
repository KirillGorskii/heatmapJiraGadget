  function getInfoForHeatmapView(drawFunction){
     var gadgets = AJS.$(this)[0].gadgets;
     var result = [];

     gadgets.Prefs().getString("projects").split(',').forEach(function(project){
         var queryObject = getStandardQueryObject();
         queryObject.projectName = project;
         queryObject.searchDate = new Date();
         AJS.$.ajax({
            type: 'GET',
            contentType: 'application/json',
            url: collectUrlForRestQuery(queryObject),
            success: function(data){
                result.push(calculateRiskScore(data, queryObject));
                drawFunction(result);
            }
         });
     })
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

function calculateRiskScore(dataFromJira, queryObject){
    var issues = dataFromJira.issues;
    var calculatedRiskScore = {
        name: queryObject.projectName,
        color: null,
        value: 1,
        riskScore: 1,
        critical: 0,
        blocker: 0,
        major:0
    }

    issues.forEach(function(issue){
        if(isOverdue(issue, queryObject)){
            calculateRateScoreForOneIssue(issue, queryObject, calculatedRiskScore);
        }
    });
    calculatedRiskScoreForOverallData(calculatedRiskScore);
    setColour(calculatedRiskScore)
    return calculatedRiskScore;
}



function calculateRateScoreForOneIssue(issue, queryObject, calculatedRiskScore){
    var priorityName = issue.fields.priority.name;
    if(priorityName == gadgets.Prefs().getString("highestPriorityName")){
        calculatedRiskScore.riskScore+= 10 + getOverdueDateForPriorityInDays(issue, queryObject);
        calculatedRiskScore.blocker++;
    } else if (priorityName == gadgets.Prefs().getString("highPriorityName")){
        calculatedRiskScore.riskScore+= 1 + (0.5 * getOverdueDateForPriorityInDays(issue, queryObject));
        calculatedRiskScore.critical++;
    } else if (priorityName == gadgets.Prefs().getString("majorPriorityName")){
        calculatedRiskScore.riskScore+=(0.1 * getOverdueDateForPriorityInDays(issue, queryObject));
        calculatedRiskScore.major++;
    }
}

function calculatedRiskScoreForOverallData(calculatedRiskScore){
   calculatedRiskScore.riskScore+=calculatedRiskScore.blocker*10;
   calculatedRiskScore.riskScore+=calculatedRiskScore.critical;
   calculatedRiskScore.riskScore+=calculatedRiskScore.major*0.05;
   calculatedRiskScore.riskScore= Math.ceil(calculatedRiskScore.riskScore);
   calculatedRiskScore.value=calculatedRiskScore.riskScore;
}

function collectUrlForRestQuery(queryObject){
    var urlString = '/rest/api/2/search?jql=project=' + queryObject.projectName +
    '&priority%20IN%20('+queryObject.highestPriorityName +','+queryObject.highPriorityName +',' + queryObject.majorPriorityName + ')' +
    '&issueTypeNames=%27Vulnerability%27';
    if(queryObject.labels!=null){
        urlString+= '&%20labels%20in%20(' + queryObject.labels + ')';
    }
    if(queryObject.date!=null){
        urlString+='&%20status%20was%20not%20in%20(Closed,%20Resolved)%20DURING ('+ queryObject.searchDate + "',%20'" + queryObject.searchDate + ')';
    } else {
        urlString+='&status%20not%20in%20(Closed,%20Resolved)';
    }
    urlString+= '&fields=id,key,status,summary,assignee,issuetype,priority,duedate,created&maxResults=500'
    return urlString;
}

function isOverdue(issue, queryObject){
    if(issue.fields.duedate == null){
        var created = new Date(issue.fields.created);
        var duedate = created.setDate(created.getDate() + getOverdueSLAForPriority(issue.fields.priority.name));
        return new Date(queryObject.searchDate) > duedate;
    } else {
        return new Date(queryObject.searchDate) > new Date(issue.fields.duedate);
    }
}

function getStandardQueryObject(){
    return {
        projectName: null,
        highestPriorityName: gadgets.Prefs().getString("highestPriorityName"),
        highPriorityName: gadgets.Prefs().getString("highPriorityName"),
        majorPriorityName: gadgets.Prefs().getString("majorPriorityName"),
        labels: gadgets.Prefs().getString("labels"),
        searchDate:null
    };
}

function setColour(calculatedRiskScore){
    if(calculatedRiskScore.riskScore < gadgets.Prefs().getString("amber")){
        calculatedRiskScore.color= '#7F9943';
    } else if(calculatedRiskScore.riskScore < gadgets.Prefs().getString("red")){
     calculatedRiskScore.color= '#F0B400';
    } else {
        calculatedRiskScore.color= '#AF2947';
    }
}

function getOverdueDateForPriorityInDays(issue, queryObject){
    var duedate;
    if(issue.fields.duedate == null){
        var created =  new Date(issue.fields.created);
        duedate = new Date(created.setDate(created.getDate() + getOverdueSLAForPriority(issue.fields.priority.name)));
    } else {
        duedate = new Date(issue.fields.duedate);
    }
    var difference = Math.abs(queryObject.searchDate.getTime() - duedate.getTime());
    return Math.ceil(difference/(1000*24*3600));
}

function getOverdueSLAForPriority(priorityName){
    if(priorityName == gadgets.Prefs().getString("highestPriorityName")){
        return gadgets.Prefs().getString("blocker")/24;
    }
    if(priorityName==gadgets.Prefs().getString("highPriorityName")){
        return gadgets.Prefs().getString("critical")/24;
    }
    if(priorityName==gadgets.Prefs().getString("majorPriorityName")){
        return gadgets.Prefs().getString("major")/24;
    }
    return 2;
}