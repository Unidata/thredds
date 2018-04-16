$(document).ready(function () {
    addNotice();
    setTutorialNav();

    function setTutorialNav() {
        var pageTitle = $("title").html();
        if (pageTitle != 'THREDDS Data Server Administration Tutorial') {
            var workshopNav = '<ul id="breadcrumbs"">' +
                    '<li><a href="index.html">TDS Administration Tutorial Home Page</a></li>' +
                    '<li>' + pageTitle + '</li>' +
                    '</ul>';

            $(workshopNav).prependTo('#container');
            $(workshopNav).appendTo('#container');
        }
    }



    function addNotice() {
        var date_modified = "";
        var modDate = new Date(document.lastModified)
        var m_names = new Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
        if (Date.parse(document.lastModified) != 0) {
            var modiDate = new Date(document.lastModified);
            var monthName = new Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
            date_modified = "Last updated: " + monthName[modiDate.getMonth()] + " " + modiDate.getDate() + ", " + modiDate.getFullYear();
        }


        var notice = '<p class="notice">This document is maintained by Unidata.  Send comments to <a href="mailto:support-thredds@unidata.ucar.edu">THREDDS support</a>. &nbsp; ' + date_modified + '</p>';
        $(notice).appendTo('#container');
    }


});
