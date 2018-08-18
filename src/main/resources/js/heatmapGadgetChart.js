var issuesDescription;
var redColor = '#d04437';
var standardColor = '#ccc';
var amberInputSelector = 'input#amber.numeric';
var projectsSelector = 'input#projects';
var redInputSelector = 'input#red.numeric';
var cellsNumberSelector = 'input#cellsNumber.numeric';

function calculateFontSize(){
    var minHeight = findMinSquareSizeForDimention('height');
    var minWidth = findMinSquareSizeForDimention('width');
    var minFontSizeForHeight = calculateMinFontSize(minHeight)*1.2;
    var minFontSizeForWidth = calculateMinFontSize(minWidth);
    return Math.min(minFontSizeForHeight, minFontSizeForWidth);
}

function calculateMinFontSize(minSize){
    var standardFontSize = 16;
    if(minSize < 20){
        return standardFontSize-6;
    } else if(minSize < 40){
        return standardFontSize-5;
    } else if(minSize < 60){
        return standardFontSize-4;
    } else if(minSize < 80){
        return standardFontSize-3;
    } else if(minSize < 100){
        return standardFontSize-2;
    } else if(minSize < 120){
        return standardFontSize-1;
    } else{
        return standardFontSize;
    }
}

function findMinSquareSizeForDimention(attrName){
    var array = [];
    AJS.$('rect.highcharts-point').each(function(){
        array.push(AJS.$(this).attr(attrName));
    });
    return Math.min.apply(Math, array);
}

AJS.$(document).on("input", ".numeric", function() {
    this.value = this.value.replace(/\D/g,'');
});


AJS.$(document).on("keydown", cellsNumberSelector, function(data){
    var errorMessageSelector = '#cellsNumber-error';
    var cellsNumberInput = AJS.$(cellsNumberSelector);
    cellsNumberInput.css('border-color', standardColor)
    hideErrorMessage(errorMessageSelector);
    if(isCorrectSymbol(data)){
        var correctionValue = AJS.$(projectsSelector).val().split(',').length;
        var newValue = getNewValue(data);
        if(parseInt(newValue) < parseInt(correctionValue)){
            showErrorMessage(errorMessageSelector, 'Incorrect value. "Cells number" field value should be greater than number of selected projects in "Projects" field');
            cellsNumberInput.css('border-color', redColor);
            return false;
        }
    } else if(data.keyCode > 39 && /\w/g.test(data.key)){
        showErrorMessage(errorMessageSelector, 'Incorrect value. Only numeric values are allowed');
        cellsNumberInput.css('border-color', redColor);
        return false;
    }
    return true;
});

AJS.$(document).on("keydown", amberInputSelector, function(data) {
    var errorMessageSelector = '#amber-error';
    var amberInput = AJS.$(amberInputSelector);
    amberInput.css('borderColor', standardColor)
    hideErrorMessage(errorMessageSelector);

    if(isCorrectSymbol(data)){
        var redValue = AJS.$(redInputSelector).val();
        var newValue = getNewValue(data);
        if(parseInt(newValue) >  parseInt(redValue)){
            showErrorMessage(errorMessageSelector, 'Incorrect value. "Amber" score value should be less than "Red" score value');
            amberInput.css('border-color', redColor);
            return false;
        }
    } else if(data.keyCode > 39 && /\w/g.test(data.key)){
        showErrorMessage(errorMessageSelector, 'Incorrect value.');
        amberInput.css('border-color', redColor);
        return false;
    }
    return true;
});

AJS.$(document).on("change", "#fromDate", function(data){
    var gadgets = AJS.$(window)[0].gadgets.Prefs();
    gadgets.set('startDate', data.target.value);
    redrawToDrilldown();
});

AJS.$(document).on("change", '#fromDate', function(data){
    var newDate = new Date(data.target.value);
    if(newDate!=null){
        AJS.$(window)[0].gadgets.Prefs().set('startDate', formatDate(newDate));

    }
});

function redrawTable(){
    var table = AJS.$('#issuesDescriptionTable')
    AJS.$('#issuesDescriptionTable').show();
    if(table){
        table.empty();
    }
    table.append("<tr class='headerRow'><td id='calculatedRateScore'><strong>Rate score:</strong></td>"
    + "<td id='issueKey'><strong>Issue name</strong></td><td id='issuePriority'><strong>Issue priority</strong></td>"
    + "<td id='issueExpiration'><strong>Past Due Date</strong></td><td id='assignee'><strong>Assignee</strong></td>"
    + "<td id='issueSummary'><strong>Summary<strong></td></tr>");
    issuesDescription.sort(function(a, b){
        if(a.calculatedRateScore>b.calculatedRateScore){
            return -1;
        }
        if(a.calculatedRateScore<b.calculatedRateScore){
            return 1;
        }
        return 0;
    });
    issuesDescription.forEach(function(issue){
        var linkToIssue = AJS.gadget.getBaseUrl() + "/browse/" + issue.issueKey;
        table.append("<tr class='bodyRow'><td id='calculatedRateScore'><span class='" + issue.color + "'>" + issue.calculatedRateScore + "</span></td>"
        + "<td id='issueKey'><a href='"+linkToIssue+"' target='_blank'>" + issue.issueKey + "</a></td><td id='issuePriority'>" + issue.issuePriority + "</td>"
        + "<td id='issueExpiration'>" + issue.issueExpiration + " days</td><td id='assignee'>" + issue.assignee + "</td>"
        + "<td id='issueSummary'>" + issue.summary + "</td></tr>");

    });
    AJS.$(window)[0].gadgets.window.adjustHeight();
}


