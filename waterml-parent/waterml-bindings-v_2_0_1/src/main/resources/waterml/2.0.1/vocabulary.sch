<?xml version="1.0" encoding="UTF-8"?>
<schema fpi="http://schemas.opengis.net/waterml/2.0/xml-rules.sch" 
  see="http://www.opengis.net/spec/waterml/2.0/req/xsd-xml-rules"
  xmlns="http://purl.oclc.org/dsdl/schematron" 
  xmlns:sch="http://www.ascc.net/xml/schematron"
  queryBinding="xslt2" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  schemaVersion="ISO19757-3">
  <!--
        This schematron schema checks the XML vocabulary requirements of OGC WaterML2.0, as specified
        in the requirements class: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references but
        also within the specification document. 
        
        OGC WaterML 2.0 is an OGC Standard.
        Copyright (c) 2012 Open Geospatial Consortium.
        To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
        
        version="2.0.1"
  -->

  <title>OGC WaterML2.0 Vocabulary tests</title>
  <p>This schematron schema checks the XML encoding requirements of OGC WaterML2.0, as specified
    in the requirements class: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references. 
    It further checks that the correct codes are available within the OGC repository. </p>
  
  <phase id="Required" >                
    <active pattern="InterpolationType"/>
    <active pattern="processType"/>
    <active pattern="processType"/>
  </phase>
  
  <phase id="Suggested" >                
    <active pattern="medium"/>
  </phase>
  
  <ns prefix="wml2" uri="http://www.opengis.net/waterml/2.0"/>
  <ns prefix="om" uri="http://www.opengis.net/om/2.0"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <ns prefix="swe" uri="http://www.opengis.net/swe/2.0"/>
  <ns prefix="rdf" uri="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
  
  <!-- the logic for sch:report is opposite to assert; you report if the statement is true --> 
  <pattern id="InterpolationType">
    <title>Test recommendation: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references</title>
    <rule context="//wml2:interpolationType">
      <assert test="starts-with(@xlink:href, 'http://www.opengis.net/def/waterml/2.0/interpolationType')  "> Interpolation Type URI is not WaterML2</assert>
      <assert test="document(concat(@xlink:href,'.rdf'))//rdf:RDF/rdf:Description[@rdf:about=@xlink:href]">Interpolation Type  resource (<iso:value-of 
        select="@xlink:href"/>) is not in the OGC repository.</assert>
    </rule>
  </pattern>
  
  <pattern id="processType">
    <title>Test recommendation: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references</title>
    <rule context="//wml2:processType">
      <report test="not(starts-with(@xlink:href, 'http://www.opengis.net/def/waterml/2.0/processType') ) "> processType URI is not WaterML2</report>
      <assert test="document(concat(@xlink:href,'.rdf'))//rdf:RDF/rdf:Description[@rdf:about=@xlink:href]">processType resource (<iso:value-of 
        select="@xlink:href"/>) is not in the OGC repository.</assert>
    </rule>  
  </pattern>
  
  <pattern id="quality">
    <title>Test recommendation: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references</title>
    <rule context="//wml2:quality">
      <assert test="starts-with(@xlink:href, 'http://www.opengis.net/def/waterml/2.0/quality/')  "> quality URI is not WaterML2</assert>
      <assert test="document(concat(@xlink:href,'.rdf'))//rdf:RDF/rdf:Description[@rdf:about=@xlink:href]">quality resource (<iso:value-of 
        select="@xlink:href"/>)is not in the OGC repository.</assert>
    </rule>
  </pattern>
  
  <pattern id="medium">
    <title>Test recommendation: http://www.opengis.net/spec/waterml/2.0/rec/xsd-xml-rules/vocabulary-references</title>
    <rule context="//wml2:sampledMedium">
      <assert test="starts-with(@xlink:href, 'http://www.opengis.net/def/waterml/2.0/medium')  "> Sample Medium URI is not WaterML2</assert>
      <assert test="document(concat(@xlink:href,'.rdf'))//rdf:RDF/rdf:Description[@rdf:about=@xlink:href]">Sample Medium resource (<iso:value-of 
        select="@xlink:href"/>) is not in the OGC repository.</assert>
    </rule>
  </pattern>
  
  
</schema>
