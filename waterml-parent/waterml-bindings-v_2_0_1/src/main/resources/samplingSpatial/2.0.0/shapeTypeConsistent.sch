<?xml version="1.0" encoding="UTF-8"?>
<schema
    fpi="http://schemas.opengis.net/om/2.0/shapeTypeConsistent.sch"
    see="http://www.opengis.net/doc/IP/OMXML/2.0"
    xmlns="http://purl.oclc.org/dsdl/schematron"
    queryBinding="xslt2"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
    <!--
        Observations and Measurements - XML Implementation is an OGC Standard.
        Copyright (c) [2010] Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/. 
    -->

    <title>Spatial sampling feature validation</title>
    <p>This Schematron schema checks that the content model of each sampling feature shape is
        consistent with the type of the sampling feature.</p>
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
        id="samplingFeature-type-point">
        <rule
            context="//sams:SF_SpatialSamplingFeature[ sam:type/@xlink:href='http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingPoint' ] ">
            <include
                href="./shape-point.sch"/>
        </rule>
    </pattern>

    <pattern
        id="samplingFeature-type-curve">
        <rule
            context="//sams:SF_SpatialSamplingFeature[ sam:type/@xlink:href='http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingCurve' ] ">
            <include
                href="./shape-curve.sch"/>
        </rule>
    </pattern>

    <pattern
        id="samplingFeature-type-surface">
        <rule
            context="//sams:SF_SpatialSamplingFeature[ sam:type/@xlink:href='http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingSurface' ] ">
            <include
                href="./shape-surface.sch"/>
        </rule>
    </pattern>

    <pattern
        id="samplingFeature-type-solid">
        <rule
            context="//sams:SF_SpatialSamplingFeature[ sam:type/@xlink:href='http://www.opengis.net/def/samplingFeatureType/OGC-OM/2.0/SF_SamplingSolid' ] ">
            <include
                href="./shape-solid.sch"/>
        </rule>
    </pattern>

</schema>
