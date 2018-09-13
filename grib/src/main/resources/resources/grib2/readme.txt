# grib2/readme.txt

03/03/2011 Atsushi (AShimazaki@wmo.int) sends me XML tables.
 - here is where they apparently live at WMO, see link on this page:
   http://www.wmo.int/pages/prog/www/WMOCodes/TDCFtables.html#TDCFtables

12/7/2011
 - edward glen (edward.glen@metservice.com) sends a file from center (canadian met) with parameter 0-4-192.
   from this page: http://www.weatheroffice.gc.ca/grib/what_is_GRIB_e.html referencing NCEP (at bottom), Ive decided to
    map all of center 54 to NCEP tables.

12/8/2011
 - jeff.brogden@noaa.gov sent me a new HRRR (hi res rapid refresh) model output E:/work/brogden/hrrr_20111207-1400F0000.grib2
   center is 59 with lots of local params. i am going to assume all FSL files should use NCEP center 7 tables

12/8/2011 new WMO tables found from http://www.wmo.int/pages/prog/www/WMOCodes/TDCFtables.html#TDCFtables
 - GRIB2 8.0
 - BUFR 17.0 (not integrated yet)
 - Common Codes 02Nov11
 - move older tables into src/main/sources to reduce jar size

01/30/2011
 - correction to 0-15-3 (Vertically Integrated Liquid)

    Hello John,

    The correction was proposed by NOAA/NCEP.  It was reported that the proposer has not seen the data in km/m.  You can therefore practically correct the unit.
    The approval process by WMO on the correction is underway, but I think it will be corrected in May 2012.

    Best regards,
    Atsushi

    On 30 January 2012 17:35, John Caron <caron@unidata.ucar.edu> wrote:
    >
    > Hi Atsushi:
    >
    > I have a report that the parameter 0-15-3 (Vertically Integrated Liquid)
    >
    > which has units of kg/m in
    >
    >   http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/2011edition/WMO306_vI2_GRIB2_CodeFlag_en.pdf
    >
    > should have units of
    >
    >   kg/m2
    >
    > What do you think? Does everyone already use kg/m2 so we should just correct all version of the tables?

02/02/2012 FSL HRRR  http://ruc.noaa.gov/hrrr/GRIB2Table.txt
 - turns out FSL local params are very different from NCEP
 - add FslLocalTables using C:\dev\github\thredds\grib\src\main\resources\resources\grib2\local\Fsl-hrrr.csv
 - There are a few possible conflicts, as seen below (your table is p1, WMO is p2).
The first 3 are differences in units, the second 3 are possible problems in parameter names.

Table 1 : FSL-HRRR
Table 2 : Standard WMO version 8

  p1=    0.2.15                               Wind Shear             m/s            null U-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level
  p2=4.2.0.2.15               Vertical u-component shear             1/s            null null

  ud=    0.2.15 m/s (m.s-1) != 1/s for 0.2.15 (Wind Shear)

  p1=    0.2.16                               Wind Shear             m/s            null V-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level
  p2=4.2.0.2.16               Vertical v-component shear             1/s            null null

  ud=    0.2.16 m/s (m.s-1) != 1/s for 0.2.16 (Wind Shear)

  p1=    0.2.19                              Wind Energy            W/m2            null Wind energy generation potential using 0.5*density*speed^3 at 80 m above ground level
  p2=4.2.0.2.19                       Wind mixing energy               J            null null

  ud=    0.2.19 W/m2 (kg.s-3) != J for 0.2.19 (Wind Energy)


  p1=     0.3.5                                   Height             gpm            null Adiabatic condensation level above ground (LCL)
  p2= 4.2.0.3.5                      Geopotential height             gpm            null null


  p1=     0.7.6                              Instability            J/kg            null Most unstable CAPE (MUCAPE) using parcel with highest theta-e in lowest 300 mb
  p2= 4.2.0.7.6    Convective available potential energy            J/kg            null null

  p1=     0.7.7                              Instability            J/kg            null Most unstable CIN (MUCIN) using parcel with highest theta-e in lowest 300 mb
  p2= 4.2.0.7.7                    Convective inhibition            J/kg            null null

