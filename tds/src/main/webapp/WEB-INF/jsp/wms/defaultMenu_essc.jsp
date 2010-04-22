<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%--
     Displays the hierarchy of layers from this server as a JSON object
     See MetadataController.showLayerHierarchy().
     
     Data (models) passed in to this page:
         serverTitle = String: title for this server
         datasets = Map<String, Dataset>: all the datasets in this server
         serverInfo  = uk.ac.rdg.resc.ncwms.config.Server object
--%>
<menu:folder label="${serverTitle}">

     <c:set var="dataset" value="${datasets}" />

     <menu:folder label="NCOF Products">
        <%-- <menu:dataset dataset="${datasets.MERSEA_NATL}"/> --%>
 	 <menu:dataset dataset="${datasets.NCOF_FOAM_ONE}"/>
 	 <menu:dataset dataset="${datasets.NCOF_AMM}"/>
        <menu:dataset dataset="${datasets.NCOF_IRISH}"/>
	 <menu:dataset dataset="${datasets.NCOF_WAVES}"/>
    
         <menu:folder label="OSTIA GMPE">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/std" label="Standard deviation of input analyses"/>
         </menu:folder>
         <menu:folder label="OSTIA Anomalies">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/sst_anomaly" label="sea_surface_temperature_anomaly"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/analysed_sst" label="sea_surface_temperature"/>
         </menu:folder>
         <menu:folder label="OSTIA SST">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysis_error" label="estimated error standard deviation of analysed_sst"/>
         </menu:folder>
         <menu:folder label="North West Shelf Daily 17 level">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/phytoc" label="mass_concentration_of_total_phytoplankton_in_seawater_expressed_as_carbon"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/sn" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/o2o" label="mole_concentration_of_dissolved_oxygen_in_seawater"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/po4" label="mole_concentration_of_dissolved_organic_phosphate_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/no3" label="mole_concentration_of_dissolved_organic_nitrate_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/vn" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/un" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/potmp" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/pprd" label="net_primary_productivity_of_carbon"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/chl" label="concentration_of_chlorophyll_in_seawater"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/attn" label="volume_beam_attenuation_coefficient_of_radiative_flux_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="North West Shelf Hourly 3 level">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sse" label="sea_surface_height_above_reference_ellipsoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sn" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/vn" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/un" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/potmp" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
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
         <menu:folder label="Mediterranean 1/12 degree">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med12/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - Indian Ocean">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_indian/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - Mediterranean">
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/sossheig" label="sea_surface_height_above_geoid"/>
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/vosaline" label="sea_water_potential_temperature"/>
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/votemper" label="northward_sea_water_velocity"/>
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/vomecrty" label="eastward_sea_water_velocity"/>
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/vozocrtx" label="sea_surface_height_above_geoid"/>
	      <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_med/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - Global Ocean">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_global/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
          <menu:folder label="Global - Southern Ocean">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_southern/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
          <menu:folder label="Global - Arctic Ocean">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_arctic/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
          <menu:folder label="Global - North Atlantic">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_natlantic/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - North Pacific">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/sossheig" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/iiceconc" label="sea_ice_area_fraction"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/iicethic" label="sea_ice_thickness"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/itmecrty" label="Ice meridional current"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/itzocrtx" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/vosaline" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/votemper" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/vomecrty" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/vozocrtx" label="sea_surface_height_above_geoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_npacific/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - South Atlantic">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/iiceconc" label="sea_ice_area_fraction"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/iicethic" label="sea_ice_thickness"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/itmecrty" label="Ice meridional current"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/itzocrtx" label="sea_water_salinity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_satlantic/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - South Pacific">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/iiceconc" label="sea_ice_area_fraction"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/iicethic" label="sea_ice_thickness"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/itmecrty" label="Ice meridional current"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/itzocrtx" label="sea_water_salinity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_spacific/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="Global - Tropical Atlantic">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tatlantic/sea_water_velocity" label="sea_water_velocity"/>
        </menu:folder>
  	 <menu:folder label="Global - Tropical Pacific">
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/sossheig" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/vosaline" label="sea_water_potential_temperature"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/votemper" label="northward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/vomecrty" label="eastward_sea_water_velocity"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/vozocrtx" label="sea_surface_height_above_geoid"/>
              <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="glo_tpacific/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
     </menu:folder>

     <menu:folder label="EU-MERSEA">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
         <%-- <menu:dataset dataset="${datasets.MERSEA_NATL}"/> --%>
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
  	  <menu:dataset dataset="${datasets.MED_ANALYSES_ITALY}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
 	  <%--	
		<menu:dataset dataset="${datasets.MERSEA_GLOBAL}"/>
  		<menu:dataset dataset="${datasets.MERSEA_MED_V}"/>
         	<menu:dataset dataset="${datasets.MERSEA_MED_U}"/>
         	<menu:dataset dataset="${datasets.MERSEA_MED_T}"/>
  	  <menu:folder label="MyOcean Test Data">
        	<menu:dataset dataset="${datasets.met_nw3}"/>
        	<menu:dataset dataset="${datasets.met_nw17}"/>
        	<menu:dataset dataset="${datasets.met_ostia_anom}"/>
        	<menu:dataset dataset="${datasets.met_ostia_gmpe}"/>
	  </menu:folder>
	  --%>
         <menu:folder label="OSTIA GMPE">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/std" label="Standard deviation of input analyses"/>
         </menu:folder>
         <menu:folder label="OSTIA Anomalies">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/sst_anomaly" label="sea_surface_temperature_anomaly"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/analysed_sst" label="sea_surface_temperature"/>
         </menu:folder>
         <menu:folder label="OSTIA SST">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysis_error" label="estimated error standard deviation of analysed_sst"/>
         </menu:folder>
         <menu:folder label="North West Shelf Daily 17 level">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/phytoc" label="mass_concentration_of_total_phytoplankton_in_seawater_expressed_as_carbon"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/sn" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/o2o" label="mole_concentration_of_dissolved_oxygen_in_seawater"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/po4" label="mole_concentration_of_dissolved_organic_phosphate_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/no3" label="mole_concentration_of_dissolved_organic_nitrate_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/vn" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/un" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/potmp" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/pprd" label="net_primary_productivity_of_carbon"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/chl" label="concentration_of_chlorophyll_in_seawater"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/attn" label="volume_beam_attenuation_coefficient_of_radiative_flux_in_sea_water"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_daily/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
         <menu:folder label="North West Shelf Hourly 3 level">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sse" label="sea_surface_height_above_reference_ellipsoid"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sn" label="sea_water_salinity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/vn" label="northward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/un" label="eastward_sea_water_velocity"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/potmp" label="sea_water_potential_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="nwshelf_hourly/sea_water_velocity" label="sea_water_velocity"/>
         </menu:folder>
     </menu:folder>



     <menu:folder label="EU-ECOOP">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
    	  <menu:folder label="BSH Coastal Model Data">
        	<menu:dataset dataset="${datasets.ECOOP_BSH_TS}"/>
        	<menu:dataset dataset="${datasets.ECOOP_BSH_etaV}"/>
        	<menu:dataset dataset="${datasets.ECOOP_BSH_ice}"/>
	  </menu:folder>
         <menu:dataset dataset="${datasets.ECOOP_CYCO}"/>
         <menu:dataset dataset="${datasets.ECOOP_ROMS_TEST}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
         <menu:dataset dataset="${datasets.ECOOP_TEST_CYPRUS}"/>
    </menu:folder>

     <menu:folder label="Ocean Hindcasts">
         <menu:dataset dataset="${datasets.CLIVAR_NASA_JPL_ECCO}"/>
         <menu:dataset dataset="${datasets.CLIVAR_SODA_POP}"/>
	  <menu:dataset dataset="${datasets.HYCOM_PACIFIC_OCEAN_BASIN_SIMULATION}"/>

	  <menu:folder label="With data assimilation">

      	  	<menu:folder label="DRAKKAR 1 degree global model (ORCA1-MV01)">
     			<menu:dataset dataset="${datasets.ORCA1_mv01_MONTHLY_assim}" label="Monthly means (NEW)"/>
     			<menu:dataset dataset="${datasets.ORCA1_mv01_MONTHLY_assim_exp18}" label="Monthly means (Exp 18)"/>
           	</menu:folder>

 	     <menu:folder label="DRAKKAR 1/4 degree global S(T) reanalysis (ORCA025-R07)">

		    	<menu:folder label="5 day means">
             			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_5day_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_5day_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_5day_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_5day_v}" label="V-GRID"/>
       	    	</menu:folder>

		    	<menu:folder label="Monthly means">
             			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_MONTHLY_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_MONTHLY_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_MONTHLY_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_MONTHLY_v}" label="V-GRID"/>
       	    	</menu:folder>

		    	<menu:folder label="Seasonal means">
             			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_SEASONAL_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_SEASONAL_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_SEASONAL_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_SEASONAL_v}" label="V-GRID"/>
       	    	</menu:folder>

		    	<menu:folder label="Annual means">
             			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_ANNUAL_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_ANNUAL_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_ANNUAL_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_Exp4_ANNUAL_v}" label="V-GRID"/>
       	    	</menu:folder>
           	</menu:folder>
       	<menu:folder label="1/4 degree Global S(T) Reanalysis with DFS4 Forcing (ORCA025-R07/Exp5)">

		    	<menu:folder label="Monthly means">
           			<menu:dataset dataset="${datasets.DFS4_ORCA025_R07_gridT_Monthly}" label="T-GRID"/>
       	    	</menu:folder>
		    	<menu:folder label="Seasonal means">
           			<menu:dataset dataset="${datasets.DFS4_ORCA025_R07_gridT_Annual}" label="T-GRID"/>
       	    	</menu:folder>
		    	<menu:folder label="Annual means">
           			<menu:dataset dataset="${datasets.DFS4_ORCA025_R07_gridT_Seasonal}" label="T-GRID"/>
       	    	</menu:folder>
           	</menu:folder>
         </menu:folder>

	  <menu:folder label="Without data assimilation">
      	  	<menu:folder label="DRAKKAR 1 degree global model (ORCA1-R70)">
     			<menu:dataset dataset="${datasets.ORCA1_new_MONTHLY}" label="Monthly means (NEW)"/>
     			<menu:dataset dataset="${datasets.ORCA1_R70_MONTHLY}" label="Monthly means"/>
    			<menu:dataset dataset="${datasets.ORCA1_R70_SEASONAL}" label="Seasonal means"/>
		    	<menu:folder label="Annual means">
             			<menu:dataset dataset="${datasets.ORCA1_R70_ANNUAL_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA1_R70_ANNUAL_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA1_R70_ANNUAL_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA1_R70_ANNUAL_v}" label="V-GRID"/>
       	    	</menu:folder>
           	</menu:folder>

      		<menu:folder label="DRAKKAR 1/4 degree global model (ORCA025-G70)">
		    	<menu:folder label="Monthly means">
             			<menu:dataset dataset="${datasets.ORCA025_G70_MONTHLY_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_G70_MONTHLY_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_G70_MONTHLY_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_G70_MONTHLY_v}" label="V-GRID"/>
       	    	</menu:folder>

		    	<menu:folder label="Seasonal means">
         			<menu:dataset dataset="${datasets.ORCA025_G70_SEASONAL_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_G70_SEASONAL_v}" label="V-GRID"/>
       	    	</menu:folder>

		    	<menu:folder label="Annual means">
           			<menu:dataset dataset="${datasets.ORCA025_G70_ANNUAL_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_G70_ANNUAL_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_G70_ANNUAL_v}" label="V-GRID"/>
       	    	</menu:folder>
     		</menu:folder>

      		<menu:folder label="DRAKKAR 1/4 degree global model (ORCA025-R07)">
		    	<menu:folder label="5 day means">
             			<menu:dataset dataset="${datasets.ORCA025_R07_S_5day_ice}" label="ICE"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_S_5day_t}" label="T-GRID"/>
         			<menu:dataset dataset="${datasets.ORCA025_R07_S_5day_u}" label="U-GRID"/>
           			<menu:dataset dataset="${datasets.ORCA025_R07_S_5day_v}" label="V-GRID"/>
       	    	</menu:folder>
     		</menu:folder>

         </menu:folder>
     </menu:folder>
   

     <menu:folder label="Observations">
         <menu:folder label="Ocean">
         <menu:folder label="GODAE SST analyses">
            <menu:dataset dataset="${datasets.OSTIA}" label="OSTIA (UKMO)"/>
            <menu:dataset dataset="${datasets.EUR_ODYSSEA}" label="ODYSSEA (FR)"/>
	  <%--	
            <menu:dataset dataset="${datasets.NAVO_SST}" label="NAVO (US Navy)"/>
            <menu:dataset dataset="${datasets.NCDC_AVHRR_AMSR_OI}" label="NCDC (Reynolds)"/>
            <menu:dataset dataset="${datasets.REMSS_mw_ir_OI}" label="RemSS (Remote Sens. Sys.)"/>
	  --%>
         </menu:folder>

         <menu:folder label="OSTIA GMPE">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_gmpe/std" label="Standard deviation of input analyses"/>
         </menu:folder>
         <menu:folder label="OSTIA Anomalies">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/sst_anomaly" label="sea_surface_temperature_anomaly"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia_anom/analysed_sst" label="sea_surface_temperature"/>
         </menu:folder>
         <menu:folder label="OSTIA SST">
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysed_sst" label="sea_surface_temperature"/>
             <menu:remoteLayer server="http://data.ncof.co.uk:8080/ncWMS/wms" layerName="ostia/analysis_error" label="estimated error standard deviation of analysed_sst"/>
         </menu:folder>


         <menu:dataset dataset="${datasets.MERSEA_CNR_SST}"/>
         <menu:dataset dataset="${datasets.OSTIA_OLD}"/>
         <menu:dataset dataset="${datasets.OSTIA}"/>
         <menu:dataset dataset="${datasets.MERSEA_NRTSLA}"/>
         </menu:folder>
         <menu:folder label="Atmosphere">
             <menu:dataset dataset="${datasets.TEST_OZONE}"/>
         </menu:folder>
         <menu:folder label="Land surface">
             <menu:dataset dataset="${datasets.NSIDC}"/>
             <menu:dataset dataset="${datasets.NSIDC_STDEV}"/>
         </menu:folder>
     </menu:folder>

     <menu:folder label="Other">
         <menu:dataset dataset="${datasets.ORCA1_NOCS}" label="Test ORCA1-NOCS"/>
         <menu:dataset dataset="${datasets.GLOBMODEL}"/>
         <menu:dataset dataset="${datasets.GENIE}"/>
         <menu:dataset dataset="${datasets.HadCEM}"/>
         <menu:dataset dataset="${datasets.USGS_ADRIATIC_SED038}"/>
         <menu:dataset dataset="${datasets.OFAM_TEST}"/>
         <menu:folder label="MarQuest">
                <menu:dataset dataset="${datasets.SEAW4}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_CONT_PHYS}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_ASSIM_PHYS}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_INC_PHYS}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_CONT_BIO}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_ASSIM_BIO}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_INC_BIO}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_CONT_DIAD}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_ASSIM_DIAD}"/>
                <menu:dataset dataset="${datasets.NEWRUN3_INC_DIAD}"/>
 	   </menu:folder>
     </menu:folder>

</menu:folder>
