<%@tag description="Displays all the timesteps for a layer as separate overlays" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wms2kml" prefix="wms2kml"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="wmsUtils"%>
<%@attribute name="tiledLayer" required="true" type="uk.ac.rdg.resc.ncwms.controller.TiledLayer" description="Layer object with tile information"%>
<%@attribute name="baseURL" required="true" description="URL to use as a base for any callbacks to this server, e.g. in NetworkLinks"%>
<%@attribute name="elevation" required="false" description="elevation value"%>
<c:set var="layer" value="${tiledLayer.layer}"/>
<c:choose>
    <c:when test="${empty layer.timesteps}">
        <wms2kml:regionBasedOverlay tiledLayer="${tiledLayer}" elevation="${elevation}" baseURL="${baseURL}"/>
    </c:when>
    <%-- If the layer has a time dimension we create a folder for each timestep --%>
    <c:otherwise>
        <c:forEach items="${layer.timesteps}" var="timestep">
            <c:set var="isoTime" value="${wmsUtils:dateToISO8601(timestep.date)}"/>
            <Folder>
                <name>${isoTime}</name>
                <wms2kml:regionBasedOverlay tiledLayer="${tiledLayer}" elevation="${elevation}" time="${isoTime}" baseURL="${baseURL}"/>
            </Folder>
        </c:forEach>
    </c:otherwise>
</c:choose>
