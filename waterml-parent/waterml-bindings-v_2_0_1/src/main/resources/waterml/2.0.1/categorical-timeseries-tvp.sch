<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/timeseries_category.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-category-timeseries"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
        This Schematron schema checks the type of the value of the time series is a measurement. 
        
        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
    
        version="2.0.1"
    -->

  <title>OGC WaterML2.0 category time series validation</title>
  <p>Verifies the value type of the time series is a catgory (string with a codespace). This is a test of 
    the requirement: http://www.opengis.net/spec/waterml/2.0/req/xsd-category-timeseries-tvp/value</p>
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>

  <xsl:import-schema schema-location="http://schemas.opengis.net/waterml/2.0/waterml2.xsd"
  namespace="http://www.opengis.net/waterml/2.0"/>
  
  <xsl:import-schema schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"
    namespace="http://www.opengis.net/gml/3.2"/>
  
  <pattern id="point-type">
    <title>Test requirement: /req/xsd-categorical-timeseries-tvp/value-category</title>
    <rule context="//wml2:CategoricalTimeseries/wml2:point"> 
      <assert test="schema-element(wml2:CategoricalTVP)">The time series points must be of type category</assert>
    </rule>
  </pattern>
  
  <pattern id="default-point-metadata-type">
    <title>Test default point metadata type</title>
    <rule context="//wml2:CategoricalTimeseries/wml2:defaultPointMetadata"> 
      <assert test="schema-element(wml2:DefaultTVPCategoricalMetadata)">The default metadata
        for a point must use the category specific type. </assert>
    </rule>
  </pattern>
  
  <pattern id="timeseries-point-metadata-type">
    <title>Test point metadata type</title>
    <rule context="//wml2:point/wml2:CategoricalTVP/wml2:metadata"> 
      <assert test="schema-element(wml2:TVPMetadata)">The time series 
        metadata type be the base metadata type. I.e. a TVPMedata element</assert>
    </rule>
  </pattern>
  
  <!-- The element check checks for specific element name rather than any element 
    in the substitution group (which schema-element does) --> 
  <pattern id="timeseries-metadata-type">
    <title>Test metadata type</title>
    <rule context="//wml2:CategoricalTimeseries/wml2:metadata"> 
      <assert test="element(wml2:TimeseriesMetadata)">The timeseries metadata
        must be consistent with the categorical series. I.e. TimeseriesMetadata</assert>
    </rule>
  </pattern>
  
</schema>
