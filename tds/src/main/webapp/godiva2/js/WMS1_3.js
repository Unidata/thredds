/* Copyright (c) 2006 University of Reading, UK, published under the BSD license. */
// @require: OpenLayers/Layer/WMS.js
/**
* A WMS 1.3.0 layer, created by subclassing the WMS (1.1.1) layer type
* @class
*/
OpenLayers.Layer.WMS1_3 = OpenLayers.Class(OpenLayers.Layer.WMS1_1_1, {

    /** @final @type hash */
    DEFAULT_PARAMS: { service: "WMS",
                      version: "1.3.0",
                      request: "GetMap",
                      styles: "",
                      exceptions: "XML",  // todo, should be INIMAGE
                      format: "image/png",
                      crs: "CRS:84"
                     },
                     
    projection: 'none',
    srsKey: 'CRS', // WMS 1.3.0 uses "CRS=" instead of "SRS="
                     
    /** @final @type String */
    CLASS_NAME: "OpenLayers.Layer.WMS1_3"
});
