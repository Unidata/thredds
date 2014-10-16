Ncss.map = null;
Ncss.log("Ncss loading...");

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

Ncss.initMapPreview = function () {
    var vector_layer = new OpenLayers.Layer.Vector({
        renderers: ["Canvas"]
    });

    var wkt = new OpenLayers.Format.WKT();
    vector_layer.addFeatures(wkt.read(gridWKT));
    var geometry = OpenLayers.Geometry.fromWKT(gridWKT);
    var gridCentroid = geometry.getCentroid();

    //Checks if we got a geometry, if so we should get a good centroid for it.
    //otherwise preview is not available
    //if(typeof gridCentroid !== "undefined" ){

    if (gridCentroid !== null) {
        // Init map preview
        map = new OpenLayers.Map({
            div: "gridPreview",
            theme: null,
            controls: [new OpenLayers.Control.Navigation(), new OpenLayers.Control.Zoom(), new OpenLayers.Control.MousePosition({numDigits: 2, separator: '|', emptyString: 'Mouse is not over map', formatOutput: ncssFormatOutput})], //No controls in preview
            layers: [
                new OpenLayers.Layer.MapServer("Basic", "http://vmap0.tiles.osgeo.org/wms/vmap0",
                    {layers: 'basic'},
                    {wrapDateLine: true}
                )
            ],
            center: new OpenLayers.LonLat(gridCentroid.x, gridCentroid.y),
            zoom: 0
        });

        map.addLayer(vector_layer);
        Ncss.gridCrossesDateLine =
            (vector_layer.getDataExtent().left <= 180 & vector_layer.getDataExtent().right >= 180);

        Ncss.log("layer data extent:" +
            vector_layer.getDataExtent().left + ", " +
            vector_layer.getDataExtent().bottom + ", " +
            vector_layer.getDataExtent().right + ", " +
            vector_layer.getDataExtent().top);

        map.zoomToExtent(new OpenLayers.Bounds(
            vector_layer.getDataExtent().left,
            vector_layer.getDataExtent().bottom,
            vector_layer.getDataExtent().right,
            vector_layer.getDataExtent().top));
    } else {
        //preview not available --> hide the map div
        $('#gridPreviewFrame').css('border', 'none');
    }
};

Ncss.buildAccessUrl = function () {
    var req = $("form").serialize();
    var serverUrl = document.URL.split("/thredds/")[0];
    var dataUrl = serverUrl + $("#datasetPath")[0].innerHTML;
    $("#urlBuilder").html(dataUrl + "?" + req);
};

Ncss.resetForm = function () {
    $("form")[0].reset();
    Ncss.buildAccessUrl();
};

var ncssFormatOutput = function (lonLat) {
    var digits = parseInt(this.numDigits);

    if (Ncss.gridCrossesDateLine) {
        if (lonLat.lon < 0) {
            lonLat.lon += 360;
        }
    }

    var newHtml =
        this.prefix +
        lonLat.lon.toFixed(digits) +
        this.separator +
        lonLat.lat.toFixed(digits) +
        this.suffix;

    return newHtml;
};
