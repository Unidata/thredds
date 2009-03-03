<%@page contentType="application/vnd.ogc.wms_xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8" standalone="no"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/tld/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
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
        xmlns:xlink="http://www.w3.org/1999/xlink"><%-- TODO: do UpdateSequence properly --%>
    <!-- Service Metadata -->
    <Service>
        <!-- The WMT-defined name for this type of service -->
        <Name>OGC:WMS</Name>
        <!-- Human-readable title for pick lists -->
        <Title>${config.server.title}</Title>
        <!-- Narrative description providing additional information -->
        <Abstract>${config.server.abstract}</Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.server.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <!-- Top-level web address of service or service provider. See also OnlineResource
        elements under <DCPType>. -->
        <OnlineResource xlink:type="simple" xlink:href="${config.server.url}" />
        <!-- Contact information -->
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson>${config.contact.name}</ContactPerson>
                <ContactOrganization>${config.contact.org}</ContactOrganization>
            </ContactPersonPrimary>
            <ContactVoiceTelephone>${config.contact.tel}</ContactVoiceTelephone>
            <ContactElectronicMailAddress>${config.contact.email}</ContactElectronicMailAddress>
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
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}" />
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
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}" />
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
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}" />
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
            <Title>${config.server.title}</Title>
            <c:forEach var="crsCode" items="${supportedCrsCodes}">
            <SRS>${crsCode}</SRS>
            </c:forEach>
            <Layer>
                <Title>${dataset.title}</Title>
                <c:forEach var="layer" items="${layers}">
                <Layer<c:if test="${config.server.allowFeatureInfo} and ${layer.queryable}"> queryable="1"</c:if>>
                    <Name>${layer.layerName}</Name>
                    <Title>${layer.title}</Title>
                    <Abstract>${layer.abstract}</Abstract>
                    <c:set var="bbox" value="${layer.bbox}"/>
                    <LatLonBoundingBox minx="${bbox[0]}" maxx="${bbox[2]}" miny="${bbox[1]}" maxy="${bbox[3]}"/>
                    <BoundingBox SRS="EPSG:4326" minx="${bbox[0]}" maxx="${bbox[2]}" miny="${bbox[1]}" maxy="${bbox[3]}"/>
                    <c:if test="${layer.zaxisPresent}"><Dimension name="elevation" units="${layer.zunits}"/><!-- TODO: units correct? --></c:if>
                    <c:if test="${layer.taxisPresent}"><Dimension name="time" units="ISO8601"/></c:if>
                    <c:if test="${layer.zaxisPresent}">
                    <Extent name="elevation" default="${layer.defaultZValue}">
                        <%-- Print out the dimension values, comma separated, making sure
                             that there is no comma at the start or end of the list.  Note that
                             we can't use ${fn:join} because the z values are an array of doubles,
                             not strings. --%>
                        <c:forEach var="zval" items="${layer.zvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
                    </Extent>
                    </c:if>                                                              
                    <c:set var="tvalues" value="${layer.tvalues}"/>
                    <c:if test="${layer.taxisPresent}">
                    <Extent name="time" multipleValues="1" current="1" default="${utils:dateTimeToISO8601(layer.defaultTValue)}">
                        <c:forEach var="tval" items="${tvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${utils:dateTimeToISO8601(tval)}</c:forEach>
                    </Extent>
                    </c:if>
                    <c:forEach var="style" items="${layer.supportedStyles}">
                    <c:forEach var="paletteName" items="${paletteNames}">
                    <Style>
                        <Name>${style}/${paletteName}</Name>
                        <Title>${style}/${paletteName}</Title>
                        <Abstract>${style} style, using the ${paletteName} palette</Abstract>
                        <LegendURL width="${legendWidth}" height="${legendHeight}">
                            <Format>image/png</Format>
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}?REQUEST=GetLegendGraphic&amp;LAYER=${layer.layerName}&amp;PALETTE=${paletteName}"/>
                        </LegendURL>
                    </Style>
                    </c:forEach>
                    </c:forEach>
                </Layer>
                </c:forEach> <%-- End loop through variables --%>
            </Layer>
        </Layer>
    </Capability>
</WMT_MS_Capabilities>