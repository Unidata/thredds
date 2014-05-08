<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wms/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays a JSON object showing the available timesteps for a given date
     See MetadataController.showTimesteps().
     
     Data (models) passed in to this page:
         timesteps = list of times (in milliseconds since the epoch) that fall on this day --%>
<json:object>
    <json:array name="timesteps" items="${timesteps}" var="t">
        ${utils:formatUTCTimeOnly(t)}
    </json:array>
</json:object>