08/06/2012
  get http://ruc.noaa.gov/hrrr/GRIB2Table.txt again, rename as Fsl-hrrr2.csv and compare with previous

Table 1 = FSL2 (resources/grib2/local/Fsl-hrrr2.csv)
Table 2 = FSL (resources/grib2/local/Fsl-hrrr.csv)
Table 1 :
  t1=     0.0.0                                      TMP               K             null Mean temperature in lowest 30 mb
  t2=     0.0.0                              Temperature               K             null Mean temperature in lowest 30 mb

  t1=     0.0.2                                      POT               K             null Mean potential temperature in lowest 30 mb
  t2=     0.0.2                    Potential temperature               K             null Mean potential temperature in lowest 30 mb

  t1=     0.0.6                                      DPT               K             null Mean dewpoint temperature in lowest 30 mb
  t2=     0.0.6                    Dew-point temperature               K             null Mean dewpoint temperature in lowest 30 mb

  t1=    0.0.10                                    LHTFL            W/m2             null Latent heat net flux at surface
  t2=    0.0.10                     Latent heat net flux           W.m-2             null Latent heat net flux at surface

  t1=    0.0.11                                    SHTFL            W/m2             null Sensible heat net flux at surface
  t2=    0.0.11                   Sensible heat net flux           W.m-2             null Sensible heat net flux at surface

  t1=   0.0.195                    Convective Initiation         minutes             null Time since last convective initation (> 1 hr) based upon lightning threat 3 and no adjacent activity
  t2=   0.0.195                   Convective Initiation1         minutes             null Time since last convective initation (> 1 hr) based upon lightning threat 3 and no adjacent activity

  t1=   0.0.196                     Convective Acitivity         minutes             null Time since last convective activity based upon lightning threat 3
  t2=   0.0.196                    Convective Acitivity1         minutes             null Time since last convective activity based upon lightning threat 3

  t1=   0.0.201                     Convective Activitiy         minutes             null Time since last convective activity based upon vertical flux of rain and graupel
  t2=   0.0.201                    Convective Activitiy2         minutes             null Time since last convective activity based upon vertical flux of rain and graupel

  t1=   0.0.202                      Convective Activity         minutes             null Time since last convective activitiy based upon 35 dBZ reflectivity at -10C
  t2=   0.0.202                     Convective Activity3         minutes             null Time since last convective activitiy based upon 35 dBZ reflectivity at -10C

  t1=     0.1.0                                     SPFH           kg/kg             null Specific humidity at 2 m above ground level
  t2=     0.1.0                        Specific humidity           kg/kg             null Specific humidity at 2 m above ground level

  t1=     0.1.1                                       RH               %             null Mean relative humidity in lowest 30 mb
  t2=     0.1.1                        Relative humidity               %             null Mean relative humidity in lowest 30 mb

  t1=     0.1.3                                     PWAT           kg/m2             null Precipitable water in model column
  t2=     0.1.3                       Precipitable water          kg.m-2             null Precipitable water in model column

  t1=     0.1.8                                     APCP           kg/m2             null Total precipitation accumulated over previous hour
  t2=     0.1.8                      Total precipitation          kg.m-2             null Total precipitation accumulated over previous hour

  t1=     0.1.9                                    NCPCP           kg/m2             null Large-scale precipitation accumulated over previous hour
  t2=     0.1.9 Large-scale precipitation (non-convective)          kg.m-2             null Large-scale precipitation accumulated over previous hour

  t1=    0.1.10                                    ACPCP           kg/m2             null Convective-scale precipitation accumulated over previous hour
  t2=    0.1.10                 Convective precipitation          kg.m-2             null Convective-scale precipitation accumulated over previous hour

  t1=    0.1.13                                    WEASD           kg/m2             null Water equivalent of accumulated snow depth over previous hour
  t2=    0.1.13 Water equivalent of accumulated snow depth          kg.m-2             null Water equivalent of accumulated snow depth over previous hour

  t1=   0.1.192                       Precipitation Type           no=0)             null Categorical rain (yes=1
  t2=   0.1.192                      Precipitation Type4            flag             null Categorical rain (yes=1 no=0)

  t1=   0.1.193                       Precipitation Type           no=0)             null Categorical freezing rain (yes=1
  t2=   0.1.193                      Precipitation Type3            flag             null Categorical freezing rain (yes=1 no=0)

  t1=   0.1.194                       Precipitation Type           no=0)             null Categorical ice pellets (yes=1
  t2=   0.1.194                      Precipitation Type2            flag             null Categorical ice pellets (yes=1 no=0)

  t1=   0.1.195                       Precipitation Type           no=0)             null Categorical snow (yes=1
  t2=   0.1.195                      Precipitation Type1            flag             null Categorical snow (yes=1 no=0)

  t1=   0.1.213                    Convective Initiation         minutes             null Time since last convective initation (> 1 hr) based upon flux of rain/graupel and no adjacent activity
  t2=   0.1.213                   Convective Initiation2         minutes             null Time since last convective initation (> 1 hr) based upon flux of rain/graupel and no adjacent activity

  t1=   0.1.214                    Convective Initiation         minutes             null Time since last convective initation (> 1 hr) based upon 35 dBZ reflectivity at -10C and no adjacent activity
  t2=   0.1.214                   Convective Initiation3         minutes             null Time since last convective initation (> 1 hr) based upon 35 dBZ reflectivity at -10C and no adjacent activity

  t1=     0.2.2                                     UGRD             m/s             null U-component of wind at 10 m above ground level
  t2=     0.2.2                      u-component of wind             m/s             null U-component of wind at 10 m above ground level

  t1=     0.2.3                                     VGRD             m/s             null V-component of wind at 10 m above ground level
  t2=     0.2.3                      v-component of wind             m/s             null V-component of wind at 10 m above ground level

  t1=     0.2.9                                     DZDT             m/s             null Mean vertical velocity in sigma 0.8-0.5 layer
  t2=     0.2.9            Vertical velocity (geometric)             m/s             null Mean vertical velocity in sigma 0.8-0.5 layer

  t1=    0.2.15                                    VUCSH             m/s             null U-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level
  t2=    0.2.15               Vertical u-component shear             1/s             null U-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level

  t1=    0.2.16                                    VVCSH             m/s             null V-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level
  t2=    0.2.16               Vertical v-component shear             1/s             null V-component of vector wind difference between wind in surface-500 m and 5.5-6.0 km layers above ground level

  t1=    0.2.19                                    WMIXE            W/m2             null Wind energy generation potential using 0.5*density*speed^3 at 80 m above ground level
  t2=    0.2.19                       Wind mixing energy               J             null Wind energy generation potential using 0.5*density*speed^3 at 80 m above ground level

  t1=    0.2.22                                     GUST             m/s             null Wind gust speed at 10 m above ground level
  t2=    0.2.22                        Wind speed (gust)             m/s             null Wind gust speed at 10 m above ground level

  t1=   0.2.194                             Storm Motion             m/s             null U-component of convective storm motion for right-moving cells using Bunkers et al. 2000
  t2=   0.2.194                           Storm Motion-U             m/s             null U-component of convective storm motion for right-moving cells using Bunkers et al. 2000

  t1=   0.2.195                             Storm Motion             m/s             null V-component of convective storm motion for right-moving cells using Bunkers et al. 2000
  t2=   0.2.195                           Storm Motion-V             m/s             null V-component of convective storm motion for right-moving cells using Bunkers et al. 2000

  t1=     0.3.0                                     PRES              Pa             null Surface pressure
  t2=     0.3.0                                 Pressure              Pa             null Surface pressure

  t1=     0.3.1                                    PRMSL              Pa             null Pressure reduced to mean sea level via MAPS method
  t2=     0.3.1                  Pressure reduced to MSL              Pa             null Pressure reduced to mean sea level via MAPS method

  t1=     0.3.5                                      HGT             gpm             null Adiabatic condensation level above ground (LCL)
  t2=     0.3.5                      Geopotential height             gpm             null Adiabatic condensation level above ground (LCL)

  t1=     0.6.1                                     TCDC               %             null Total cloud cover fraction based on maximum in model column within 30 km radius of a grid point
  t2=     0.6.1                        Total cloud cover               %             null Total cloud cover fraction based on maximum in model column within 30 km radius of a grid point

  t1=     0.6.3                                     LCDC               %             null Low-level cloud cover fraction based on maximum below 642 mb within 30 km radius of a grid point
  t2=     0.6.3                          Low cloud cover               %             null Low-level cloud cover fraction based on maximum below 642 mb within 30 km radius of a grid point

  t1=     0.6.4                                     MCDC               %             null Mid-level cloud cover fraction based on maximum between 642 mb and 350 mb within 30 km radius of a grid point
  t2=     0.6.4                       Medium cloud cover               %             null Mid-level cloud cover fraction based on maximum between 642 mb and 350 mb within 30 km radius of a grid point

  t1=     0.6.5                                     HCDC               %             null High-level cloud cover fraction based on maximum between 350 mb and 150 mb within 30 km radius of a grid point
  t2=     0.6.5                         High cloud cover               %             null High-level cloud cover fraction based on maximum between 350 mb and 150 mb within 30 km radius of a grid point

  t1=     0.7.6                                     CAPE            J/kg             null Most unstable CAPE (MUCAPE) using parcel with highest theta-e in lowest 300 mb
  t2=     0.7.6    Convective available potential energy            J/kg             null Most unstable CAPE (MUCAPE) using parcel with highest theta-e in lowest 300 mb

  t1=     0.7.7                                      CIN            J/kg             null Most unstable CIN (MUCIN) using parcel with highest theta-e in lowest 300 mb
  t2=     0.7.7                    Convective inhibition            J/kg             null Most unstable CIN (MUCIN) using parcel with highest theta-e in lowest 300 mb

  t1=     0.7.8                                     HLCY           m2/s2             null Storm-relative helicity for 0-1 km above ground level using Bunkers et al. 2000 storm-motion
  t2=     0.7.8                  Storm relative helicity            J/kg             null Storm-relative helicity for 0-1 km above ground level using Bunkers et al. 2000 storm-motion

  t1=    0.19.0                                      VIS               m             null Surface visibility
  t2=    0.19.0                               Visibility               m             null Surface visibility

  t1=     2.0.2                                    TSOIL               K             null Soil temperature 300 cm below surface
  t2=     2.0.2                         Soil temperature               K             null Soil temperature 300 cm below surface

  t1=    10.1.2                                    UOGRD             m/s             null U-component of wind at 80 m above ground level
  t2=    10.1.2                   u-component of current             m/s             null U-component of wind at 80 m above ground level

  t1=    10.1.3                                    VOGRD             m/s             null V-component of wind at 80 m above ground level
  t2=    10.1.3                   v-component of current             m/s             null V-component of wind at 80 m above ground level


Conflicts=45 extra=0

Table 2 :

extra=0


also a few random problems i see:

1) duplicate names in table resources/grib2/local/Fsl-hrrr2.csv
 DUPLICATE NAME 0.1.213 and 0.0.195 (Convective Initiation)
 DUPLICATE NAME 0.1.214 and 0.1.213 (Convective Initiation)
 DUPLICATE NAME 2.0.192 and 2.0.192 (Soil Moisture)
 DUPLICATE NAME 2.0.192 and 2.0.192 (Soil Moisture)
 DUPLICATE NAME 2.0.192 and 2.0.192 (Soil Moisture)
 DUPLICATE NAME 2.0.192 and 2.0.192 (Soil Moisture)
 DUPLICATE NAME 2.0.192 and 2.0.192 (Soil Moisture)
 DUPLICATE NAME 0.1.194 and 0.1.195 (Precipitation Type)
 DUPLICATE NAME 0.1.193 and 0.1.194 (Precipitation Type)
 DUPLICATE NAME 0.1.192 and 0.1.193 (Precipitation Type)
 DUPLICATE NAME 0.4.192 and 0.5.193 (Radiation)
 DUPLICATE NAME 0.2.195 and 0.2.194 (Storm Motion)
 DUPLICATE NAME 0.7.193 and 0.7.192 (Lifted Index)


