<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the Capabilities document in XML.
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.wms.ServerConfig)
         datasets   = collection of datasets to display in this Capabilities document (Collection<uk.ac.rdg.resc.ncwms.wms.Dataset>)
         lastUpdate = Last update time of the dataset(s) displayed in this document (org.joda.time.DateTime)
         wmsBaseUrl = Base URL of this server (java.lang.String)
         supportedCrsCodes = List of Strings of supported Coordinate Reference System codes
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
         layerLimit = Maximum number of layers that can be requested simultaneously from this server (int)
         featureInfoFormats = Array of Strings representing MIME types of supported feature info formats
         legendWidth, legendHeight = size of the legend that will be returned from GetLegendGraphic
         paletteNames = Names of colour palettes that are supported by this server (Set<String>)
     --%>
<WMS_Capabilities
        version="1.3.0"
        updateSequence="${utils:dateTimeToISO8601(lastUpdate)}"
        xmlns="http://www.opengis.net/wms"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd">
        
    <Service>
        <Name>WMS</Name>
        <Title><c:out value="${config.title}"/></Title>
        <Abstract><c:out value="${config.abstract}"/></Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <OnlineResource xlink:type="simple" xlink:href="<c:out value="${config.serviceProviderUrl}"/>"/>
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson><c:out value="${config.contactName}"/></ContactPerson>
                <ContactOrganization><c:out value="${config.contactOrganization}"/></ContactOrganization>
            </ContactPersonPrimary>
            <ContactVoiceTelephone><c:out value="${config.contactTelephone}"/></ContactVoiceTelephone>
            <ContactElectronicMailAddress><c:out value="${config.contactEmail}"/></ContactElectronicMailAddress>
        </ContactInformation>
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
        <LayerLimit>${layerLimit}</LayerLimit>
        <MaxWidth>${config.maxImageWidth}</MaxWidth>
        <MaxHeight>${config.maxImageHeight}</MaxHeight>
    </Service>
    <Capability>
        <Request>
            <GetCapabilities>
                <Format>text/xml</Format>
                <DCPType><HTTP><Get><OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>"/></Get></HTTP></DCPType>
            </GetCapabilities>
            <GetMap>
                <c:forEach var="mimeType" items="${supportedImageFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType><HTTP><Get><OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>"/></Get></HTTP></DCPType>
            </GetMap>
            <GetFeatureInfo>
                <c:forEach var="mimeType" items="${featureInfoFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType><HTTP><Get><OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>"/></Get></HTTP></DCPType>
            </GetFeatureInfo>
        </Request>
        <Exception>
            <Format>XML</Format>
        </Exception>
        <Layer>
            <Title><c:out value="${config.title}"/></Title><%-- Use of c:out escapes XML --%>
            <c:forEach var="crsCode" items="${supportedCrsCodes}">
            <CRS>${crsCode}</CRS>
            </c:forEach>
            <c:forEach var="dataset" items="${datasets}">
            <c:if test="${dataset.ready}">
            <Layer>
                <Title><c:out value="${dataset.title}"/></Title>
                <c:forEach var="layer" items="${dataset.layers}">
                <Layer<c:if test="${layer.queryable}"> queryable="1"</c:if>>
                    <Name>${layer.name}</Name>
                    <Title><c:out value="${layer.title}"/></Title>
                    <Abstract><c:out value="${layer.abstract}"/></Abstract>
                    <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
                    <EX_GeographicBoundingBox>
                        <westBoundLongitude>${bbox.westBoundLongitude}</westBoundLongitude>
                        <eastBoundLongitude>${bbox.eastBoundLongitude}</eastBoundLongitude>
                        <southBoundLatitude>${bbox.southBoundLatitude}</southBoundLatitude>
                        <northBoundLatitude>${bbox.northBoundLatitude}</northBoundLatitude>
                    </EX_GeographicBoundingBox>
                    <BoundingBox CRS="CRS:84" minx="${bbox.westBoundLongitude}" maxx="${bbox.eastBoundLongitude}" miny="${bbox.southBoundLatitude}" maxy="${bbox.northBoundLatitude}"/>
                    <c:if test="${not empty layer.elevationValues}">
                    <Dimension name="elevation" units="${layer.elevationUnits}" default="${layer.defaultElevationValue}">
                        <%-- Print out the dimension values, comma separated, making sure
                             that there is no comma at the start or end of the list.  Note that
                             we can't use ${fn:join} because the z values are an array of doubles,
                             not strings. --%>
                        <c:forEach var="zval" items="${layer.elevationValues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
                    </Dimension>
                    </c:if>
                    <c:set var="tvalues" value="${layer.timeValues}"/>
                    <c:if test="${not empty tvalues}">
                        <Dimension name="time" units="${utils:getTimeAxisUnits(layer.chronology)}" multipleValues="true" current="true" default="${utils:dateTimeToISO8601(layer.defaultTimeValue)}">
                        <c:forEach var="tval" items="${tvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${utils:dateTimeToISO8601(tval)}</c:forEach>
                        </Dimension>
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
            </c:if> <%-- End if dataset is ready --%>
            </c:forEach> <%-- End loop through datasets --%>
        </Layer>
    </Capability>
    
</WMS_Capabilities>