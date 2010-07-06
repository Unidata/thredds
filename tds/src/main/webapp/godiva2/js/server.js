/**
 * Contains methods for reading metadata from the server.  The server can also
 * delegate requests to a third-party server, thereby allowing third-party data
 * to be displayed on the Godiva2 site.
 */

// Converts ISO8601 string to Date object
// From http://delete.me.uk/2005/03/iso8601.html, copied 16th October 2007
function iso8601ToDate(string)
{
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
        "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?" +
        "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?";
    var d = string.match(new RegExp(regexp));

    var offset = 0;
    var date = new Date();
    date.setFullYear(d[1]);

    if (d[3]) { date.setMonth(d[3] - 1); }
    if (d[5]) { date.setDate(d[5]); }
    if (d[7]) { date.setHours(d[7]); }
    if (d[8]) { date.setMinutes(d[8]); }
    if (d[10]) { date.setSeconds(d[10]); }
    if (d[12]) { date.setMilliseconds(Number("0." + d[12]) * 1000); }
    if (d[14]) {
        offset = (Number(d[16]) * 60) + Number(d[17]);
        offset *= ((d[15] == '-') ? 1 : -1);
    }

    //offset -= date.getTimezoneOffset();  We don't correct for the local time zone offset
    var time = (Number(date) + (offset * 60 * 1000));
    var ret = new Date();
    ret.setTime(Number(time));
    return ret;
}
    
/**
 * Function for making an Ajax request to the server.  This will
 * simply throw an alert if the XMLHttpRequest call fails.
 * @param url URL to the third-party server or '' to read data from the "home" server
 * @param Object containing parameters, which must include:
 *        urlparams Object containing the URL parameters
 *        onSuccess Callback to be called with the JSON object that is
 *                  returned from the server
 *        onServerError Callback to be called when the server reports an error
 *                  (if omitted, a default is provided)
 */
function makeAjaxRequest(url, params) {
    if (typeof params.onServerError == 'undefined') {
        params.onServerError = function(exception) {
            alert("Server exception class: " + exception.className + 
                ", Message: " + exception.message);
        }
    }
    // Add the common elements to the URL parameters
    /*if (url != null && url != '') {
        params.urlparams.url = url;
    }*/
    params.urlparams.request = 'GetMetadata';

    new Ajax.Request(url, {
        method: 'get',
        parameters: params.urlparams,
        onSuccess: function(transport) {
            try {
                var myobj = transport.responseText.evalJSON();
            } catch(err) {
                alert("Invalid JSON returned from server");
                return;
            }
            if (typeof myobj.exception == 'undefined') {
                params.onSuccess(myobj);
            } else {
                params.onServerError(myobj.exception);
            }
        },
        onFailure: function() {
            alert('Error getting data from server'); // TODO: get the full URL somehow?
        }
    });
}

/**
 * Gets the skeleton hierarchy of layers that are offered by this server
 * @param node Root node to which this hierarchy will be added
 * @param params Object containing a callback that will be called when the result
 * is returned from the server and an optional menu argument that can be used to
 * load a specific menu structure.
 */
function getMenu(node, params) {
    makeAjaxRequest(node.data.server, {
        urlparams: {
            item: 'menu',
            menu: typeof params.menu == 'undefined' ? '' : params.menu
        },
        onSuccess: function(layers) { 
            params.callback(node, layers);
        }
    });
}

/**
 * Gets the details for the given displayable layer
 * @param Object containing parameters, which must include:
 *        callback the function to be called with the object that is returned
 *            from the call to the server
 *        layerName The unique ID for the displayable layer
 *        time The time that we're currently displaying on the web interface
 *            (the server calculates the nearest point on the t axis to this time).
 */
function getLayerDetails(url, params) {
    makeAjaxRequest(url, {
        urlparams: {
            item: 'layerDetails',
            layerName : params.layerName,
            time: params.time
        },
        onSuccess: function(layerDetails) {
            // Convert the nearest-time ISO string to a Javascript date object
            if (typeof layerDetails.nearestTimeIso != 'undefined') {
                layerDetails.nearestTime = iso8601ToDate(layerDetails.nearestTimeIso);
            }
            layerDetails.server = url;
            layerDetails.id = params.layerName;
            params.callback(layerDetails);
        }
    });
}

/**
 * Gets the timesteps for the given displayable layer and the given day
 * @param Object containing parameters, which must include:
 *        callback the function to be called with the object that is returned
 *            from the call to the server (an array of times as strings)
 *        layerName The unique ID for the displayable layer
 *        day The day for which we will request the timesteps, in "yyyy-mm-dd"
 *            format
 */
function getTimesteps(url, params) {
    makeAjaxRequest(url, {
        urlparams: {
            item: 'timesteps',
            layerName: params.layerName,
            // TODO: Hack! Use date only and adjust server-side logic
            day: params.day + 'T00:00:00Z'
        },
        onSuccess: function(timesteps) {
            params.callback(params.day, timesteps.timesteps);
        }
    });
}

/**
 * Gets the min and max values of the layer for the given time, depth and
 * spatial extent (used by the auto-scale function).  ncWMS layers only.
 * @param Object containing parameters, which must include:
 *        callback the function to be called with the object that is returned
 *            from the call to the server (simple object with properties "min" and "max")
 *        layerName The unique ID for the displayable layer
 *        bbox Bounding box *string* (e.g. "-180,-90,180,90")
 *        crs CRS *String* (not a Projection object)
 *        elevation Elevation value
 *        time Time value
 */
function getMinMax(url, params) {
    makeAjaxRequest(url, {
        urlparams: {
            item: 'minmax',
            layers: params.layerName,
            bbox: params.bbox,
            elevation: params.elevation,
            time: params.time,
            crs: params.crs,
            width: 50, // Request only a small box to save extracting lots of data
            height: 50
        },
        onSuccess: params.callback
    });
}

/**
 * Gets the list of animation timesteps from the given layer ncWMS layers only.
 * @param Object containing parameters, which must include:
 *        callback the function to be called with the object that is returned
 *            from the call to the server (simple object with properties "min" and "max")
 *        layerName The unique ID for the displayable layer
 *        start Start time for the animation in ISO8601
 *        start End time for the animation in ISO8601
 */
function getAnimationTimesteps(url, params) {
    makeAjaxRequest(url, {
        urlparams: {
            item: 'animationTimesteps',
            layerName: params.layerName,
            start: params.startTime,
            end: params.endTime
        },
        onSuccess: function(timestrings) {
            params.callback(timestrings.timeStrings);
        }
    });
}

/**
 * Gets a link to a screenshot that is generated on the server
 */
function getScreenshotLink(url, params) {
    // Add the common elements to the URL pa
    new Ajax.Request('screenshots/createScreenshot', {
        method: 'post',
        parameters: params.urlparams,
        onSuccess: function(transport) {
            try {
                var myobj = transport.responseText.evalJSON();
            } catch(err) {
                alert("Invalid JSON returned from server");
                return;
            }
            if (typeof myobj.exception == 'undefined') {
                params.callback(myobj.screenshotUrl);
            } else {
                params.error(myobj.exception);
            }
        },
        onFailure: function() {
            var exception = new Object();
            exception.className="Transport error";
            exception.message=('Error getting data from server'); // TODO: get the full URL somehow?
            params.error(exception);
        }
    });
}