<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/tld/wms/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the ECOOP site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=ECOOP" --%>
<c:set var="pmlServer" value="http://ncof.pml.ac.uk/ncWMS/wms"/>
<c:set var="esscServer" value="http://lovejoy.nerc-essc.ac.uk:8080/ncWMS/wms"/>
<menu:folder label="ECOOP data visualization">
    <menu:folder label="UK Met Office">
        <menu:dataset dataset="${datasets.NCOF_MRCS}" label="POLCOMS MRCS (Physical)"/>
        <menu:folder label="POLCOMS MRCS (Biological)">
            <%-- We have to add these layers manually because they are coming from a remote server --%>
            <menu:layer id ="ECOVARSALL/po4" label="Phosphate Concentration" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/si" label="Silicate Concentration" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/no3" label="Nitrate Concentration" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/o2o" label="Dissolved Oxygen Concentration" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/chl" label="Chlorophyll a" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/vis01" label="Visibility in water column" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/p3c" label="Picoplankton biomass" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/zoop" label="Zooplankton biomass" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/p4c" label="Dinoflagellate biomass" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/p2c" label="Flagellate biomass" server="${pmlServer}"/>
            <menu:layer id ="ECOVARSALL/p1c" label="Diatom biomass" server="${pmlServer}"/>
        </menu:folder>
    </menu:folder>
    <menu:folder label="University of Cyprus">
        <menu:dataset dataset="${datasets.ECOOP_CYCO}"/>
         <menu:dataset dataset="${datasets.ECOOP_TEST_CYPRUS}"/>
     </menu:folder>
    <menu:folder label="Marine Institute, Ireland">
        <menu:dataset dataset="${datasets.ECOOP_ROMS_TEST}"/>
    </menu:folder>
    <menu:folder label="DMI, Denmark">
        <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
        <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
    </menu:folder>
</menu:folder>