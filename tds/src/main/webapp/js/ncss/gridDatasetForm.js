var map;

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

        if (!$('#disableProjSubset').is(':checked')) {
            var inputs = $(':input[type=text]', coordinateSubset);
            for (var i = 0; i < inputs.length; i++) {
                $(inputs[i]).removeAttr("disabled");
            }
        }

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

        if (!$('#disableLLSubset').is(':checked')) {
            var inputs = $(':input[type=text]', latlonSubset);
            for (var i = 0; i < inputs.length; i++) {
                $(inputs[i]).removeAttr("disabled");
            }
        }

        $('input[name=maxy]').attr("disabled", "disabled");
        $('input[name=miny]').attr("disabled", "disabled");
        $('input[name=minx]').attr("disabled", "disabled");
        $('input[name=maxx]').attr("disabled", "disabled");
    }
};

Ncss.changeTemporalSubsetting = function () {
    var timeRangeTab = $('#inputTimeRange');
    var singleTimeTab = $('#inputSingleTime');
    var temporalSubset = $('#timeRangeSubset');
    var singleTimeSubset = $('#singleTimeSubset');

    if (timeRangeTab.attr('class') == "selected") {
        timeRangeTab.removeClass("selected").addClass("unselected");
        singleTimeTab.removeClass("unselected").addClass("selected");
        temporalSubset.addClass('hidden');
        singleTimeSubset.removeClass('hidden');

        $('input[name=time]').removeAttr("disabled");
        $('input[name=time_start]').attr("disabled", "disabled");
        $('input[name=time_end]').attr("disabled", "disabled");
        $('input[name=timeStride]').attr("disabled", "disabled");
    } else {
        timeRangeTab.removeClass("unselected").addClass("selected");
        singleTimeTab.removeClass("selected").addClass("unselected");
        singleTimeSubset.addClass('hidden');
        temporalSubset.removeClass('hidden');

        $('input[name=time]').attr("disabled", "disabled");
        $('input[name=time_start]').removeAttr("disabled");
        $('input[name=time_end]').removeAttr("disabled");
        $('input[name=timeStride]').removeAttr("disabled");
    }
};

Ncss.verticalSubsetting = function () {
    var singleLevelTab = $('#inputSingleLevel');
    var verticalStrideTab = $('#inputVerticalStride');
    var inputSingleLevel = $('#singleLevel');
    var inputStrideLevels = $('#strideLevels');

    if (singleLevelTab.attr('class') === "selected") {
        singleLevelTab.removeClass("selected").addClass("unselected");
        verticalStrideTab.removeClass("unselected").addClass("selected");
        inputSingleLevel.addClass('hidden');
        inputStrideLevels.removeClass('hidden');

        $('input[name=vertCoord]').attr("disabled", "disabled");
        $('input[name=vertStride]').removeAttr("disabled");
    } else {
        verticalStrideTab.removeClass("selected").addClass("unselected");
        singleLevelTab.removeClass("unselected").addClass("selected");
        inputStrideLevels.addClass('hidden');
        inputSingleLevel.removeClass('hidden');

        $('input[name=vertStride]').attr("disabled", "disabled");
        $('input[name=vertCoord]').removeAttr("disabled");
    }
};

Ncss.initGridDataset = function () {
    Ncss.initGridDatasetForm();
    Ncss.initMapPreview();
    Ncss.buildAccessUrl();
};

Ncss.initGridDatasetForm = function () {
    Ncss.log("initGridDatasetForm...(starts)");

    //Add events to spatial subset selectors
    $('#inputLatLonSubset').click(Ncss.changeSpatialSubsetType);
    $('#inputCoordSubset').click(Ncss.changeSpatialSubsetType);
    $('#disableLLSubset').change(Ncss.toogleLLSubsetting);
    $('#disableProjSubset').change(Ncss.toogleProjSubsetting);

    // Programmatically "checking" these boxes.
    // That means that horizontal subsetting will be disabled by default.
    if (!$('#disableLLSubset').is(':checked')) {
        $('#disableLLSubset').click();
    }
    if (!$('#disableProjSubset').is(':checked')) {
        $('#disableProjSubset').click();
    }

    Ncss.fullLatLonExt = {
        north: $('input[name=dis_north]').val(),
        south: $('input[name=dis_south]').val(),
        west: $('input[name=dis_west]').val(),
        east: $('input[name=dis_east]').val()
    };

    Ncss.fullProjExt = {
        maxy: $('input[name=dis_maxy]').val(),
        miny: $('input[name=dis_miny]').val(),
        minx: $('input[name=dis_minx]').val(),
        maxx: $('input[name=dis_maxx]').val()
    };

    $('#resetLatLonbbox').click(function () {
        $('input[name=north]').val(Ncss.fullLatLonExt.north);
        $('input[name=south]').val(Ncss.fullLatLonExt.south);
        $('input[name=west]').val(Ncss.fullLatLonExt.west);
        $('input[name=east]').val(Ncss.fullLatLonExt.east);
        Ncss.buildAccessUrl();
    });

    $('#resetProjbbox').click(function () {
        $('input[name=maxy]').val(Ncss.fullProjExt.maxy);
        $('input[name=miny]').val(Ncss.fullProjExt.miny);
        $('input[name=minx]').val(Ncss.fullProjExt.minx);
        $('input[name=maxx]').val(Ncss.fullProjExt.maxx);
        Ncss.buildAccessUrl()
    });

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
        Ncss.buildAccessUrl()
    });

    //Add events to vertical subset selectors
    $('#inputSingleLevel').click(Ncss.verticalSubsetting);
    $('#inputVerticalStride').click(Ncss.verticalSubsetting);

    //Filters unwanted stuff in the url
    $('#form').on('submit', function () {
        Ncss.log('submitting....');
        $('#disableLLSubset').prop("disabled", "disabled");
        $('#disableProjSubset').prop("disabled", "disabled");
    });

    Ncss.log("initGridDatasetForm...(ends)");
};

Ncss.toogleLLSubsetting = function () {
    Ncss.log("Will disable/enable spatial subsetting...");

    if (this.checked) {
        Ncss.log("disabling bounding params...");
        var inputs = $(':input[type=text]', $('#latlonSubset'));

        for (var i = 0; i < inputs.length; i++) {
            $(inputs[i]).attr("disabled", "disabled");
        }
    } else {
        Ncss.log("enabling bounding params...");
        var inputs = $(':input[type=text]', $('#latlonSubset'));

        for (var i = 0; i < inputs.length; i++) {
            $(inputs[i]).removeAttr("disabled");
        }
    }
};

Ncss.toogleProjSubsetting = function () {
    Ncss.log("Will disable/enable spatial subsetting...");

    if (this.checked) {
        Ncss.log("disabling bounding params...");
        var inputs = $(':input[type=text]', $('#coordinateSubset'));

        for (var i = 0; i < inputs.length; i++) {
            $(inputs[i]).attr("disabled", "disabled");
        }
    } else {
        Ncss.log("enabling bounding params...");
        var inputs = $(':input[type=text]', $('#coordinateSubset'));

        for (var i = 0; i < inputs.length; i++) {
            $(inputs[i]).removeAttr("disabled");
        }
    }
};
