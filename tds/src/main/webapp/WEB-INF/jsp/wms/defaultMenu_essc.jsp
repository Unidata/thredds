<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/tld/wms/MenuMaker" prefix="menu"%>
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
         <menu:dataset dataset="${datasets.MERSEA_NATL}"/>
 	 <menu:dataset dataset="${datasets.NCOF_FOAM_ONE}"/>
 	 <menu:dataset dataset="${datasets.NCOF_AMM}"/>
         <menu:dataset dataset="${datasets.NCOF_IRISH}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
	 <menu:dataset dataset="${datasets.NCOF_WAVES}"/>
     </menu:folder>

     <menu:folder label="EU-MERSEA">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
         <menu:dataset dataset="${datasets.MERSEA_NATL}"/>
         <menu:dataset dataset="${datasets.MERSEA_GLOBAL}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_V}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_U}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_T}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
     </menu:folder>

     <menu:folder label="EU-ECOOP">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
         <menu:dataset dataset="${datasets.ECOOP_CYCO}"/>
         <menu:dataset dataset="${datasets.ECOOP_ROMS_TEST}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
         <menu:dataset dataset="${datasets.ECOOP_TEST_CYPRUS}"/>
    </menu:folder>

     <menu:folder label="Ocean Hindcasts">
         <menu:dataset dataset="${datasets.CLIVAR_NASA_JPL_ECCO}"/>
         <menu:dataset dataset="${datasets.CLIVAR_SODA_POP}"/>
	
	  <menu:folder label="With data assimilation">
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
         </menu:folder>

	  <menu:folder label="Without data assimilation">
      	  	<menu:folder label="DRAKKAR 1 degree global model (ORCA1-R70)">
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
            <menu:dataset dataset="${datasets.NAVO_SST}" label="NAVO (US Navy)"/>
            <menu:dataset dataset="${datasets.NCDC_AVHRR_AMSR_OI}" label="NCDC (Reynolds)"/>
            <menu:dataset dataset="${datasets.REMSS_mw_ir_OI}" label="RemSS (Remote Sens. Sys.)"/>
         </menu:folder>
         <menu:dataset dataset="${datasets.MERSEA_CORIOLIS_SAL}"/>
         <menu:dataset dataset="${datasets.MERSEA_CORIOLIS_TEMP}"/>
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
         <menu:dataset dataset="${datasets.GLOBMODEL}"/>
         <menu:dataset dataset="${datasets.GENIE}"/>
         <menu:dataset dataset="${datasets.HadCEM}"/>
         <menu:dataset dataset="${datasets.USGS_ADRIATIC_SED038}"/>
         <menu:folder label="MarQuest">
         	<menu:dataset dataset="${datasets.SEAW4}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO2_ASSIMILATION_GRIDT}"/>
        	<menu:dataset dataset="${datasets.NEMO_BIO2_ASSIMILATION_A_GRID}"/>  
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_CONT_PHYSICS}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_CONT_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_CONT_DIAG}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_PHYSICS}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_BIO}"/>
 	      <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_DIAG}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_TLEV_PHYSICS}"/>
              <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_TLEV_BIOLOGY}"/>
 	      <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_TLEV_DIAGNOSTICS}"/>
              <menu:dataset dataset="${datasets.NEMO_FULL_ASSIM_TLEV_PHYS}"/>  
              <menu:dataset dataset="${datasets.NEMO_FULL_ASSIM_TLEV_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_FULL_ASSIM_TLEV_DIAD}"/>
              <menu:dataset dataset="${datasets.NEMO_FULL_CONT_PHYS}"/>  
              <menu:dataset dataset="${datasets.NEMO_FULL_CONT_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_FULL_CONT_DIAD}"/>

               <menu:dataset dataset="${datasets.NEMO_NEWRUN_CONT_PHY}"/>  
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_CONT_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_CONT_DIAD}"/>
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_ASSIM_PHY}"/>  
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_ASSIM_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_ASSIM_DIAD}"/>
             <menu:dataset dataset="${datasets.NEMO_NEWRUN_NINSIT_PHY}"/>  
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_NINSIT_BIO}"/>
              <menu:dataset dataset="${datasets.NEMO_NEWRUN_NINSIT_DIAD}"/>
 	   </menu:folder>
      </menu:folder>

</menu:folder>
