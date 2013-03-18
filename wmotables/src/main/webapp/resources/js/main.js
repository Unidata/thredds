$(document).ready(function() {

      $("table tbody tr:nth-child(odd)", this).addClass("odd");
    $("table").tablesorter();
});

/* 
 Generic JQuery POST function 
 */
function doPost (url, data, callback, dataType) {
    $.post(
        url, 
        data,
        function(data) {
            callback
        }, 
        dataType
    );
}


