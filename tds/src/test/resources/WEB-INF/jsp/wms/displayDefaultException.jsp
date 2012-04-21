<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays an unexpected exception (i.e. not a WmsException) --%>
<ServiceExceptionReport version="1.3.0"
                        xmlns="http://www.opengis.net/ogc"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd">
    <ServiceException>Unexpected error of type ${exception.class.name}: ${exception.message}</ServiceException>
</ServiceExceptionReport>