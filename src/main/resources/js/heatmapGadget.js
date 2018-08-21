 function changeSelect(eventData){

    var data = eventData.currentTarget.selectedOptions;
    var gadget = AJS.$(window)[0].gadgets;
    var values = [];
    var projects;
    if(data.length>0){
        for(var i = 0; i < data.length; i++){
            values.push(data[i].value);
        }
        if(values.length>1){
            projects = values.join(',');
        } else {
            projects = values[0];
        }
    }
    gadget.Prefs().set('projects', projects);
    gadget.window.adjustHeight();
}

AJS.$(document).on('select2:select', '.projects', function(eventData){
    changeSelect(eventData);
});

AJS.$(document).on('select2:unselect', '.projects', function(eventData){
    changeSelect(eventData);
});