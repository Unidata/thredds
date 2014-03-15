<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/xml-rules.sch" see="http://www.opengis.net/spec/waterml/2.0/req/xsd-xml-rules"
  xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!--
        This schematron schema checks the XML encoding requirements of OGC WaterML2.0, as specified
        in the requirements class: http://www.opengis.net/spec/waterml/2.0/req/xsd-xml-rules
        
        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
    
        version="2.0.1"
    -->

  <title>OGC WaterML2.0 XML encoding tests</title>
  <p>This schematron schema checks the XML encoding requirements of OGC WaterML2.0, as specified
    in the requirements class: http://www.opengis.net/spec/waterml/2.0/req/xsd-xml-rules</p>
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <ns prefix="swe" uri="http://www.opengis.net/swe/2.0"/>
  
  <!-- the logic for sch:report is opposite to assert; you report if the statement is true --> 
  <pattern id="result">
    <title>Test recommendation: /req/xsd-xml-rules/xlink-title</title>
    <rule context="*[@xlink:href]">
      <report test="not(starts-with(@xlink:href, '#') ) and not(@xlink:title)">If an xlink:href is used to reference a controlled vocabulary item, the element should encode the
        xlink:title attribute with a text description of the referenced item.If an xlink:href is used to reference a controlled vocabulary item, the
        element should encode the xlink:title attribute with a text description of the referenced item.</report>
    </rule>
  </pattern>
  
  <pattern id="swe-types">
    <title>Test requirement: /req/xsd-xml-rules/swe-types</title>
    <rule context="swe:Category">
      <assert test="not(swe:quality) and not(swe:nilValues) and not(swe:constraint)">When using the SWE Common types, the following elements shall not be used: 
        swe:quality (AbstractSimpleComponentType), swe:nilValues (AbstractSimpleComponentType), swe:constraint.</assert>
    </rule>
    <rule context="swe:QuantityType">
      <assert test="not(swe:quality) and not(swe:nilValues) and not(swe:constraint)">When using the SWE Common types, the following elements shall not be used: 
        swe:quality (AbstractSimpleComponentType), swe:nilValues (AbstractSimpleComponentType), swe:constraint.
       </assert>
    </rule>
    <rule context="swe:Quantity">
      <assert test="not(swe:quality) and not(swe:nilValues) and not(swe:constraint)">When using the SWE Common types, the following elements shall not be used: 
        swe:quality (AbstractSimpleComponentType), swe:nilValues (AbstractSimpleComponentType), swe:constraint.
       </assert>
    </rule>
    <!-- Add check for non-existence of attributes optional and updatable -->
  </pattern>
  
  <pattern id="validLocalXlink">
    <title>Test recommendation: /req/xsd-xml-rules/xlink-valid-local-reference</title>
    <rule context="*[@xlink:href]">
      <!-- removed namespace just use local  namespace-uri()='http://www.opengis.net/gml/3.2' and   -->
       <report test="starts-with(@xlink:href, '#') and not(//@*[local-name()='id' ]=substring(@xlink:href, 2))">If an xlink:href is a local reference
        the reference element must exist. </report>
    </rule>
  </pattern>
  
 <!-- timeseries.sch  includes an xlink title for observerd property and feature of interest -->
  
</schema>
