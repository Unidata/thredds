<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/tld/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the Capabilities document in XML.
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
<WMS_Capabilities
        version="1.3.0"
        <%-- TODO: do UpdateSequence properly --%>
        xmlns="http://www.opengis.net/wms"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd">
        
    <Service>
        <Name>WMS</Name>
        <Title><c:out value="${config.server.title}"/></Title>
        <Abstract><c:out value="${config.server.abstract}"/></Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.server.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <OnlineResource xlink:type="simple" xlink:href="<c:out value="${config.server.url}"/>"/> 
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson><c:out value="${config.contact.name}"/></ContactPerson> 
                <ContactOrganization><c:out value="${config.contact.org}"/></ContactOrganization> 
            </ContactPersonPrimary>
            <ContactVoiceTelephone><c:out value="${config.contact.tel}"/></ContactVoiceTelephone>
            <ContactElectronicMailAddress><c:out value="${config.contact.email}"/></ContactElectronicMailAddress> 
        </ContactInformation>
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
        <LayerLimit>${layerLimit}</LayerLimit>
        <MaxWidth>${config.server.maxImageWidth}</MaxWidth>
        <MaxHeight>${config.server.maxImageHeight}</MaxHeight>
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
            <c:if test="${config.server.allowFeatureInfo}">
            <GetFeatureInfo>
                <c:forEach var="mimeType" items="${featureInfoFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType><HTTP><Get><OnlineResource xlink:type="simple" xlink:href="<c:out value="${wmsBaseUrl}"/>"/></Get></HTTP></DCPType>
            </GetFeatureInfo>
            </c:if>
        </Request>
        <Exception>
            <Format>XML</Format>
        </Exception>
        <Layer>
            <Title><c:out value="${config.server.title}"/></Title>
            <c:forEach var="crsCode" items="${supportedCrsCodes}">
            <CRS>${crsCode}</CRS>
            </c:forEach>
            <Layer>
                <Title><c:out value="${datasetTitle}" /></Title>
                <c:forEach var="layer" items="${layers}">
                <Layer<c:if test="${config.server.allowFeatureInfo and layer.queryable}"> queryable="1"</c:if>>
                    <Name>${layer.layerName}</Name>
                    <Title><c:out value="${layer.title}"/></Title>
                    <Abstract><c:out value="${layer.abstract}"/></Abstract>
                    <c:set var="bbox" value="${layer.bbox}"/>
                    <EX_GeographicBoundingBox>
                        <westBoundLongitude>${bbox[0]}</westBoundLongitude>
                        <eastBoundLongitude>${bbox[2]}</eastBoundLongitude>
                        <southBoundLatitude>${bbox[1]}</southBoundLatitude>
                        <northBoundLatitude>${bbox[3]}</northBoundLatitude>
                    </EX_GeographicBoundingBox>
                    <BoundingBox CRS="CRS:84" minx="${bbox[0]}" maxx="${bbox[2]}" miny="${bbox[1]}" maxy="${bbox[3]}"/>
                    <c:if test="${layer.zaxisPresent}">
                    <Dimension name="elevation" units="${layer.zunits}" default="${layer.defaultZValue}">
                        <%-- Print out the dimension values, comma separated, making sure
                             that there is no comma at the start or end of the list.  Note that
                             we can't use ${fn:join} because the z values are an array of doubles,
                             not strings. --%>
                        <c:forEach var="zval" items="${layer.zvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
                    </Dimension>
                    </c:if>
                    <c:set var="tvalues" value="${layer.tvalues}"/>
                    <c:if test="${layer.taxisPresent}">
                    <Dimension name="time" units="ISO8601" multipleValues="true" current="true" default="${utils:dateTimeToISO8601(layer.defaultTValue)}">
                        <c:forEach var="tval" items="${tvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${utils:dateTimeToISO8601(tval)}</c:forEach>
                    </Dimension>
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
            </Layer><%-- End loop through datasets --%>
        </Layer>
    </Capability>
    
</WMS_Capabilities>
