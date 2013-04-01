$(document).ready(function() {
    $("table tbody tr:nth-child(odd)").addClass("odd");
    $("table.list").tablesorter();
    $("table.list").bind("sortEnd",function() { 
        $("table tbody tr").removeClass("odd");
        $("table tbody tr:nth-child(odd)").addClass("odd");
    }); 
});


