(function ($) {
    var url = AJS.contextPath() + "/rest/heatmap-dj/1.0/config";
       AJS.$(document).ready(function() {
       $.get(url).done(function(data){
            AJS.$("#projects").val(data.projects);
            AJS.$("#labels").val(data.labels);
            AJS.$("#blocker").val(data.blocker);
            AJS.$("#critical").val(data.critical);
            AJS.$("#major").val(data.major);
            AJS.$("#minor").val(data.minor);
            AJS.$("#red").val(data.red);
            AJS.$("#amber").val(data.amber);
        }).fail(function(){
            alert("error");
        });


    AJS.$("#heatmapConfig").submit(function(e) {
        e.preventDefault();
        alert("after click on submit")
        updateConfig();
    });
    });
})(AJS.$ || jQuery);


function updateConfig() {
    var formData = {
        projects : AJS.$("#projects").val(),
        labels : AJS.$("#labels").val(),
        blocker : AJS.$("#blocker").val(),
        critical : AJS.$("#critical").val(),
        major : AJS.$("#major").val(),
        minor : AJS.$("#minor").val(),
        red : AJS.$("#red").val(),
        amber : AJS.$("#amber").val()
    };
    alert(formData);
     AJS.$.ajax({
             url: AJS.contextPath() + "/rest/heatmap-dj/1.0/config",
             type: "PUT",
             contentType: "application/json",
             data: '{ "projects":"' + AJS.$("#projects").val() + '", "labels":"' + AJS.$("#labels").val() + '", "blocker":' + AJS.$("#blocker").val() + ', "critical":' + AJS.$("#critical").val() + ', "major":' + AJS.$("#major").val() + ', "minor":' + AJS.$("#minor").val() + ', "red":' + AJS.$("#red").val() + ', "amber":' + AJS.$("#amber").val() + '}',
             processData: false
     });
}
