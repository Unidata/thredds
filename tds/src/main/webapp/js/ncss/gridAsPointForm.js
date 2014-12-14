/*
Ncss.changeSpatialSubsetType = function () {
    var latlonTab = $('#inputLatLonSubset');
    var coordTab = $('#inputCoordSubset');
    var coordinateSubset = $("#coordinateSubset");
    var latlonSubset = $("#latlonSubset");

    if (latlonTab.attr('class') == "selected") {
        latlonTab.removeClass("selected").addClass("unselected");
        coordTab.removeClass("unselected").addClass("selected");
        latlonSubset.addClass("hidden");
        coordinateSubset.removeClass("hidden");

        $('input[name=maxy]').removeAttr("disabled");
        $('input[name=miny]').removeAttr("disabled");
        $('input[name=minx]').removeAttr("disabled");
        $('input[name=maxx]').removeAttr("disabled");
        $('input[name=north]').attr("disabled", "disabled");
        $('input[name=south]').attr("disabled", "disabled");
        $('input[name=west]').attr("disabled", "disabled");
        $('input[name=east]').attr("disabled", "disabled");
    } else {
        latlonTab.removeClass("unselected");
        latlonTab.addClass("selected");
        coordTab.removeClass("selected");
        coordTab.addClass("unselected");
        latlonSubset.removeClass("hidden");
        coordinateSubset.addClass("hidden");

        $('input[name=north]').removeAttr("disabled");
        $('input[name=south]').removeAttr("disabled");
        $('input[name=west]').removeAttr("disabled");
        $('input[name=east]').removeAttr("disabled");
        $('input[name=maxy]').attr("disabled", "disabled");
        $('input[name=miny]').attr("disabled", "disabled");
        $('input[name=minx]').attr("disabled", "disabled");
        $('input[name=maxx]').attr("disabled", "disabled");
    }
};
*/

Ncss.initGridAsPoint = function () {
    Ncss.initGridAsPointForm();
    Ncss.initMapPreview();
    Ncss.buildAccessUrl();
};

Ncss.initGridAsPointForm = function () {
    Ncss.log("initGridAsPointForm...(starts)");

    //Add events to temporal subset selectors
    $('#inputTimeRange').click(Ncss.changeTemporalSubsetting);
    $('#inputSingleTime').click(Ncss.changeTemporalSubsetting);

    Ncss.fullTimeExt = {
        time_start: $('input[name=dis_time_start]').val(),
        time_end: $('input[name=dis_time_end]').val()
    };

    $('#resetTimeRange').click(function () {
        $('input[name=time_start]').val(Ncss.fullTimeExt.time_start);
        $('input[name=time_end]').val(Ncss.fullTimeExt.time_end);
        Ncss.buildAccessUrl();
    });

    Ncss.buildAccessUrl();
    Ncss.log("initGridAsPointForm...(ends)");
};
