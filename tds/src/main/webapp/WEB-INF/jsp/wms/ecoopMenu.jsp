<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the ECOOP site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=ECOOP" --%>
<c:set var="pmlServer" value="http://ncof.pml.ac.uk/ncWMS/wms"/>
<menu:folder label="ECOOP data visualization">

	<menu:folder label="Baltic Region BOOS">
	    	<menu:folder label="BSH Germany">
    			<menu:folder label="Coastal Model Data (Physical)">
        			<menu:dataset dataset="${datasets.ECOOP_BSH_TS}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_etaV}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_ice}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_TS_hindcast}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_etaV_hindcast}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_ice_hindcast}"/>
	 		</menu:folder>
	 		<menu:folder label="Coastal Model Data (Biology)">
        			<menu:dataset dataset="${datasets.ECOOP_BSEcologicalForecast}"/>
	      			<menu:dataset dataset="${datasets.ECOOP_BSEcologicalBestEstimate}"/>
			</menu:folder>
 		</menu:folder>
    		<menu:folder label="DMI Denmark">
     		       <menu:dataset dataset="${datasets.ECOOP_NSBS}"/>
        		<menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
        		<menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
    		</menu:folder>
    		<menu:folder label="FMI Finland">
       		<menu:dataset dataset="${datasets.FMI_FORECAST}"/>
       		<menu:dataset dataset="${datasets.FMI_BEST}"/>
        		<%--
       		<menu:dataset dataset="${datasets.ECOOP_FMI_s_forecast}"/>
        		<menu:dataset dataset="${datasets.ECOOP_FMI_t_forecast}"/>
	      		<menu:dataset dataset="${datasets.ECOOP_FMI_u_forecast}"/>
        		<menu:dataset dataset="${datasets.ECOOP_FMI_v_forecast}"/>
      			<menu:dataset dataset="${datasets.ECOOP_FMI_w_forecast}"/>
        		<menu:dataset dataset="${datasets.ECOOP_FMI_s_hindcast}"/>
      			<menu:dataset dataset="${datasets.ECOOP_FMI_t_hindcast}"/>
        		<menu:dataset dataset="${datasets.ECOOP_FMI_u_hindcast}"/>
        		<menu:dataset dataset="${datasets.ECOOP_FMI_v_hindcast}"/>
      			<menu:dataset dataset="${datasets.ECOOP_FMI_w_hindcast}"/>
		       --%>
    		</menu:folder>
	</menu:folder>

	<menu:folder label="NW Shelves NOOS">
		<menu:dataset dataset="${datasets.ecoop_mumm}"/>

	    	<menu:folder label="Met Office UK">
        		<%--
			<menu:dataset dataset="${datasets.NCOF_MRCS}" label="POLCOMS MRCS (Physical)"/>
        	       <menu:folder label="POLCOMS MRCS (Biological)">
            			We have to add these layers manually because they are coming from a remote server 
                        ******************************************************************************************************
                        *** TODO: the layer tag has changed.  If these layers are re-enabled we need to update the syntax. ***
                        ******************************************************************************************************
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
		       --%>
        		<menu:dataset dataset="${datasets.NCOF_MRCS}" label="POLCOMS MRCS (Physical)"/>

      	  		<menu:dataset dataset="${datasets.MRCS_ECOOP_TOP_best}"/>
    		</menu:folder>

    		<menu:folder label="DMI Denmark">
     		       <menu:dataset dataset="${datasets.ECOOP_NSBS}"/>
    		</menu:folder>
	    	<menu:folder label="BSH Germany">
    			<menu:folder label="Coastal Model Data (Physical)">
            			<menu:dataset dataset="${datasets.ECOOP_BSH_TS}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_etaV}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_ice}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_TS_hindcast}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_etaV_hindcast}"/>
        			<menu:dataset dataset="${datasets.ECOOP_BSH_ice_hindcast}"/>
	 		</menu:folder>
	 		<menu:folder label="Coastal Model Data (Biology)">
        			<menu:dataset dataset="${datasets.ECOOP_NSEcologicalForecast}"/>
	      			<menu:dataset dataset="${datasets.ECOOP_NSEcologicalBestEstimate}"/>
			</menu:folder>
 		</menu:folder>
	</menu:folder>
	<menu:folder label="Iberian IBIROOS">
	        	<menu:dataset dataset="${datasets.ECOOP_Rectilinear_ROMS}" label="NE Atlantic ROMS Ireland"/>
	        	<menu:dataset dataset="${datasets.ECOOP_PREVIMER_MANGA}" label="PREVIMER Bay of Biscay forecasts France"/>
	        	<menu:dataset dataset="${datasets.ECOOP_ODYSSEA_SST}"/>
	 		<menu:folder label="Mercator psy2v3">
        			<menu:dataset dataset="${datasets.ECOOP_MERCATOR_psy2v3_best_estimate}" label="Best Estimate"/>
	      			<menu:dataset dataset="${datasets.ECOOP_MERCATOR_psy2v3_1week_forecast}" label="One Week Forecast"/>
			</menu:folder>
	        	<menu:dataset dataset="${datasets.ECOOP_PCOMS}"/>
	        	<menu:dataset dataset="${datasets.ecoop_pol_irish}" label="NERC Irish Sea"/>
	</menu:folder>
 
	<menu:folder label="Mediterranean MOON">
              <menu:folder label="Mediterranean ocean analyses Italy">
			<menu:dataset dataset="${datasets.MED_ANALYSES_ITALY}"/>
			<%--
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="temperature" label="sea_water_potential_temperature"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="salinity" label="sea_water_salinity"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="ssh" label="sea_surface_height_above_sea_level"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_T}" id="mld" label="ocean_mixed_layer_thickness"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_U}" id="u" label="eastward_sea_water_velocity"/>
              	<menu:layer dataset="${datasets.MERSEA_MED_V}" id ="v" label="northward_sea_water_velocity"/>
		       --%>
 	       </menu:folder>
	    	<menu:folder label="University of Athens">
			<%--
 			<menu:dataset dataset="${datasets.ECOOP_ALERMO}"/>
		       --%>
         		<menu:folder label="ALERMO Model forecast">
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/ETA" label="sea_surface_height"/>
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/TONZ" label="sea_water_potential_temperature"/>
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/SONZ" label="sea_water_salinity"/>
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/UONZ" label="eastward_sea_water_velocity"/>
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/VONZ" label="northward_sea_water_velocity"/>
     		   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="alermo/sea_water_velocity" label="sea_water_velocity"/>
	        	</menu:folder>
			<menu:folder label="NORTH AEGEAN">
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/ETA" label="sea_surface_height"/>
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/TONZ" label="sea_water_potential_temperature"/>
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/SONZ" label="sea_water_salinity"/>
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/UONZ" label="eastward_sea_water_velocity"/>
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/VONZ" label="northward_sea_water_velocity"/>
	    	   		<menu:remoteLayer server="http://diavlos.oc.phys.uoa.gr/ncwms/wms" layerName="North%20Aegean/sea_water_velocity" label="sea_water_velocity"/>
	        	</menu:folder>
        	</menu:folder>

	    	<menu:folder label="University of Cyprus">
 			<menu:dataset dataset="${datasets.ECOOP_CYPRUS_daily_inst}"/>
			<menu:dataset dataset="${datasets.ECOOP_CYPRUS_6h_avg}"/>
    		</menu:folder>
 		<menu:dataset dataset="${datasets.ECOOP_IOLR}"/>
        	<menu:dataset dataset="${datasets.ECOOP_IMEDEA}"/>
        	<menu:dataset dataset="${datasets.Rosario_6420}"/>
        	<menu:dataset dataset="${datasets.Rosario_9620}"/>
		<menu:dataset dataset="${datasets.AFS_Forecast_Last}"/>
 	</menu:folder>

	<menu:folder label="Black Sea">
  		<menu:dataset dataset="${datasets.ECOOP_BULGARIA}"/>
 		<menu:dataset dataset="${datasets.ECOOP_ROMANIA}"/>
		<menu:dataset dataset="${datasets.MHI_BLACKSEA_CRIMEA_FOR}"/>
	</menu:folder>


</menu:folder>


