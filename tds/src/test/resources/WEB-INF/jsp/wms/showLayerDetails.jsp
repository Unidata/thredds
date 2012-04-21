<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
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

    <c:set var="bbox" value="${layer.geographicBoundingBox}"/>
    <json:array name="bbox">
        <json:property>${bbox.westBoundLongitude}</json:property>
        <json:property>${bbox.southBoundLatitude}</json:property>
        <json:property>${bbox.eastBoundLongitude}</json:property>
        <json:property>${bbox.northBoundLatitude}</json:property>
    </json:array>

    <json:array name="scaleRange">
        <json:property>${layer.approxValueRange.minimum}</json:property>
        <json:property>${layer.approxValueRange.maximum}</json:property>
    </json:array>

    <json:property name="numColorBands" value="${layer.defaultNumColorBands}"/>

    <c:set var="styles" value="boxfill"/>
    <c:if test="${utils:isVectorLayer(layer)}">
        <c:set var="styles" value="vector,boxfill"/>
    </c:if>
    <json:array name="supportedStyles" items="${styles}"/>

    <c:if test="${not empty layer.elevationValues}">
        <json:object name="zaxis">
            <json:property name="units" value="${layer.elevationUnits}"/>
            <json:property name="positive" value="${layer.elevationPositive}"/>
            <json:array name="values" items="${layer.elevationValues}"/>
        </json:object>
    </c:if>

    <c:if test="${not empty layer.timeValues}">
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
        <%-- The time axis units: "ISO8601" for "normal" axes, "360_day" for
             axes that use the 360-day calendar --%>
        <json:property name="timeAxisUnits" value="${utils:getTimeAxisUnits(layer.chronology)}"/>
    </c:if>
    
    <json:property name="moreInfo" value="${layer.dataset.moreInfoUrl}"/>
    <json:property name="copyright" value="${layer.dataset.copyrightStatement}"/>
    <json:array name="palettes" items="${paletteNames}"/>
    <json:property name="defaultPalette" value="${layer.defaultColorPalette.name}"/>
    <json:property name="logScaling" value="${layer.logScaling}"/>
</json:object>
