<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/timeseries_observations.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-timeseries-observation"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
        This Schematron schema checks the restricts the OM_Observation type to be consistent 
        with a OGC WaterML2.0 Timeseries Observation. 

        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .

        version="2.0.1"        
    -->

  <title>OGC WaterML2.0 observation validation</title>
  <p>Verifies that the OM_Observation type is valid according to the core WaterML2 observation restrictions. Tests
    requirements from http://www.opengis.net/spec/waterml/2.0/req/xsd-timeseries-observation</p>
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>

  <xsl:import-schema schema-location="http://schemas.opengis.net/waterml/2.0/waterml2.xsd"
  namespace="http://www.opengis.net/waterml/2.0"/>
  
  <xsl:import-schema schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"
    namespace="http://www.opengis.net/gml/3.2"/>

  <pattern id="procedure">
    <title>Test requirement: /req/xsd-timeseries-observation/procedure</title>
    <rule context="//om:OM_Observation/om:procedure">
      <assert test="schema-element(wml2:ObservationProcess) | @xlink:href">The xml element om:procedure shall contain a subelement of wml2:ObservationProcess, a
        member of its substitution group or a reference to an external definition of the process using the xlink:href attribute.</assert>
    </rule>
  </pattern>
  <pattern id="metadata">
    <title>Test requirement: /req/xsd-timeseries-observation/metadata</title>
    <rule context="//om:OM_Observation/om:metadata">
      <assert test="schema-element(wml2:ObservationMetadata) | @xlink:href">The xml element om:metadata shall contain a subelement of wml2:ObservationMetadata, a
        member of its substitution group or a reference to an external definition of the process using the xlink:href attribute.</assert>
    </rule>
  </pattern>
  <pattern id="phenomenonTime">
    <title>Test requirement: /xsd-timeseries-observation/phenomenonTime </title>
    <rule context="//om:OM_Observation/om:phenomenonTime">
      <assert test="schema-element(gml:TimePeriod) | @xlink:href">The om:phenomenonTime element shall contain a 
        gml:TimePeriod element that represents the temporal extent of the timeseries result of the observation or a reference
       to such an element.</assert>
    </rule>
  </pattern>
</schema>
