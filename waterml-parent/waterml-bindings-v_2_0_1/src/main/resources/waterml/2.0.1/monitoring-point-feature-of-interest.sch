<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/monitoring-point-feature-of-interest.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-feature-of-interest-monitoring-point"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
        This Schematron schema checks that the type of the observation result is correct. 
        
        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
    
        version="2.0.1" 
    -->

  <title>OGC WaterML2.0 observation validation</title>
  <p>Verifies that the om:featureOfInterest element contains a OGC WaterML2.0 monitoring point type.</p>
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>

  <xsl:import-schema schema-location="http://schemas.opengis.net/waterml/2.0/waterml2.xsd"
  namespace="http://www.opengis.net/waterml/2.0"/>
  
  <xsl:import-schema schema-location="http://schemas.opengis.net/gml/3.2.1/gml.xsd"
    namespace="http://www.opengis.net/gml/3.2"/>

  <pattern id="featureOfInterest">
    <title>Test requirement: /req/xsd-feature-of-interest-monitoring-point/featureOfInterest</title>
    <rule context="//om:OM_Observation/om:featureOfInterest">
      <assert test="schema-element(wml2:MonitoringPoint) | @xlink:href">The xml element om:featureOfInterest shall contain a subelement of wml2:MonitoringPoint, a
        member of its substitution group or a reference to a representation of the monitoring point using xlink.</assert>
    </rule>
  </pattern>
</schema>
