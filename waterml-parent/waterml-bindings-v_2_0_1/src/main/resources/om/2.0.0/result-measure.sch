<?xml version="1.0" encoding="UTF-8"?>
<assert
    xmlns="http://purl.oclc.org/dsdl/schematron"
    test="(om:result/@uom castable as gml:UomSymbol or om:result/@uom castable as gml:UomURI) and om:result castable as xs:double">result
    model must match gml:MeasureType</assert>
