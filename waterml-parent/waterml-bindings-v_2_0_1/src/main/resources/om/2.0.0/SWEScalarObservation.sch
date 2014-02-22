<?xml version="1.0" encoding="UTF-8"?>
<schema
    fpi="http://schemas.opengis.net/om/2.0/complexObservation.sch"
    see="http://www.opengis.net/doc/IP/OMXML/2.0"
    xmlns="http://purl.oclc.org/dsdl/schematron"
    queryBinding="xslt2"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
        This Schematron schema checks that the type of the observation result is correct. 
        
        Observations and Measurements - XML Implementation is an OGC Standard.
        Copyright (c) [2010] Open Geospatial Consortium, Inc. All Rights Reserved.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/.
    -->

    <title>SWE scalar observation validation</title>
    <p>Verifies that all instances of OM_Observation or elements derived from OM_Observation (i.e. having an om:resultTime property) have a result that matches the pattern for
        SWEScalarObservations</p>
    <ns
        prefix="om"
        uri="http://www.opengis.net/om/2.0"/>
    <ns
        prefix="swe"
        uri="http://www.opengis.net/swe/2.0"/>
    <ns
        prefix="xsi"
        uri="http://www.w3.org/2001/XMLSchema-instance"/>
    <ns
        prefix="xlink"
        uri="http://www.w3.org/1999/xlink"/>

    <!--
    <xsl:import-schema schema-location="http://schemas.opengis.net/sweCommon/2.0/swe.xsd"/>
    -->
    <xsl:import-schema
        schema-location="../../sweCommon/2.0/swe.xsd"/>

    <pattern
        id="observation-type-complex">
        <rule
            context="//*[om:resultTime]">
            <include
                href="./result-SimpleComponent.sch"/>
        </rule>
    </pattern>

</schema>
