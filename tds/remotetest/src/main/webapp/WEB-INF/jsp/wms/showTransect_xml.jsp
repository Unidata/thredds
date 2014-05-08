<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Shows data from a GetTransect request as XML
     
     Data (models) passed in to this page:
          crs  = String representing the coordinate reference system used by this transect
          linestring = String representing the line string that the client selected, from which the transect is generated
          layer = Layer from which the data were generated
          data = Map of ProjectionPoints (x-y points)  objects to data values (floats) --%>
<transect>
    <description>
        The locations of data lie on the line string.  Data values are extracted
        from the nearest-neighbour grid point to each point on the line string.
        The number of data points is chosen to sample the source data adequately
        but without excessive oversampling.  Some grid cells may nevertheless
        be sampled more than once.
    </description>
    <crs>${crs}</crs>
    <linestring>${linestring}</linestring>
    <dataset>${layer.dataset.title}</dataset>
    <variable>${layer.title}</variable>
    <units>${layer.units}</units>
    <transectData numPoints="${fn:length(data)}">
        <c:forEach var="datapoint" items="${data}">
        <dataPoint>
            <location>${datapoint.key.x} ${datapoint.key.y}</location>
            <value>${datapoint.value}</value>
        </dataPoint>
        </c:forEach>
    </transectData>
</transect>