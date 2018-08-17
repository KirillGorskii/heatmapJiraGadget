function getInfoForHeatmapView(drawFunction){
     var gadgets = AJS.$(this)[0].gadgets;
     var result = [];

     gadgets.Prefs().getString("projects").split(',').forEach(function(project){
         var queryObject = getStandardQueryObject();
         queryObject.projectName = project;
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

function calculateRiskScore(dataFromJira, queryObject){
    var issues = dataFromJira.issues;
    var calculatedRiskScore = {
        name: queryObject.projectName,
        color: null,
        value: 1,
        riskScore: 0,
        critical: 0,
        blocker: 0,
        major:0,
        dateOfRiskScore: queryObject.searchDate,
        issues: []
    }

    issues.forEach(function(issue){
        if(isOverdue(issue, queryObject)){
            calculateRateScoreForOneIssue(issue, queryObject, calculatedRiskScore);
        }
    });

    calculatedRiskScoreForOverallData(calculatedRiskScore);
    if(calculatedRiskScore.riskScore==0){
        calculatedRiskScore.riskScore=1;
    }
    setColour(calculatedRiskScore);
    return calculatedRiskScore;
}


function getDataForDetailView(drawFunction){
    var queryObjects = []
    var gadgets = AJS.$(this)[0].gadgets;
    var result = [];
    var startSearchingDate = new Date(gadgets.Prefs().getString("startDate"));
    var projectName = gadgets.Prefs().getString("projectName");
    var now = new Date();
    now.setHours(0,0,0,0);
    var days =  Math.ceil((now.getTime() - startSearchingDate.getTime())/(3600*24*1000));
    for(var i=0; i<days; i++){
        var queryObject = getStandardQueryObject();
        queryObject.projectName = projectName;
        var newDate = new Date();
        queryObject.searchDate = newDate.setDate(newDate.getDate() - i);

        if(i==0){
            queryObject.returnIssueTable=true;
        }
        queryObjects.push(queryObject);
    }

    queryObjects.forEach(function(queryObject){
        AJS.$.ajax({
                type: 'GET',
                url: collectUrlForRestQuery(queryObject),
                contentType: 'application/json',
                success: function(data){
                    result.push(calculateRiskScore(data, queryObject));
                    result.sort(function(a, b){
                    var dateOfRiskScoreA = new Date(a.dateOfRiskScore).getTime();
                    var dateOfRiskScoreB = new Date(b.dateOfRiskScore).getTime();
                        if(dateOfRiskScoreA>dateOfRiskScoreB){
                            return 1;
                        }
                        if(dateOfRiskScoreA<dateOfRiskScoreB){
                            return -1;
                        }
                        return 0;
                    });
                    var dataToDraw = {
                        projectName: projectName,
                        projectInfoByDates: result
                    }
                    drawFunction(dataToDraw);

                }
            });
    });
}


function calculateRateScoreForOneIssue(issue, queryObject, calculatedRiskScore){
    var priorityName = issue.fields.priority.name;
    var daysOverdue = getOverdueDateForPriorityInDays(issue, queryObject);
    if(priorityName == gadgets.Prefs().getString("highestPriorityName")){
        calculatedRiskScore.riskScore+=daysOverdue;
        calculatedRiskScore.blocker++;
        if(queryObject.returnIssueTable){
            calculatedRiskScore.issues.push(collectIssueInfo(issue, 10 + daysOverdue, daysOverdue));
        }
    } else if (priorityName == gadgets.Prefs().getString("highPriorityName")){
        var calcRiskScoreForPriority = Math.round((0.5 * daysOverdue))
        calculatedRiskScore.riskScore+=calcRiskScoreForPriority;
        calculatedRiskScore.critical++;
         if(queryObject.returnIssueTable){
            calculatedRiskScore.issues.push(collectIssueInfo(issue, 1 + calcRiskScoreForPriority, daysOverdue));
        }
    } else if (priorityName == gadgets.Prefs().getString("majorPriorityName")){
        var calcRiskScoreForPriority = Math.round((0.1 * daysOverdue))
        calculatedRiskScore.riskScore+=calcRiskScoreForPriority;
        calculatedRiskScore.major++;
        if(queryObject.returnIssueTable){
            calculatedRiskScore.issues.push(collectIssueInfo(issue, calcRiskScoreForPriority, daysOverdue));
        }
    }
}


function collectIssueInfo(issueFromJira, calculatedRiskScore, days){
    var calcColor;
    if(calculatedRiskScore>=5){
        calcColor = 'red';
    } else if(calculatedRiskScore>=2){
        calcColor = 'amber';
    } else {
        calcColor = 'green';
    }
    var assignee = issueFromJira.fields.assignee;
    if(assignee==null){
        assignee= 'Unassigned'
    } else{
        assignee = issueFromJira.fields.assignee.name;
    }
    return {
        issuePriority: issueFromJira.fields.priority.name,
        calculatedRateScore: calculatedRiskScore,
        issueKey: issueFromJira.key,
        color: calcColor,
        issueExpiration: days,
        assignee: assignee,
        summary: issueFromJira.fields.summary
    }
}


function calculatedRiskScoreForOverallData(calculatedRiskScore){
    calculatedRiskScore.riskScore+=Math.round(calculatedRiskScore.blocker*10);
    calculatedRiskScore.riskScore+=Math.round(calculatedRiskScore.critical);
    calculatedRiskScore.riskScore+=Math.round(calculatedRiskScore.major*0.05);
    calculatedRiskScore.value=calculatedRiskScore.riskScore;
}

function formatDate(dateToFormat){
    dateToFormat = new Date(dateToFormat);
    var days = dateToFormat.getDate();
    var month = dateToFormat.getMonth();
    month++;
    if(days<10){
        days='0'+days;
    }
    if(month<10){
        month='0'+month;
    }
    return dateToFormat.getFullYear() + '-' + month + '-' +days;
}

function collectUrlForRestQuery(queryObject){
    var urlString = '/rest/api/2/search?jql=project=' + queryObject.projectName +
    '%20AND%20priority%20IN%20('+queryObject.highestPriorityName +','+queryObject.highPriorityName +',' + queryObject.majorPriorityName + ')';
    if(queryObject.labels!=null){
        urlString+= '%20AND%20labels%20in%20(' + queryObject.labels + ')';
    }
    if(queryObject.searchDate!=null){
        var searchDate = formatDate(queryObject.searchDate);
        urlString+='%20AND%20status%20was%20not%20in%20(CLOSED,%20RESOLVED)%20DURING%20("'+ searchDate + '",%20"' + searchDate + '")';
    } else {
        urlString+='%20AND%20status%20not%20in%20(CLOSED,%20RESOLVED)';
    }
    urlString+= '&fields=id,key,status,summary,assignee,issuetype,priority,duedate,created&maxResults=500'
    return urlString;
}

function isOverdue(issue, queryObject){
    var searchDate = queryObject.searchDate;
    if(searchDate==null){
        searchDate= new Date();
        searchDate.setHours(0,0,0,0);
    }
    if(issue.fields.duedate == null){
        var created = new Date(issue.fields.created);
        var duedate = created.setDate(created.getDate() + getOverdueSLAForPriority(issue.fields.priority.name));
        return new Date(searchDate) > duedate;
    } else {
        return new Date(searchDate) > new Date(issue.fields.duedate);
    }
}

function getStandardQueryObject(){
    return {
        projectName: null,
        highestPriorityName: gadgets.Prefs().getString("highestPriorityName"),
        highPriorityName: gadgets.Prefs().getString("highPriorityName"),
        majorPriorityName: gadgets.Prefs().getString("majorPriorityName"),
        labels: gadgets.Prefs().getString("labels"),
        returnIssueTable:false,
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
    var searchDate = queryObject.searchDate;
    if(searchDate==null){
        searchDate= new Date();
        searchDate.setHours(0,0,0,0);
    }
    if(issue.fields.duedate == null){
        var created =  new Date(issue.fields.created);
        created.setHours(0,0,0,0);
        duedate = new Date(created.setDate(created.getDate() + getOverdueSLAForPriority(issue.fields.priority.name)));
    } else {
        duedate = new Date(issue.fields.duedate);
        duedate.setHours(0,0,0,0);
    }
    var difference = Math.abs(new Date(searchDate).getTime() - duedate.getTime());
    return Math.ceil(difference/(1000*24*3600));
}

function getOverdueSLAForPriority(priorityName){
    if(priorityName.toLowerCase() == gadgets.Prefs().getString("highestPriorityName").toLowerCase()){
        return gadgets.Prefs().getString("blocker")/24;
    }
    if(priorityName.toLowerCase()==gadgets.Prefs().getString("highPriorityName").toLowerCase()){
        return gadgets.Prefs().getString("critical")/24;
    }
    if(priorityName.toLowerCase()==gadgets.Prefs().getString("majorPriorityName").toLowerCase()){
        return gadgets.Prefs().getString("major")/24;
    }
    return 2;
}