2) the description is sometimes level dependent. here that fails:

 float Specific_humidity_sigma(time=1, sigma=50, y=1059, x=1799);
     :long_name = "Specific humidity @ Sigma level";
     :units = "kg/kg";
     :missing_value = NaNf; // float
     :description = "Specific humidity at 2 m above ground level";
     :grid_mapping = "LambertConformal_Projection";
     :Grib_Variable_Id = "VAR_0-1-0_L104";
     :Grib2_Parameter = 0, 1, 0; // int
     :Grib2_Level_Type = 104; // int
     :Grib2_Generating_Process_Type = "Forecast";


8/15/2012

The page for tables of WMO Table-Driven Code Forms (TDCF) has been moved to:
http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
Please change your link to the page.  Thank you.

4/29/2013

from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html

download
  http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/GRIB2_10_0_1.zip
  http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/BUFRCREX_19_1_1.zip
  http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/Common_20130226.zip

  DIFFERENCES of GRIB2_10_0_1 with standard WMO table (8-0-0)
  **0 0 6 (358)
   this =Dewpoint temperature
   wmo=Dew-point temperature
  **0 0 7 (359)
   this =Dewpoint depression (or deficit)
   wmo=Dew-point depression (or deficit)
  **0 0 14 (366)
   this =Minimum dewpoint depression
   wmo=Minimum dew-point depression
  **0 1 39 (415)
   this =Per cent frozen precipitation
   wmo=Percent frozen precipitation
  **0 1 85 (461)
   this =Specific rainwater content
   wmo=Specific rain water content
  **0 2 0 (470)
   this =Wind direction (from which blowing)
   wmo=Wind direction (from which blowing) (degree true)
   units this='degree.true' wmo='deg'
   NEW 0 2 34 Normal wind component (m.s-1)
   NEW 0 2 35 Tangential wind component (m.s-1)
   NEW 0 3 26 Exner pressure ()
  **0 5 5 (563)
   this =Net long-wave radiation flux
   wmo=Net long wave radiation flux
  **0 6 28 (596)
   this =Number of cloud droplets per unit mass of air
   wmo=Number concentration of cloud droplets
  **0 6 29 (597)
   this =Number of cloud ice particles per unit mass of air
   wmo=Number concentration of cloud ice
  **0 6 31 (599)
   this =Number density of cloud ice particles
   wmo=Number density of cloud ice
   NEW 0 6 34 Surface long-wave effective total cloudiness ()
   NEW 0 6 35 Surface short-wave effective total cloudiness ()
  **0 15 3 (639)
   this =Vertically integrated liquid water (VIL)
   wmo=Vertically-integrated liquid
  **0 19 25 (705)
   this =Weather
   wmo=Weather Interpretation ww (WMO)
   units this='(Code.table.4.225)' wmo=''
   NEW 0 19 27 Icing scenario ((Code.table.4.227))
  **0 20 101 (739)
   this =Vertical visual range
   wmo=Atmosphere optical thickness
  **1 2 11 (789)
   this =Attenuation coefficient of water with respect to solar radiation
   wmo=Attenuation coefficient of water with respect to solar attenuation coefficient of water with respect to solar radiation
  **2 0 29 (820)
   this =Evergreen forest cover
   wmo=Evergreen forest
  **2 0 30 (821)
   this =Deciduous forest cover
   wmo=Deciduous forest
   NEW 2 4 3 Fire burned area (%)
  **10 0 4 (992)
   this =Direction of wind waves
   wmo=Direction of wind waves (degree true)
   units this='degree.true' wmo='deg'
  **10 0 7 (995)
   this =Direction of swell waves
   wmo=Direction of swell waves (degree true)
   units this='degree.true' wmo='deg'
  **10 0 10 (998)
   this =Primary wave direction
   wmo=Primary wave direction (degree true)
   units this='degree.true' wmo='deg'
  **10 0 12 (1000)
   this =Secondary wave direction
   wmo=Secondary wave direction (degree true)
   units this='degree.true' wmo='deg'
  **10 0 14 (1002)
   this =Direction of combined wind waves and swell
   wmo=Direction of combined wind waves and swell (degree true)
   units this='degree.true' wmo='deg'
  **10 0 19 (1007)
   this =Normalized wave stress
   wmo=Normalised wave stress
  **10 0 26 (1014)
   this =Inverse mean frequency of wind waves
   wmo=Inverse mean frequency of the wind waves
  **10 0 27 (1015)
   this =Inverse mean frequency of total swell
   wmo=Inverse mean frequency of the total swell
  **10 0 29 (1017)
   this =Mean zero-crossing period of wind waves
   wmo=Mean zero-crossing period of the wind waves
  **10 0 30 (1018)
   this =Mean zero-crossing period of total swell
   wmo=Mean zero-crossing period of the total swell
  **10 0 32 (1020)
   this =Directional width of wind waves
   wmo=Directional width of the wind waves
  **10 0 33 (1021)
   this =Directional width of total swell
   wmo=Directional width of the total swell
  **10 0 35 (1023)
   this =Peak period of wind waves
   wmo=Peak period of the wind waves
  **10 0 36 (1024)
   this =Peak period of total swell
   wmo=Peak period of the total swell
  **10 0 40 (1028)
   this =10-metre neutral wind speed over waves
   wmo=10 metre neutral wind speed over waves
  **10 0 41 (1029)
   this =10-metre wind direction over waves
   wmo=10 metre wind direction over waves
  **10 0 43 (1031)
   this =Kurtosis of the sea-surface elevation due to waves
   wmo=Kurtosis of the sea surface elevation due to waves
  **10 1 0 (1041)
   this =Current direction
   wmo=Current direction (degree true)
   units this='degree.true' wmo='deg'
  **10 2 2 (1050)
   this =Direction of ice drift
   wmo=Direction of ice drift (degree true)
   units this='degree.true' wmo='deg'

  Total=538 same=496 sameIgnoreCase=503 dif=35 unknown=0 new=7

