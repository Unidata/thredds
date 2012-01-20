<%@page contentType="text/xml"%><%--@page contentType="application/vnd.ogc.wms_xml"--%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8" standalone="no"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the Capabilities document in XML for WMS 1.1.1
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config)
         datasets   = collection of datasets to display in this Capabilities document (Collection<Dataset>)
         wmsBaseUrl = Base URL of this server (java.lang.String)
         supportedCrsCodes = List of Strings of supported Coordinate Reference System codes
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
         layerLimit = Maximum number of layers that can be requested simultaneously from this server (int)
         featureInfoFormats = Array of Strings representing MIME types of supported feature info formats
         legendWidth, legendHeight = size of the legend that will be returned from GetLegendGraphic
         paletteNames = Names of colour palettes that are supported by this server (Set<String>)
     --%>
<!DOCTYPE WMT_MS_Capabilities SYSTEM "http://schemas.opengis.net/wms/1.1.1/capabilities_1_1_1.dtd">
<WMT_MS_Capabilities
        version="1.1.1"
        updateSequence="${utils:dateTimeToISO8601(lastUpdate)}"
        xmlns:xlink="http://www.w3.org/1999/xlink">
    <!-- Service Metadata -->
    <Service>
        <!-- The WMT-defined name for this type of service -->
        <Name>OGC:WMS</Name>
        <!-- Human-readable title for pick lists -->
        <Title><c:out value="${config.title}"/></Title>
        <!-- Narrative description providing additional information -->
        <Abstract><c:out value="${config.serverAbstract}"/></Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <!-- Top-level web address of service or service provider. See also OnlineResource
        elements under <DCPType>. -->
        <OnlineResource xlink:type="simple" xlink:href="<c:out value="${config.serviceProviderUrl}"/>"/>
        <!-- Contact information -->
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson><c:out value="${config.contactName}"/></ContactPerson>
                <ContactOrganization><c:out value="${config.contactOrganization}"/></ContactOrganization>
            </ContactPersonPrimary>
            <ContactVoiceTelephone><c:out value="${config.contactTelephone}"/></ContactVoiceTelephone>
            <ContactElectronicMailAddress><c:out value="${config.contactEmail}"/></ContactElectronicMailAddress>
        </ContactInformation>
        <!-- Fees or access constraints imposed. -->
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
    </Service>
    <Capability>
        <Request>
            <GetCapabilities>
                <Format>application/vnd.ogc.wms_xml</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetCapabilities>
            <GetMap>
                <c:forEach var="mimeType" items="${supportedImageFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetMap>
            <GetFeatureInfo>
                <c:forEach var="mimeType" items="${featureInfoFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>" />
                        </Get>
                    </HTTP>
                </DCPType>
            </GetFeatureInfo>
        </Request>
        <Exception>
            <Format>application/vnd.ogc.se_xml</Format>
            <!--<Format>application/vnd.ogc.se_inimage</Format>
            <Format>application/vnd.ogc.se_blank</Format>-->
        </Exception>
        
        <Layer>
            <Title><c:out value="${config.title}"/></Title><%-- Use of c:out escapes XML --%>
            <c:forEach var="crsCode" items="${supportedCrsCodes}">
            <SRS>${crsCode}</SRS>
            </c:forEach>
            <c:forEach var="dataset" items="${datasets}">
            <c:if test="${dataset.ready}">
            <Layer>
                <Title><c:out value="${dataset.title}"/></Title>
                <c:forEach var="layer" items="${dataset.layers}">
                <Layer<c:if test="${layer.queryable}"> queryable="1"</c:if>>
                    <Name>${layer.name}</Name>
                    <Title><c:out value="${layer.title}"/></Title>
                    <Abstract><c:out value="${layer.layerAbstract}"/></Abstract>
                    <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
                    <LatLonBoundingBox minx="${bbox.westBoundLongitude}" maxx="${bbox.eastBoundLongitude}" miny="${bbox.southBoundLatitude}" maxy="${bbox.northBoundLatitude}"/>
                    <BoundingBox SRS="EPSG:4326" minx="${bbox.westBoundLongitude}" maxx="${bbox.eastBoundLongitude}" miny="${bbox.southBoundLatitude}" maxy="${bbox.northBoundLatitude}"/>
                    <c:if test="${not empty layer.elevationValues}"><Dimension name="elevation" units="${layer.elevationUnits}"/><!-- TODO: units correct? --></c:if>
                    <c:if test="${not empty layer.timeValues}"><Dimension name="time" units="${utils:getTimeAxisUnits(layer.chronology)}"/></c:if>
                    <c:if test="${not empty layer.elevationValues}">
                    <Extent name="elevation" default="${layer.defaultElevationValue}">
                        <%-- Print out the dimension values, comma separated, making sure
                             that there is no comma at the start or end of the list.  Note that
                             we can't use ${fn:join} because the z values are an array of doubles,
                             not strings. --%>
                        <c:forEach var="zval" items="${layer.elevationValues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
                    </Extent>
                    </c:if>
                    <c:set var="tvalues" value="${layer.timeValues}"/>
                    <c:if test="${not empty tvalues}">
                    <Extent name="time" multipleValues="1" current="1" default="${utils:dateTimeToISO8601(layer.defaultTimeValue)}">
                        <c:forEach var="tval" items="${tvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${utils:dateTimeToISO8601(tval)}</c:forEach>
                    </Extent>
                    </c:if>
                    <c:set var="styles" value="boxfill"/>
                    <c:if test="${utils:isVectorLayer(layer)}">
                        <c:set var="styles" value="vector,boxfill"/>
                    </c:if>
                    <c:forEach var="style" items="${styles}">
                    <c:forEach var="paletteName" items="${paletteNames}">
                    <Style>
                        <Name>${style}/${paletteName}</Name>
                        <Title>${style}/${paletteName}</Title>
                        <Abstract>${style} style, using the ${paletteName} palette</Abstract>
                        <LegendURL width="${legendWidth}" height="${legendHeight}">
                            <Format>image/png</Format>
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}?REQUEST=GetLegendGraphic&amp;LAYER=${layer.name}&amp;PALETTE=${paletteName}"/>
                        </LegendURL>
                    </Style>
                    </c:forEach>
                    </c:forEach>
                </Layer>
                </c:forEach> <%-- End loop through variables --%>
            </Layer>
            </c:if>
            </c:forEach>
        </Layer>
    </Capability>
</WMT_MS_Capabilities>