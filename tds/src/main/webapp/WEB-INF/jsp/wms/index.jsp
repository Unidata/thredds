<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Front page of the ncWMS server.
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.wms.ServerConfig)
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
--%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>${config.title}</title>
    </head>
    <body>

    <h1>${config.title}</h1>

    <p><a href="godiva2.html">Godiva2 interface</a></p>
    <c:choose>
        <c:when test="${config.allowsGlobalCapabilities}">
            <p><a href="wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.3.0">WMS 1.3.0 Capabilities</a></p>
            <p><a href="wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.1.1">WMS 1.1.1 Capabilities</a></p>
        </c:when>
        <c:otherwise>
            <em>The system administrator has disabled the generation of Capabilities
            documents that include all datasets on this server.</em>
        </c:otherwise>
    </c:choose>
    <p><a href="admin/">Admin interface (requires login)</a></p>
    
    <h2>Datasets:</h2>
    <!-- Print a GetMap link for every variable we have, in each available image format -->
    <table border="1">
        <tr>
            <th>Dataset</th>
            <th>Godiva2 links</th>
            <c:forEach var="mimeType" items="${supportedImageFormats}">
                <th>Test GetMap: ${mimeType}</th>
            </c:forEach>
            <th>FeatureInfo</th>
        </tr>
        <c:set var="allDatasets" value="${config.allDatasets}"/>
        <c:if test="${empty allDatasets}">
            This server contains no datasets, or it is not possible to list all
            the datasets on this server.
        </c:if>
        <c:forEach var="datasetEntry" items="${allDatasets}">
        <c:set var="dataset" value="${datasetEntry.value}"/>
        <c:if test="${dataset.ready}">
        <tr>
            <th>
                ${dataset.title}<br />
                <a href="wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.3.0&amp;DATASET=${dataset.id}">WMS 1.3.0</a><br />
                <a href="wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.1.1&amp;DATASET=${dataset.id}">WMS 1.1.1</a><br />
                <a href="admin/editVariables?dataset=${dataset.id}">Edit variables</a> (requires login)
            </th>
            <c:set var="layers" value="${dataset.layers}"/>
            <td>
                <!-- Direct links to the Godiva2 site -->
                <c:forEach var="layer" items="${layers}">
                    <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
                    <a href="godiva2.html?layer=${layer.name}&amp;bbox=${bbox.westBoundLongitude},${bbox.southBoundLatitude},${bbox.eastBoundLongitude},${bbox.northBoundLatitude}">${layer.title}</a><br />
                </c:forEach>
            </td>
            <c:forEach var="mimeType" items="${supportedImageFormats}">
                <c:set var="transparent" value="true"/>
                <c:if test="${mimeType == 'image/jpeg'}">
                    <c:set var="transparent" value="false"/>
                </c:if>
                <td>
                    <c:forEach var="layer" items="${layers}">
                    <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
                    <a href="wms?REQUEST=GetMap&amp;VERSION=1.3.0&amp;STYLES=&amp;CRS=CRS:84&amp;WIDTH=256&amp;HEIGHT=256&amp;FORMAT=${mimeType}&amp;TRANSPARENT=${transparent}&amp;LAYERS=${layer.name}&amp;BBOX=${bbox.westBoundLongitude},${bbox.southBoundLatitude},${bbox.eastBoundLongitude},${bbox.northBoundLatitude}">${layer.title}</a><br />
                    </c:forEach>
                </td>
            </c:forEach>
            <td>
                <c:forEach var="layer" items="${layers}">
                <c:if test="${layer.queryable}">
                <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
                <a href="wms?REQUEST=GetFeatureInfo&amp;VERSION=1.3.0&amp;STYLES=&amp;CRS=CRS:84&amp;WIDTH=256&amp;HEIGHT=256&amp;I=128&amp;J=128&amp;INFO_FORMAT=text/xml&amp;QUERY_LAYERS=${layer.name}&amp;BBOX=${bbox.westBoundLongitude},${bbox.southBoundLatitude},${bbox.eastBoundLongitude},${bbox.northBoundLatitude}">${layer.title}</a><br />
                </c:if>
                </c:forEach>
            </td>
        </tr>
        </c:if> <%-- End if dataset is ready --%>
        </c:forEach>
    </table>
    
    </body>
</html>
