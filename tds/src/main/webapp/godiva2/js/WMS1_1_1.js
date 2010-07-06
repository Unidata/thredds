/* 
 * Modifies the default OpenLayers WMS layer implementation by setting
 * wrapDateLine = true, transitionEffect = 'resize', format='image/png'
 * and preventing the loading of tiles outside the latitude range [-90,90]
 * (reducing load on the target WMS server).
 */

OpenLayers.Layer.WMS1_1_1 = OpenLayers.Class(OpenLayers.Layer.WMS, {
    
    /**
     * Constant: DEFAULT_PARAMS
     * {Object} Hashtable of default parameter key/value pairs 
     */
    DEFAULT_PARAMS: { service: "WMS",
                      version: "1.1.1",
                      request: "GetMap",
                      styles: "",
                      exceptions: "application/vnd.ogc.se_inimage",
                      format: "image/png"
                     },
    
    wrapDateLine: true,
    //transitionEffect: 'resize', // Seems to cause strange behaviour sometimes
    srsKey: 'SRS', // Can be overridden by subclasses
    
    /*
     * Overrides function in superclass, preventing the loading of tiles outside
     * the latitude range [-90,90] in EPSG:4326
     */
    getURL: function(bounds) {
        bounds = this.adjustBounds(bounds);
        if (this.isLatLon() && (bounds.bottom >= 90 || bounds.top <= -90)) {
            return "./images/blank.png"; // TODO: specific to this application
        }
        var imageSize = this.getImageSize();
        var newParams = {
            'BBOX': this.encodeBBOX ?  bounds.toBBOX() : bounds.toArray(),
            'WIDTH': imageSize.w,
            'HEIGHT': imageSize.h
        };
        return this.getFullRequestString(newParams);
    },
    
    /*
     * returns true if this layer is in lat-lon projection
     */
    isLatLon: function() {
        return this.params[this.srsKey] == 'EPSG:4326' || this.params[this.srsKey] == 'CRS:84';
    },
    
    /*
     * Replaces superclass implementation, allowing for the fact that subclasses
     * might use different keys for the SRS= parameter
     */
    getFullRequestString:function(newParams, altUrl) {
        var projectionCode = this.map.getProjection();
        this.params[this.srsKey] = (projectionCode == "none") ? null : projectionCode;

        return OpenLayers.Layer.Grid.prototype.getFullRequestString.apply(
                                                    this, arguments);
    },
    
    CLASS_NAME: "OpenLayers.Layer.WMS1_1_1"
});