AJS.$(document).on("keydown", redInputSelector, function(data) {
    var errorMessageSelector = '#red-error';
    var redInput = AJS.$(redInputSelector);
    redInput.css('borderColor', standardColor)
    hideErrorMessage(errorMessageSelector);
    if(isCorrectSymbol(data)){
        var amberValue = AJS.$(amberInputSelector).val();
        var newValue = getNewValue(data);
        if(parseInt(newValue) <  parseInt(amberValue)){
            showErrorMessage(errorMessageSelector, 'Incorrect value. "Red" score value should be greater than "Amber" score value');
            redInput.css('border-color', redColor);
            return false;
        }
    } else if(data.keyCode > 39 && /\w/g.test(data.key)){
        showErrorMessage(errorMessageSelector, 'Incorrect value.');
        redInput.css('border-color', redColor);
        return false;
    }
    return true;
});

AJS.$(document).on("keydown", 'input#projects', function(data) {
    var errorMessageSelector = '#projects-error';
    var redInput = AJS.$('input#projects');
    redInput.css('borderColor', standardColor)
    hideErrorMessage(errorMessageSelector);
    if( data.keyCode < 40 || /\w/g.test(data.key) || data.key===','){
        return true;
    }
    showErrorMessage(errorMessageSelector, 'Incorrect value. Possible values are: "A-z", "1-9" and ","]');
    redInput.css('border-color', redColor);
    return false;
});

function isNumeric(key){
    return /\d/g.test(key);
}


function getNewValue(data){
    var currentValue = data.currentTarget.value.toString();
    if(data.keyCode==8 && data.currentTarget.selectionStart == data.currentTarget.value.toString().length){
        return parseInt(currentValue.substring(0, data.currentTarget.selectionStart-1))
    }
    return parseInt(currentValue.substring(0, data.currentTarget.selectionStart) + data.key + currentValue.substring(data.currentTarget.selectionEnd))
}

function hideErrorMessage(selector){
    var errorMessage = AJS.$(selector);
    errorMessage.css('visibility: hidden; font-size: 8px; color: red');
    errorMessage.text('');
    errorMessage.hide();
    resizeConfigurationForm();
}

function resizeConfigurationForm(){
    AJS.$(this)[0].gadgets.window.adjustHeight();
}

function showErrorMessage(selector, message){
    var errorMessage = AJS.$(selector);
    errorMessage.text(message);
    errorMessage.show();
    resizeConfigurationForm();

}

function isCorrectSymbol(data){
    var keyCode = data.keyCode;
    return isNumeric(data.key) || keyCode == 8 ||  keyCode == 30 || keyCode == 33 || keyCode == 127;
}


function redrawToTreeMap(){
    var chart = AJS.$("#container").highcharts();
    if(chart !=  null){
        chart.destroy();
       AJS.$(window)[0].gadgets.window.adjustHeight();
    }
    getInfoForHeatmapView(redrawChartToTreemap);
}

function redrawToDrilldown(){
    var date = gadgets.Prefs().getString('startDate');
    AJS.$('#dateConfig').datepicker('setDate', date);
    getDataForDetailView(redrawChartToDrilldown);
}

function redrawChartToTreemap(seriesData){
    AJS.$('#dateConfig').hide();
    AJS.$('#issuesDescriptionTable').hide();
    var treemapOptions = {
        chart: {
            type: 'treemap',
            height: '50%',
            layoutAlgorithm: 'squarified',
            alternateStartingDirection: true,
            zoomType: 'xy',
            margin: [50, 5, 20, 5]
        },
        tooltip: {
            useHTML: true,
            pointFormat: '<span style="font-size: 14px"><strong>{point.name}</strong></span><br/>Risk Score: {point.riskScore}<br/>Blocker: {point.blocker}<br/>Critical: {point.critical}<br/>Major: {point.major}'
        },
        credits: {
            enabled: false
        },
        plotOptions: {
            series: {
                dataLabels: {
                    enabled: true,
                    allowOverlap: false,
                    useHTML: true,
                    format: '<div style="margin-left: 10px">{point.name}<br />  Risk score: {point.riskScore}</div>',
                    style: {
                        fontWeight: 'bold',
                        fontSize: '16px',
                        textOutline:'0px'
                    }
                },
                cursor: 'pointer',
                point: {
                    events: {
                        click: function(){
                            gadgets.Prefs().set("projectName", this.name);
                            redrawToDrilldown();
                        }
                    }
                }
            }
        },
        series: [{
            type: 'treemap',
            layoutAlgorithm: 'squarified',
            data: seriesData

        }],
        navigation: {
            buttonOptions: {
                align: 'right',
                  menuItems: [
                    'back to heatmap view',
                    'downloadPNG',
                    'downloadJPEG',
                    'downloadPDF',
                    'downloadSVG'
                  ]
            }
        },
        xAxis: {
            scrollbar: {
                enabled: true
            }
        },
        yAxis: {
            scrollbar: {
                enabled: true
            }
        },
        title: {
            text: 'Projects Status Heatmap'
        }
    }
    var charts = Highcharts.chart('container', treemapOptions);
    var calculatedValuer = calculateFontSize() + 'px';
    charts.update({
         plotOptions: {
                 series: {
                    dataLabels: {
                    style: {
                        fontWeight: 'bold',
                        fontSize: calculatedValuer,
                        textOutline:'0px'
                    }
                    }
                 }
         }
    });
    charts.redraw();
    gadgets.window.adjustHeight();
}

