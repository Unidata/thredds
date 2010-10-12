<%@page contentType="application/vnd.ogc.se_xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<c:when test="${exception.code == 'LayerNotDefined'}">
    <% response.setStatus( HttpServletResponse.SC_NOT_FOUND ); %>
</c:when>
<c:otherwise>
    <% response.setStatus( HttpServletResponse.SC_BAD_REQUEST ); %>
</c:otherwise>

<%-- Displays a WmsException in the correct format --%>
<!DOCTYPE ServiceExceptionReport SYSTEM "http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd">
<ServiceExceptionReport version="1.1.1">
    <ServiceException<c:if test="${not empty exception.code}"> code="${exception.code}"</c:if>>
        ${exception.message}
    </ServiceException>
</ServiceExceptionReport>