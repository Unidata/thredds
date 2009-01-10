<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/tld/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%--
     Displays the details of a variable as a JSON object
     See MetadataController.showLayerDetails().
     
     Data (models) passed in to this page:
         layer = Layer object
         datesWithData = Map<Integer, Map<Integer, List<Integer>>>.  Contains
                information about which days contain data for the Layer.  Maps
                years to a map of months to an array of day numbers.
         nearestTimeIso = ISO8601 String representing the point along the time
                axis that is closest to the required date (as passed to the server)
--%>
<json:object>
    <json:property name="units" value="${layer.units}"/>
    <json:array name="bbox" items="${layer.bbox}"/>
    <json:array name="scaleRange" items="${layer.scaleRange}"/>
    <json:array name="supportedStyles" items="${layer.supportedStyles}"/>
    <c:if test="${layer.zaxisPresent}">
        <json:object name="zaxis">
            <json:property name="units" value="${layer.zunits}"/>
            <json:property name="positive" value="${layer.zpositive}"/>
            <json:array name="values" items="${layer.zvalues}" var="z">
                ${utils:abs(z)}
            </json:array>
        </json:object>
    </c:if>
    <c:if test="${layer.taxisPresent}">
        <json:object name="datesWithData">
            <c:forEach var="year" items="${datesWithData}">
                <json:object name="${year.key}">
                    <c:forEach var="month" items="${year.value}">
                        <json:array name="${month.key}" items="${month.value}"/>
                    </c:forEach>
                </json:object>
            </c:forEach>
        </json:object>
        <%-- The nearest time on the time axis to the time that's currently
             selected on the web interface, in ISO8601 format --%>
        <json:property name="nearestTimeIso" value="${nearestTimeIso}"/>
    </c:if>
    <json:property name="copyright" value="${layer.copyrightStatement}"/>
    <json:array name="palettes" items="${paletteNames}"/>
</json:object>
