<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the NCEO site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=NCEO" --%>
<menu:folder label="NCEO data visualization">
    <menu:folder label="Atmosphere">
        <menu:dataset dataset="${datasets.GLOBMODEL}" label="GlobModel"/>
    </menu:folder>
    <menu:folder label="Ocean">
        <menu:dataset dataset="${datasets.OSTIA}" label="OSTIA analysis"/>
        <menu:dataset dataset="${datasets.KRIGED_TOPEX}"/>
    </menu:folder>
    <menu:folder label="Land surface">
        <menu:dataset dataset="${datasets.NSIDC}" label="NSIDC Snow Water Equivalent"/>
        <menu:dataset dataset="${datasets.fasir}"/>
    </menu:folder>
</menu:folder>
