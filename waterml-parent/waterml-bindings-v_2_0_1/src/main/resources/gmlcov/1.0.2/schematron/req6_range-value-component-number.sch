<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Requirement 6 / partial</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:ns uri="http://www.opengis.net/gmlcov/1.0/gml" prefix="gmlcov"/>
    <sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>
    <sch:ns uri="http://www.opengis.net/gml/3.2" prefix="gml"/>
    <sch:pattern>
        <sch:title>Req 6</sch:title>
        <sch:rule context="*[local-name()='GridCoverage']">
            <sch:let name="band" value="count(..//*[local-name()='rangeType']//*[local-name()='field'][child::*[local-name()!='DataArray' and local-name()!='DataRecord']])"/>
            <sch:assert test="count(tokenize(substring-before(normalize-space(//*[local-name()='DataBlock']//*[local-name()='tupleList']), ' '),',')) = $band">
                All range values contained in the range set of a coverage shall have the number of components as indicated in the range type.
            </sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>
