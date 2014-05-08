<%@page contentType="text/xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays a RadarServerException in XML format --%>
<RadarServerException>
    <exception>${exception.message}</exception>
</RadarServerException>
