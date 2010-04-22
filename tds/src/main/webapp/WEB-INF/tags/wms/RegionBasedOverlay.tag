<%@tag description="Contains the top level of a region-based overlay" pageEncoding="UTF-8"%>
<%-- Thanks to Jason Birch, http://www.jasonbirch.com/nodes/2006/06/13/21/wms-on-steroids-kml-21-regions-application/ --%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@attribute name="tiledLayer" required="true" type="uk.ac.rdg.resc.ncwms.controller.TiledLayer" description="Layer object with tile information"%>
<%@attribute name="baseURL" required="true" description="URL to use as a base for any callbacks to this server, e.g. in NetworkLinks"%>
<%@attribute name="elevation" required="false" description="Elevation value"%>
<%@attribute name="time" required="false" description="Time value in ISO8601 format"%>
<c:set var="layer" value="${tiledLayer.layer}"/>
<c:set var="href" value="${baseURL}?REQUEST=GetKMLRegion"/>
<c:if test="${not empty elevation}">
    <c:set var="href" value="${href}&amp;ELEVATION=${elevation}"/>
</c:if>
<c:if test="${not empty time}">
    <c:set var="href" value="${href}&amp;TIME=${time}"/>
</c:if>
<%-- One link for each tile associated with this Layer - see WmsController.getKML() --%>
<c:forEach var="bbox" items="${tiledLayer.tiles}">
    <NetworkLink>
        <visibility>1</visibility> 
        <Region>
            <LatLonAltBox>
                <north>${bbox[3]}</north>
                <south>${bbox[1]}</south>
                <east>${bbox[2]}</east>
                <west>${bbox[0]}</west>
            </LatLonAltBox>
            <Lod>
                <minLodPixels>380</minLodPixels> 
                <maxLodPixels>-1</maxLodPixels> 
            </Lod>
        </Region>
        <Link>
            <viewRefreshMode>onRegion</viewRefreshMode>
            <href>${href}&amp;LAYER=${layer.layerName}&amp;DBOX=${bbox[0]},${bbox[1]},${bbox[2]},${bbox[3]}</href> 
        </Link>
        <c:if test="${not empty time}">
            <TimeStamp>
                <when>${time}</when>
            </TimeStamp>
        </c:if>
    </NetworkLink>
</c:forEach>