8/2014  caron
  - downloaded and added WMO tables 13.0.1

10/6/2014 caron
  - refactor grib2 tables
  - use standardTableMap.txt

12/09/2014 caron - download from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
    The latest versions 14(.0.0) (GRIB edition 2) and 23(.0.0) (BUFR and CREX) are effective as from 5 November 2014.
    - downloaded and added WMO tables 14.0.0
    - modify ucar.nc2.grib.grib2.table.WmoCodeTable, ucar.nc2.grib.grib2.table.WmoTemplateTable

11/17/2016 sarms - download from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
    The latest versions 18(.0.0) (GRIB edition 2) and 23(.0.0) (BUFR and CREX) are effective as from 2 November 2016.
    - downloaded and added WMO tables 18.0.0
    - modify ucar.nc2.grib.grib2.table.WmoCodeTable, ucar.nc2.grib.grib2.table.WmoTemplateTable to use new 18.0.0 tables

12/01/2017 sarms - download from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
    The latest versions, 20(.0.0) (GRIB edition 2) and 29(.0.0) (BUFR and CREX), are effective as from 8 November 2017.
    - downloaded and added WMO tables 20.0.0 (CodeFlag in http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/GRIB2_20_0_0.zip,
      Template in http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/GRIB2_20_0_0_exp.zip)
    - modify ucar.nc2.grib.grib2.table.WmoCodeTable, ucar.nc2.grib.grib2.table.WmoTemplateTable to use new 20.0.0 tables