function redrawChartToDrilldown(data) {
    var chart = AJS.$("#container").highcharts();
    if(chart !=  null){
        chart.destroy();
        AJS.$(window)[0].gadgets.window.adjustHeight();
    }
    var xAxisCategories = [];
    var seriesData = [];
    var projectName = data.projectName;
    AJS.$('#dateConfig').show();
    var today = new Date();
    today.setHours(0,0,0,0);
    data.projectInfoByDates.forEach(function(projectInfo){
        seriesData.push({
            critical: projectInfo.critical,
            blocker: projectInfo.blocker,
            major: projectInfo.major,
            y: projectInfo.riskScore
        });
        var dateOfRiskScore = new Date(projectInfo.dateOfRiskScore);
        var month = dateOfRiskScore.getMonth();
        month++;
        var formatDateOfCalc = dateOfRiskScore.getFullYear() + '-' + month + '-' + dateOfRiskScore.getDate();
        xAxisCategories.push(formatDateOfCalc);
        var dateOfCalculation = new Date(formatDateOfCalc);
        dateOfCalculation.setHours(0,0,0,0);
        if(projectInfo.issues != null){
            issuesDescription = projectInfo.issues;
        }
    });
    var gadget = AJS.$(this)[0].gadgets.Prefs();
    var amberLine = parseInt(gadget.getString("amber"));
    var redLine = parseInt(gadget.getString("red"));
    var lineChart =  {
        chart: {
            type: 'line',
            marginLeft: 60,
            marginRight: 60
        },
        credits: {
            enabled: false
        },
        subtitle: {
            text: projectName + ' project',
            align: 'left',
            x: 50
        },
        title: {
            text: 'Risk score for date range',
            align: 'left',
            x: 50
        },
        xAxis: {
            categories: xAxisCategories,

        },
        yAxis: {
            title: {
                text: 'Risk score'
            },
            min: 0,
            tickAmount: 0
        },
        legend: {
            enabled: false
        },
        tooltip: {
            useHTML: true,
            pointFormat: 'Risk Score: {point.y}<br/>Blocker: {point.blocker}<br/>Critical: {point.critical}<br/>Major: {point.major}'
        },
        plotOptions: {
            series: {
                connectNulls: true
            },
            line: {
                dataLabels: {
                    enabled: true,
                    color: '#FFFFFF'
                },
                enableMouseTracking: true
            }
        },
        series: [{
            name: ' ',
            data: seriesData,
            color: '#FFFFFF'
        }],
        navigation: {
            buttonOptions: {
                align: 'right'
            }
        },
        exporting: {
            menuItemDefinitions: {
                backToTreeMap: {
                    onclick: function(){redrawToTreeMap();},
                    text: 'Back to heatmap'
                }
            },
            buttons:{
                contextButton: {
                    menuItems: [
                       'backToTreeMap',
                       'printChart',
                       'separator',
                       'downloadPNG',
                       'downloadJPEG',
                       'downloadPDF',
                       'downloadSVG'
                    ]
                }
            }
        }
    };
    var charts = Highcharts.chart('container', lineChart);
    var calculatedValuer = calculateFontSize() + 'px';
    var maxAxisValue = charts.yAxis[0].max;
     charts.update({
        line: {
            dataLabels: {
                enabled: true
            },
            enableMouseTracking: false
        },
        plotOptions: {
            series: {
                dataLabels: {
                    style: {
                        fontWeight: 'bold',
                        fontSize: calculatedValuer,
                        textOutline:'0px'
                    }
                }
            }
        },
        yAxis: {
            plotBands: [{
                from: 0,
                to: amberLine,
                color: '#7f9943'
            },{
                from: amberLine,
                to: redLine,
                color: '#ff9f00'
            },{
                from: redLine,
                to: maxAxisValue,
                color: '#d22c32',
            }]
        }
    });
    charts.redraw();
    redrawTable();
    gadgets.window.adjustHeight();
}
