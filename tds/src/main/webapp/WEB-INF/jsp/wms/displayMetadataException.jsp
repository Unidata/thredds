<%@page contentType="text/plain"%> <%-- TODO: replace with json MIME type --%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%
response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays a MetadataException in JSON format
     Objects passed in to this page:
     exception : A MetadataException object --%>
<json:object>
    <json:object name="exception">
        <json:property name="className" value="${utils:getExceptionName(exception)}"/>
        <json:property name="message" value="${exception.cause.message}"/> 
    </json:object>
</json:object>