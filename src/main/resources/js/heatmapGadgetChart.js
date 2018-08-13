var issuesDescription;

function redrawToTreeMap(){
    var chart = AJS.$("#container").highcharts();
    if(chart !=  null){
        chart.destroy();
    }
    getInfoForHeatmapView(redrawChartToTreemap);

}

function redrawToDrilldown(){
    AJS.$('#dateConfig').datepicker('setDate', gadgets.Prefs().getString('startDate'));
    getDataForDetailView(redrawChartToDrilldown(data));

}

function  redrawChartToTreemap(records){
    var seriesData = [];
    records.forEach(function(record){
        seriesData.push({
            name: record.projectName,
            color: record.color,
            value: record.squareSize,
            riskScore: record.riskScore,
            link: record.link,
            critical: record.critical,
            blocker: record.blocker,
            major: record.major
        });
    });
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
                align: 'left',
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
    }
    var xAxisCategories = [];
    var seriesData = [];
    var projectName = data.projectName;
    AJS.$('#dateConfig').show();
    AJS.$('#issuesDescriptionTable').hide();
    var today = new Date();
    today.setHours(0,0,0,0);
    data.projectInfoByDates.forEach(function(projectInfo){
        seriesData.push({
            critical: projectInfo.critical,
            blocker: projectInfo.blocker,
            major: projectInfo.major,
            y: projectInfo.riskScore
        });
        var dateOfRiskScore = projectInfo.dateOfRiskScore;
        var formatDateOfCalc = dateOfRiskScore.monthValue + '/' + dateOfRiskScore.dayOfMonth + '/' + dateOfRiskScore.year;
        xAxisCategories.push(formatDateOfCalc);
        var dateOfCalculation = new Date(formatDateOfCalc);
        dateOfCalculation.setHours(0,0,0,0);
        if( today.getTime() === dateOfCalculation.getTime()){
            issuesDescription = projectInfo.issues;
        }
    });
    var gadget = AJS.$(this)[0].gadgets.Prefs();
    var amberLine = parseInt(gadget.getString("amber"));
    var redLine = parseInt(gadget.getString("red"));
    var lineChart =  {
        chart: {
            type: 'line'
        },
        credits: {
            enabled: false
        },
        title: {
            text: 'Risk score for date range for ' + projectName + ' project'
        },
        xAxis: {
            title: {
                text: 'Date'
            },
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
                align: 'left'
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
                color: '#baffc9'
            },{
                from: amberLine,
                to: redLine,
                color: '#ffdfba'
            },{
                from: redLine,
                to: maxAxisValue,
                color: '#ffb3ba',
            }]
        }
    });
    charts.redraw();
    gadgets.window.adjustHeight();
}
