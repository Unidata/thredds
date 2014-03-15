<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/measurement-timeseries-tvp.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-measurement-timeseries-tvp"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
    This Schematron schema checks the timeseries and points within the series are 
    all consistent with the interleaved measurement timeseries requirements class. 
    
    OGC WaterML 2.0 is an OGC Standard.
    Copyright (c) 2012 Open Geospatial Consortium.
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .

    version="2.0.1"
  -->
  
  <title>OGC WaterML2.0 measurement time series validation</title>
  <p>Verifies the timeseries is valid according the measure time series class, 
    http://www.opengis.net/spec/waterml/2.0/req/xsd-measurement-timeseries-tvp</p>
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
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/value-measure</title>
    <rule context="//wml2:MeasurementTimeseries/wml2:point"> 
      <assert test="schema-element(wml2:MeasurementTVP)">The time series points must be of type measurement</assert>
    </rule>
  </pattern>
  
  <pattern id="interpolation-type">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/interpolation-type</title>
    <rule context="//wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP">
      <assert test="wml2:metadata/wml2:TVPMeasurementMetadata/wml2:interpolationType or 
        ../../wml2:defaultPointMetadata/wml2:DefaultTVPMeasurementMetadata/wml2:interpolationType">
        The interpolation type of a point must be set explicitly or through the default point metadata.  
      </assert>
    </rule>
  </pattern>
  
  <pattern id="value-measure-unit-of-measure">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/unit-of-measure</title>
    <rule context="//wml2:point/wml2:MeasurementTVP/wml2:value"> 
      <assert test="@code or ../../../wml2:defaultPointMetadata/wml2:DefaultTVPMeasurementMetadata/wml2:uom[@code]">A unit of measure
        must be supplied either through the default point metadata or by explicit attribute on the value.</assert>
    </rule>
  </pattern>
  
  <pattern id="default-point-metadata-type">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/point-metadata</title>
    <rule context="//wml2:MeasurementTimeseries/wml2:defaultPointMetadata"> 
      <assert test="schema-element(wml2:DefaultTVPMeasurementMetadata)">The default metadata
        for a point must use the measurement specific type. </assert>
    </rule>
  </pattern>
  
  <pattern id="point-metadata-type">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/point-metadata</title>
    <rule context="//wml2:point/wml2:MeasurementTVP/wml2:metadata"> 
      <assert test="schema-element(wml2:TVPMeasurementMetadata)">The point metadata for each point
        must be consistent with the measurement series. I.e. a TVPMeasurementMetadata</assert>
    </rule>
  </pattern>
  
  <pattern id="timeseries-metadata-type">
    <title>Test requirement: /req/xsd-measurement-timeseries-tvp/timeseries-metadata</title>
    <rule context="//wml2:MeasurementTimeseries/wml2:metadata"> 
      <assert test="schema-element(wml2:MeasurementTimeseriesMetadata)">The timeseries metadata
        must be consistent with the measurement series. I.e. MeasurementTimeseriesMetadata</assert>
    </rule>
  </pattern>
  
</schema>
