<?xml version="1.0" encoding="UTF-8"?>
<schema
    fpi="http://schemas.opengis.net/samplingSpatial/2.0/samplingCurve.sch"
    see="http://www.opengis.net/doc/IP/OMXML/2.0"
    xmlns="http://purl.oclc.org/dsdl/schematron"
    queryBinding="xslt2"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
    <!--
        This Schematron schema checks that the type of the spatial sampling feature shape is correct. 
        
        Observations and Measurements - XML Implementation is an OGC Standard.
        Copyright (c) [2010] Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/. 
    -->

    <title>Sampling point validation</title>
    <p>Verifies that all instances of SF_SpatialSamplingFeature have a shape that matches the pattern for SamplingCurve</p>
    <ns
        prefix="gml"
        uri="http://www.opengis.net/gml/3.2"/>
    <ns
        prefix="sam"
        uri="http://www.opengis.net/sampling/2.0"/>
    <ns
        prefix="sams"
        uri="http://www.opengis.net/samplingSpatial/2.0"/>
    <ns
        prefix="xsi"
        uri="http://www.w3.org/2001/XMLSchema-instance"/>
    <ns
        prefix="xlink"
        uri="http://www.w3.org/1999/xlink"/>

    <xsl:import-schema
        schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"/>

    <pattern
        id="sampling-type-point">
        <rule
            context="//sams:SF_SpatialSamplingFeature">
            <include
                href="./shape-curve.sch"/>
        </rule>
    </pattern>

</schema>
