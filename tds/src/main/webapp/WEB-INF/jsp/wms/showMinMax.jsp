<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the min and max values of data on a certain image
     
     Data (models) passed in to this page:
          valueRange = Range of floating-point values --%>
<json:object>
    <json:property name="min" value="${valueRange.minimum}"/>
    <json:property name="max" value="${valueRange.maximum}"/>
</json:object>