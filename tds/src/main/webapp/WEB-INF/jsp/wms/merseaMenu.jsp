<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the MERSEA site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=MERSEA"--%>
 <menu:folder label="MERSEA Dynamic Quick View">
     <menu:folder label="Global Oceans">
         <menu:folder label="MERCATOR">
             <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2}" label="Global Ocean forecast (degraded resolution)"/>
             <menu:folder label="Global ocean forecast by region (full resolution)">
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_NAT}" label="North Atlantic"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_SAT}" label="South Atlantic"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_TAT}" label="Tropical Atlantic"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_MED}" label="Mediterranean"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_NPA}" label="North Pacific"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_SPA}" label="South Pacific"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_TPA}" label="Tropical Pacific"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_ARC}" label="Arctic Ocean"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_ACC}" label="Circumpolar Antarctic"/>
                 <menu:dataset dataset="${datasets.MERSEA_GLOBAL_Psy3v2_IND}" label="Indian Ocean"/>
             </menu:folder>
         </menu:folder>
         <menu:dataset dataset="${datasets.MERSEA_NRTSLA}"/>
         <menu:dataset dataset="${datasets.MERSEA_CORIOLIS}"/>
         <menu:dataset dataset="${datasets.CERSAT_NRT_SST}"/>
     </menu:folder>
     <menu:folder label="Baltic Sea">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
     </menu:folder>
     <menu:folder label="North-East Atlantic Ocean">
         <%-- <menu:dataset dataset="${datasets.MERSEA_NATL}" label="FOAM North Atlantic analysis"/> --%>
         <menu:folder label="North Atlantic 1/12 degree">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natl12/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
     </menu:folder>
     <menu:folder label="Mediterranean Sea">
         <%-- The Mediterranean data come from three different datasets so we
              manually aggregate them in a single folder --%>
		<menu:dataset dataset="${datasets.MED_ANALYSES_ITALY}"/>
    	    <%--
	    <menu:folder label="Mediterranean ocean analyses">
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="temperature" label="sea_water_potential_temperature"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="salinity" label="sea_water_salinity"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="ssh" label="sea_surface_height_above_sea_level"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="mld" label="ocean_mixed_layer_thickness"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_U}" id="u" label="eastward_sea_water_velocity"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_V}" id ="v" label="northward_sea_water_velocity"/>
	   </menu:folder>
          --%>
     </menu:folder>
     <menu:folder label="Arctic Ocean">
         <menu:dataset dataset="${datasets.MERSEA_ARCTIC_TOPAZ}" label="Arctic Ocean analyses and forecasts"/>
     </menu:folder>
 </menu:folder>