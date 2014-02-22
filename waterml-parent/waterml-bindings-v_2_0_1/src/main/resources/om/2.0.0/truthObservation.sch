<?xml version="1.0" encoding="UTF-8"?>
<schema
    fpi="http://schemas.opengis.net/om/2.0/truthObservation.sch"
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

    <title>Truth observation validation</title>
    <p>Verifies that all instances of OM_Observation or elements derived from OM_Observation (i.e. having an om:resultTime property) have a result that matches the pattern for TruthObservations</p>
    <ns
        prefix="om"
        uri="http://www.opengis.net/om/2.0"/>
    <ns
        prefix="xs"
        uri="http://www.w3.org/2001/XMLSchema"/>
    <ns
        prefix="xsi"
        uri="http://www.w3.org/2001/XMLSchema-instance"/>
    <ns
        prefix="xlink"
        uri="http://www.w3.org/1999/xlink"/>

    <pattern
        id="observation-type-truth">
        <rule
            context="//*[om:resultTime]">
            <include
                href="./result-boolean.sch"/>
        </rule>
    </pattern>

</schema>
