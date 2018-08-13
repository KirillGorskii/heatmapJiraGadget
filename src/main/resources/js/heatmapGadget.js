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

AJS.$(document).on("click", "#showDescription", function(data){
    var table = AJS.$('#issuesDescriptionTable')
    AJS.$('#issuesDescriptionTable').show();
    if(table){
        table.empty();
    }
    table.append("<tr class='headerRow'><td id='calculatedRateScore'><strong>Contributed rate score:</strong></td>"
    + "<td id='issueKey'><strong>Issue name</strong></td><td id='issueSummary'><strong>Summary<strong></td>"
    + "<td id='issueExpiration'><strong>Expired by</strong></td><td id='assignee'><strong>Assignee</strong></td>"
    + "<td id='issuePriority'><strong>Issue priority</strong></td></tr>");
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
        + "<td id='issueKey'><a href='"+linkToIssue+"' target='_blank'>" + issue.issueKey + "</a></td><td id='issueSummary'>" + issue.summary + "</td>"
        + "<td id='issueExpiration'>" + issue.issueExpiration + " days</td><td id='assignee'>" + issue.assignee + "</td>"
        + "<td id='issuePriority'>" + issue.issuePriority + "</td></tr>");
    });
    AJS.$(window)[0].gadgets.window.adjustHeight();
});


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

