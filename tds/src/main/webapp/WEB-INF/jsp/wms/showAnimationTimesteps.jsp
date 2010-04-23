<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%--
     Displays the time strings needed to create animations at various temporal
     resolutions.
     See MetadataController.showAnimationTimesteps().

     Data (models) passed in to this page:
         timeStrings: Map<String, String> of time resolutions to time strings.
--%>
<json:object>
    <json:array name="timeStrings">
        <c:forEach var="timeString" items="${timeStrings}">
            <json:object>
                <json:property name="title" value="${timeString.key}"/>
                <json:property name="timeString" value="${timeString.value}"/>
            </json:object>
        </c:forEach>
    </json:array>
</json:object>
