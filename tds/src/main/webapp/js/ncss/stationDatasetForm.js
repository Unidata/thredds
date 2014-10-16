Ncss.changeSpatialSubsetType = function (evt) {
    var bboxTab = $('#inputBBoxSubset');
    var pointTab = $('#inputPointSubset');
    var listTab = $('#inputStationList');
    var bboxSubset = $("#bboxSubset");
    var pointSubset = $("#pointSubset");
    var listSubset = $("#listSubset");

    //Selecting bbox subset
    if (this == bboxTab[0]) {
        if (bboxTab.attr('class') == "unselected") {
            bboxTab.removeClass("unselected").addClass("selected");
            pointTab.removeClass("selected").addClass("unselected");
            listTab.removeClass("selected").addClass("unselected");

            //Enable bbox inputs
            Ncss.toogleBBoxSubsetting(bboxSubset, true);
            bboxSubset.removeClass("hidden");

            //Disable point subset inputs
            Ncss.tooglePointSubsetting(pointSubset, false);
            pointSubset.addClass("hidden");

            //Disable station subset input
            Ncss.toogleListSubsetting(listSubset, false);
            listSubset.addClass("hidden");
        }
    }

    //Selecting point subset
    if (this == pointTab[0]) {
        if (pointTab.attr('class') == "unselected") {
            pointTab.removeClass("unselected").addClass("selected");
            bboxTab.removeClass("selected").addClass("unselected");
            listTab.removeClass("selected").addClass("unselected");

            //Enable point inputs
            Ncss.tooglePointSubsetting(pointSubset, true);
            pointSubset.removeClass("hidden");

            //Disable bbox subset inputs
            Ncss.toogleBBoxSubsetting(bboxSubset, false);
            bboxSubset.addClass("hidden");

            //Disable station subset input
            Ncss.toogleListSubsetting(listSubset, false);
            listSubset.addClass("hidden");
        }
    }

    //Selecting station subset
    if (this == listTab[0]) {
        if (listTab.attr('class') == "unselected") {
            listTab.removeClass("unselected").addClass("selected");
            bboxTab.removeClass("selected").addClass("unselected");
            pointTab.removeClass("selected").addClass("unselected");

            //Enable station inputs
            Ncss.toogleListSubsetting(listSubset, true);
            listSubset.removeClass("hidden");

            //Disable bbox subset inputs
            Ncss.toogleBBoxSubsetting(bboxSubset, false);
            bboxSubset.addClass("hidden");

            //Disable station subset input
            Ncss.tooglePointSubsetting(pointSubset, false);
            pointSubset.addClass("hidden");
        }
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

/*
 Ncss.verticalSubsetting = function(){
 var singleLevelTab= $('#inputSingleLevel');
 var verticalStrideTab =$('#inputVerticalStride');
 var inputSingleLevel = $('#singleLevel');
 var inputStrideLevels = $('#strideLevels');

 if(singleLevelTab.attr('class') ==="selected" ){
 singleLevelTab.removeClass("selected").addClass("unselected");
 verticalStrideTab.removeClass("unselected").addClass("selected");
 inputSingleLevel.addClass('hidden');
 inputStrideLevels.removeClass('hidden');

 $('input[name=vertCoord]').attr("disabled","disabled");
 $('input[name=vertStride]').removeAttr("disabled");
 }else{
 verticalStrideTab.removeClass("selected").addClass("unselected");
 singleLevelTab.removeClass("unselected").addClass("selected");
 inputStrideLevels.addClass('hidden');
 inputSingleLevel.removeClass('hidden');

 $('input[name=vertStride]').attr("disabled","disabled");
 $('input[name=vertCoord]').removeAttr("disabled");
 }
 };
 */

Ncss.initStationDataset = function () {
    Ncss.initStationDatasetForm();
    Ncss.buildAccessUrl();
};

Ncss.initStationDatasetForm = function () {
    Ncss.log("initStationDataset...(starts)");

    //Add events to spatial subset selectors
    $('#inputBBoxSubset').click(Ncss.changeSpatialSubsetType);
    $('#inputPointSubset').click(Ncss.changeSpatialSubsetType);
    $('#inputStationList').click(Ncss.changeSpatialSubsetType);
    //$('#disableLLSubset').change(Ncss.toogleLLSubsetting);
    //$('#disableProjSubset').change(Ncss.toogleProjSubsetting);

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

    //Add events to all stations check
    $('#stns_all').click(Ncss.selectAllStns);

    //Add events to vertical subset selectors
    //$('#inputSingleLevel').click(Ncss.verticalSubsetting);
    //$('#inputVerticalStride').click(Ncss.verticalSubsetting);

    //Filters unwanted stuff in the url
    $('#form').on('submit', function () {
        $('#stns_all').prop("disabled", "disabled");
    });

    Ncss.log("initStationDataset...(ends)");
};

Ncss.selectAllStns = function () {
    if (this.checked)
        $("#stns").val("all");
    else
        $("#stns").val('');
};

Ncss.toogleBBoxSubsetting = function (bboxSubset, enable) {
    //Enable bbox inputs
    var inputs = $(':input[type=text]', bboxSubset);

    for (var i = 0; i < inputs.length; i++) {
        if (enable)
            $(inputs[i]).removeAttr("disabled");
        else
            $(inputs[i]).attr("disabled", "disabled");
    }
};

Ncss.tooglePointSubsetting = function (pointSubset, enable) {
    //Enable point inputs
    var latInput = $(':input[name=latitude]', pointSubset)[0];
    var lonInput = $(':input[name=longitude]', pointSubset)[0];

    if (enable) {
        $(latInput).removeAttr("disabled");
        $(lonInput).removeAttr("disabled");
    } else {
        $(latInput).attr("disabled", "disabled");
        $(lonInput).attr("disabled", "disabled");
    }
};

Ncss.toogleListSubsetting = function (listSubset, enable) {
    //Disable station subset input
    var listInput = $(':input[name=stns]', listSubset)[0];
    var subsetInput = $(':input[name=subset]', listSubset)[0];

    if (enable) {
        $(listInput).removeAttr("disabled");
        $(subsetInput).removeAttr("disabled");
    } else {
        $(listInput).attr("disabled", "disabled");
        $(subsetInput).attr("disabled", "disabled");
    }
};
