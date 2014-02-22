<?xml version="1.0" encoding="UTF-8"?>
<schema
    fpi="http://schemas.opengis.net/om/2.0/resultTypeConsistent.sch"
    see="http://www.opengis.net/doc/IP/OMXML/2.0"
    xmlns="http://purl.oclc.org/dsdl/schematron"
    queryBinding="xslt2"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    
    <!--
        Observations and Measurements - XML Implementation is an OGC Standard.
        Copyright (c) [2010] Open Geospatial Consortium, Inc. All Rights Reserved.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/. 
    -->

    <title>Observation validation</title>
    <p>This Schematron schema checks that the content model of each observation result is consistent
        with the type of the observation.</p>
    <ns
        prefix="gml"
        uri="http://www.opengis.net/gml/3.2"/>
    <ns
        prefix="swe"
        uri="http://www.opengis.net/swe/2.0"/>
    <ns
        prefix="om"
        uri="http://www.opengis.net/om/2.0"/>
    <ns
        prefix="xsi"
        uri="http://www.w3.org/2001/XMLSchema-instance"/>
    <ns
        prefix="xlink"
        uri="http://www.w3.org/1999/xlink"/>

    <xsl:import-schema
        schema-location="http://schemas.opengis.net/om/2.0/observation.xsd"/>
    <xsl:import-schema
        schema-location="http://schemas.opengis.net/sweCommon/2.0/swe.xsd"/>
    <xsl:import-schema
        schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"/>

    <pattern
        id="observation-type-measurement">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement' ] ">
            <include
                href="./result-measure.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-category">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CategoryObservation' ] ">
            <include
                href="./result-category.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-count">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_CountObservation' ]">
            <include
                href="./result-integer.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-truth">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TruthObservation' ]">
            <include
                href="./result-boolean.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-complex">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation' ] ">
            <include
                href="./result-DataRecord.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-geometry">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_GeometryObservation' ]">
            <include
                href="./result-geometry.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-temporal">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_TemporalObservation' ]">
            <include
                href="./result-temporal.sch"/>
        </rule>
    </pattern>

    <pattern
        id="observation-type-swe-simple">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/SWEScalarObservation' ] ">
            <include
                href="./result-SimpleComponent.sch"/>
        </rule>
    </pattern>
    
    <pattern
        id="observation-type-swe-array">
        <rule
            context="//om:OM_Observation[ om:type/@xlink:href='http://www.opengis.net/def/observationType/OGC-OM/2.0/SWEArrayObservation' ] ">
            <include
                href="./result-DataArray.sch"/>
        </rule>
    </pattern>
    
</schema>
