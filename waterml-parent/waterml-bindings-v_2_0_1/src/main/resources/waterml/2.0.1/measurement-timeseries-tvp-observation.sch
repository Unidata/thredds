<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/interleaved_timeseries_observation.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-measurement-timeseries-tvp-observation"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
        This Schematron schema checks that the type of the observation result is correct. 
        
        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
        
        version="2.0.1"
    -->

  <title>OGC WaterML2.0 measurement timeseries (tvp) observation</title>
  <p>Verifies that the OM_Observation result type is valid according to the WaterML2 measurement timeseries (tvp) observation . Tests
    requirements from http://www.opengis.net/spec/waterml/2.0/req/xsd-measurement-timeseries-tvp-observation</p>
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>

  <xsl:import-schema schema-location="http://schemas.opengis.net/waterml/2.0/waterml2.xsd"
  namespace="http://www.opengis.net/waterml/2.0"/>
  
  <xsl:import-schema schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"
    namespace="http://www.opengis.net/gml/3.2"/>

  <pattern id="result">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp-observation/result </title>
    <rule context="//om:OM_Observation/om:result">
      <assert test="schema-element(wml2:MeasurementTimeseries)">result must contain an element in the substitution group headed by wml2:MeasurementTimeseries</assert>
    </rule>
  </pattern>
  
</schema>
