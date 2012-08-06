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
    t1=   0.0.195                    Convective Initiation         minutes             null Time since last convective initation (> 1 hr) based upon lightning threat 3 and no adjacent activity
    t2=   0.0.195                   Convective Initiation1         minutes             null Time since last convective initation (> 1 hr) based upon lightning threat 3 and no adjacent activity

    t1=   0.0.196                     Convective Acitivity         minutes             null Time since last convective activity based upon lightning threat 3
    t2=   0.0.196                    Convective Acitivity1         minutes             null Time since last convective activity based upon lightning threat 3

    t1=   0.0.201                     Convective Activitiy         minutes             null Time since last convective activity based upon vertical flux of rain and graupel
    t2=   0.0.201                    Convective Activitiy2         minutes             null Time since last convective activity based upon vertical flux of rain and graupel

    t1=   0.0.202                      Convective Activity         minutes             null Time since last convective activitiy based upon 35 dBZ reflectivity at -10C
    t2=   0.0.202                     Convective Activity3         minutes             null Time since last convective activitiy based upon 35 dBZ reflectivity at -10C

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

    t1=   0.2.194                             Storm Motion             m/s             null U-component of convective storm motion for right-moving cells using Bunkers et al. 2000
    t2=   0.2.194                           Storm Motion-U             m/s             null U-component of convective storm motion for right-moving cells using Bunkers et al. 2000

    t1=   0.2.195                             Storm Motion             m/s             null V-component of convective storm motion for right-moving cells using Bunkers et al. 2000
    t2=   0.2.195                           Storm Motion-V             m/s             null V-component of convective storm motion for right-moving cells using Bunkers et al. 2000


  Conflicts=12 extra=0

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


2) the desciption is sometimes level dependent. here that fails:

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
