$(document).ready(function() {

    // automagically make any image alt a tooltip
    
    $(document).tooltip({ items: "img[alt]",
        content: function() { return $(this).attr("alt") } 
    });


    $("table tbody tr:nth-child(odd)").addClass("odd");
    $("table.list").tablesorter();
    $("table.list").bind("sortEnd",function() { 
        $("table tbody tr").removeClass("odd");
        $("table tbody tr:nth-child(odd)").addClass("odd");
    }); 
});


