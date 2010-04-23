<%@page contentType="text/xml"%><%--"application/vnd.google-earth.kml+xml"--%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
Displays a region and provides network links for sub-regions.

Data passed in:
   wmsBaseUrl: Base of the URL to this WMS 
   layer: Layer object
   dbox: Bounding box for this region (double[4])
   elevation: (String) can be null
   time: (String) can be null
   size: (int) Number of pixels along one side of the image
   regionDBoxes: Bounding boxes for each sub-region (double[4][4])
--%>
<c:set var="baseURL" value="${wmsBaseUrl}?"/>
<c:if test="${not empty elevation}">
    <c:set var="baseURL" value="${baseURL}ELEVATION=${elevation}&amp;"/>
</c:if>
<c:if test="${not empty time}">
    <c:set var="baseURL" value="${baseURL}TIME=${time}&amp;"/>
</c:if>
<kml xmlns="http://earth.google.com/kml/2.2"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://earth.google.com/kml/2.2 http://code.google.com/apis/kml/schema/kml22beta.xsd">
    <Document>
        <GroundOverlay>
            <drawOrder>1</drawOrder>
            <Icon>
                <%-- The GetMap request --%>
                <href>${baseURL}LAYERS=${layer.layerName}&amp;STYLES=&amp;REQUEST=GetMap&amp;VERSION=1.3.0&amp;CRS=CRS:84&amp;WIDTH=${size}&amp;HEIGHT=${size}&amp;FORMAT=image/png&amp;TRANSPARENT=true&amp;BBOX=${dbox[0]},${dbox[1]},${dbox[2]},${dbox[3]}</href>
            </Icon>
            <LatLonBox>
                <north>${dbox[3]}</north>
                <south>${dbox[1]}</south>
                <east>${dbox[2]}</east>
                <west>${dbox[0]}</west>
            </LatLonBox>
        </GroundOverlay>
        <%-- Now for the NetworkLinks covering all sub-regions --%>
        <c:forEach items="${regionDBoxes}" var="regionDBox">
            <NetworkLink>
                <visibility>1</visibility>
                <Region>
                    <LatLonAltBox>
                        <north>${regionDBox[3]}</north>
                        <south>${regionDBox[1]}</south>
                        <east>${regionDBox[2]}</east>
                        <west>${regionDBox[0]}</west>
                    </LatLonAltBox>
                    <Lod>
                        <minLodPixels>380</minLodPixels>
                        <maxLodPixels>-1</maxLodPixels>
                    </Lod>
                </Region>
                <Link>
                    <viewRefreshMode>onRegion</viewRefreshMode>
                    <href>${baseURL}LAYER=${layer.layerName}&amp;REQUEST=GetKMLRegion&amp;DBOX=${regionDBox[0]},${regionDBox[1]},${regionDBox[2]},${regionDBox[3]}</href>
                </Link>
            </NetworkLink>
        </c:forEach>
    </Document>
</kml>
