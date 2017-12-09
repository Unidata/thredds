01/07/2012 : missing params in ncep fire weather
 - screenscrape all tables from http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
 - using  ucar.nc2.grib.grib2.table.NcepHtmlScraper
 - put into directory grib\src\main\resources\resources\grib2\ncep

01/09/2012 result of comparing with current NCEP parameter tables (NcepTable.main()) using WMO 8.0 tables

NcepTable{title='Temperature', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-0.shtml', tableName='Table4.2.0.0'}
  ud=    0.0.16 Wm-2 (wm-2) != W.m-2 for 0.0.16 (Snow Phase Change Heat Flux)

  ud=    0.0.20 m-2s-1 (m-2.s-1) != m2/s for 0.0.20 (Turbulent Diffusion Coefficient for Heat)

  ud=   0.0.204 J/m2K (1000.0 2k-1.kg.m2.s-2) != J/(m2.K) for 0.0.204 (Tropical Cyclone Heat Potential)

Conflicts=0 extra=0 udunits=3


NcepTable{title='Moisture', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml', tableName='Table4.2.0.1'}
  p1=     0.1.4                           Vapor Pressure              Pa             VAPP
  p2= 4.2.0.1.4                          Vapour pressure              Pa             null

  p1=    0.1.32                                  Grauple         kg.kg-1             GRLE
  p2=4.2.0.1.32                   Graupel (snow pellets)           kg/kg             null

  p1=    0.1.43      Rain Fraction of Total Liquid Water      Proportion            FRAIN
  p2=4.2.0.1.43       Rain fraction of total cloud water                             null

  p1=    0.1.51 Total Column Water (Vertically integrated total water (vapour+cloud water/ice)          kg.m-2            TCWAT
  p2=4.2.0.1.51 Total column water (Vertically integrated total water (vapour + cloud water/ice))          kg.m-2             null

  p1=    0.1.69       Total Column Integrate Cloud Water          kg.m-2            TCOLW
  p2=4.2.0.1.69      Total column integrated cloud water          kg.m-2             null

  p1=    0.1.70         Total Column Integrate Cloud Ice          kg.m-2            TCOLI
  p2=4.2.0.1.70        Total column integrated cloud ice          kg.m-2             null

  p1=    0.1.72              Total Column Integrate Hail          kg.m-2            TCOLH
  p2=4.2.0.1.72             Total column integrated hail          kg.m-2             null

  p1=    0.1.73                    Hail Prepitation Rate       kg.m-2m-1       Validation
  p2=4.2.0.1.73                  Hail precipitation rate      kg.m-2.s-1             null

  ud=    0.1.73 kg.m-2m-1 (kg.m-3) != kg.m-2.s-1 for 0.1.73 (Hail Prepitation Rate)

  p1=    0.1.74           Total Column Integrate Graupel          kg.m-2            TCOLG
  p2=4.2.0.1.74          Total column integrated graupel          kg.m-2             null

  p1=    0.1.75  Graupel (Snow Pellets) Prepitation Rate       kg.m-2m-1       Validation
  p2=4.2.0.1.75 Graupel (snow pellets) precipitation rate      kg.m-2.s-1             null

  ud=    0.1.75 kg.m-2m-1 (kg.m-3) != kg.m-2.s-1 for 0.1.75 (Graupel (Snow Pellets) Prepitation Rate)

  ud=    0.1.76 kg.m-2m-1 (kg.m-3) != kg.m-2.s-1 for 0.1.76 (Convective Rain Rate)

  ud=    0.1.77 kg.m-2m-1 (kg.m-3) != kg.m-2.s-1 for 0.1.77 (Large Scale Rain Rate)

  p1=    0.1.78 Total Column Integrate Water (All components including precipitation)          kg.m-2           TCOLWA
  p2=4.2.0.1.78 Total column integrated water (all components including precipitation)          kg.m-2             null

  ud=    0.1.79 kg.m-2m-1 (kg.m-3) != kg.m-2.s-1 for 0.1.79 (Evaporation Rate)

  p1=    0.1.81        Total Column-Integrate Condensate          kg.m-2       Validation
  p2=4.2.0.1.81       Total Column-Integrated Condensate          kg.m-2             null

Conflicts=12 extra=3 udunits=5


NcepTable{title='Aerosols', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-13.shtml', tableName='Table4.2.0.13'}
Conflicts=0 extra=0 udunits=0


NcepTable{title='Trace gases', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-14.shtml', tableName='Table4.2.0.14'}
  p1=  0.14.193                Ozone Concentration (PPB)             PPB            OZCON
  p2=  0.14.193                      Ozone Concentration             PPB            OZCON

  udunits cant parse=  0.14.202           ?g/m3         ug/(m3)
  udunits cant parse=  0.14.203           ?g/m3         ug/(m3)
Conflicts=1 extra=0 udunits=0


NcepTable{title='Radar', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-15.shtml', tableName='Table4.2.0.15'}
  ud=    0.15.3 kg.m-2 (kg.m-2) != kg/m for 0.15.3 (Vertically Integrated Liquid)

Conflicts=0 extra=0 udunits=1


NcepTable{title='Forecast Radar Imagery', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-16.shtml', tableName='Table4.2.0.16'}
  ud=    0.16.0 mm6m-3 (1.00000000000000013E18 6m-3) != mm6.m-3 for 0.16.0 (Equivalent radar reflectivity factor for rain)

  ud=    0.16.1 mm6m-3 (1.00000000000000013E18 6m-3) != mm6.m-3 for 0.16.1 (Equivalent radar reflectivity factor for snow)

  ud=    0.16.2 mm6m-3 (1.00000000000000013E18 6m-3) != mm6.m-3 for 0.16.2 (Equivalent radar reflectivity factor for parameterized convection)

  p1=    0.16.3                    Echo Top (See Note 1)               m            RETOP
  p2=4.2.0.16.3                                 Echo top               m             null

  p1=  0.16.192 Equivalent radar reflectivity factor for rain          mm6/m3            REFZR
  p2=  0.16.192 Derived radar reflectivity backscatter from rain          mm6/m3            REFZR

  p1=  0.16.193 Equivalent radar reflectivity factor for snow          mm6/m3            REFZI
  p2=  0.16.193 Derived radar reflectivity backscatter from ice          mm6/m3            REFZI

  p1=  0.16.194 Equivalent radar reflectivity factor for parameterized convection          mm6/m3            REFZC
  p2=  0.16.194 Derived radar reflectivity backscatter from parameterized convection          mm6/m3            REFZC

  p1=  0.16.195                             Reflectivity              dB             REFD
  p2=  0.16.195               Derived radar reflectivity              dB             REFD

  p1=  0.16.196                   Composite reflectivity              dB             REFC
  p2=  0.16.196   Maximum / Composite radar reflectivity              dB             REFC

  p1=  0.16.197                    Echo Top (See Note 1)               m            RETOP
  p2=  0.16.197                Radar Echo Top (18.3 DBZ)               m            RETOP

Conflicts=7 extra=1 udunits=3


NcepTable{title='Nuclear/radiology', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-18.shtml', tableName='Table4.2.0.18'}
  p1=    0.18.0          Air Concentration of Cesium 137          Bq.m-3            ACCES
  p2=4.2.0.18.0         Air concentration of Caesium 137          Bq.m-3             null

  p1=    0.18.3          Ground Deposition of Cesium 137          Bq.m-2            GDCES
  p2=4.2.0.18.3         Ground deposition of Caesium 137          Bq.m-2             null

  p1=    0.18.6 Time Integrated Air Concentration of Cesium Pollutant*          Bq.m-3           TIACCP
  p2=4.2.0.18.6 Time-integrated air concentration of caesium pollutant        Bq.s.m-3             null

  ud=    0.18.6 Bq.m-3 (m-3.s-1) != Bq.s.m-3 for 0.18.6 (Time Integrated Air Concentration of Cesium Pollutant*)

  ud=    0.18.7 Bq.m-3 (m-3.s-1) != Bq.s.m-3 for 0.18.7 (Time Integrated Air Concentration of Iodine Pollutant*)

  ud=    0.18.8 Bq.m-3 (m-3.s-1) != Bq.s.m-3 for 0.18.8 (Time Integrated Air Concentration of Radioactive Pollutant*)

  p1=   0.18.12             Total Deposition (Wet + Dry)          Bq.m-2           TOTLWD
  p2=4.2.0.18.12                           Dry deposition          Bq.m-2             null

Conflicts=4 extra=0 udunits=3


NcepTable{title='Physical atmospheric properties', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-19.shtml', tableName='Table4.2.0.19'}
  p1=   0.19.17            Maximum Snow Albedosee Note 2               %           MXSALB
  p2=4.2.0.19.17                      Maximum snow albedo               %             null

  p1=   0.19.23 Supercooled Large Droplet (SLD) Probabilitysee Note 2               %             SLDP
  p2=4.2.0.19.23    Supercooled large droplet probability               %             null

  p1=  0.19.206                     Confidence - Ceiling                            CICEL
  p2=  0.19.206                       Confidence Ceiling                            CICEL

  p1=  0.19.207                  Confidence - Visibility                            CIVIS
  p2=  0.19.207                    Confidence Visibility                            CIVIS

  p1=  0.19.208             Confidence - Flight Category                            CIFLT
  p2=  0.19.208               Confidence Flight Category                            CIFLT

  p1=  0.19.217 Supercooled Large Droplet (SLD) Icingsee Note 2 See.Table.4.207             SIPD
  p2=  0.19.217          Supercooled Large Droplet Icing 0=None;.1=Light;.2=Moderate;.3=Severe;.4=Trace;.5=Heavy;.255=missing             SIPD

  p1=  0.19.220          Categorical Severe Thunderstorm Code.table.4.222            SVRTS
  p2=  0.19.220                                 Reserved

  p1=  0.19.221                Probability of Convection               %           PROCON
  p2=  0.19.221                                 Reserved

  ud=  0.19.221 % !=  for 0.19.221 (Probability of Convection)

  p1=  0.19.222                     Convection Potential Code.table.4.222            CONVP
  p2=  0.19.222                                 Reserved

Conflicts=9 extra=2 udunits=1


NcepTable{title='CCITT IA5 string', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-190.shtml', tableName='Table4.2.0.190'}
  ud=   0.190.0 CCITTIA5 (ccittia5) != CCITT.IA5 for 0.190.0 (Arbitrary Text String)

Conflicts=0 extra=0 udunits=1


NcepTable{title='Miscellaneous', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-191.shtml', tableName='Table4.2.0.191'}
  p1= 0.191.192                    Latitude (-90 to +90)             deg             NLAT
  p2= 0.191.192                     Latitude (-90 to 90)             deg             NLAT

  p1= 0.191.193                 East Longitude (0 - 360)             deg             ELON
  p2= 0.191.193                East Longitude (0 to 360)             deg             ELON

  p1= 0.191.196 Latitude (nearest neighbor) (-90 to +90)             deg            NLATN
  p2= 0.191.196  Latitude (nearest neighbor) (-90 to 90)             deg            NLATN

  p1= 0.191.197 East Longitude (nearest neighbor) (0 - 360)             deg            ELONN
  p2= 0.191.197 East longitude (nearest neighbor) (0 to 360)             deg            ELONN

Conflicts=4 extra=0 udunits=0


NcepTable{title='Covariance', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-192.shtml', tableName='Table4.2.0.192'}
  p1=   0.192.1 Covariance between zonal and meridional components of the wind. Defined as [uv]-[u][v], where "[]" indicates the mean over the indicated time span.           m2/s2            COVMZ
  p2=   0.192.1 Covariance between zonal and meridional components of the wind           m2/s2            COVZM

  p1=   0.192.2 Covariance between izonal component of the wind and temperature. Defined as [uT]-[u][T], where "[]" indicates the mean over the indicated time span.           K.m/s            COVTZ
  p2=   0.192.2 Covariance between zonal component of the wind and temperature           K.m/s            COVTZ

  p1=   0.192.3 Covariance between meridional component of the wind and temperature. Defined as [vT]-[v][T], where "[]" indicates the mean over the indicated time span.           K.m/s            COVTM
  p2=   0.192.3 Covariance between meridional component of the wind and temperature           K.m/s            COVTM

  p1=   0.192.4 Covariance between temperature and vertical component of the wind. Defined as [wT]-[w][T], where "[]" indicates the mean over the indicated time span.           K.m/s            COVTW
  p2=   0.192.4 Covariance between temperature and vertical component of the wind           K.m/s            COVTW

  p1=   0.192.5 Covariance between zonal and zonal components of the wind. Defined as [uu]-[u][u], where "[]" indicates the mean over the indicated time span.           m2/s2            COVZZ
  p2=   0.192.5 Covariance between zonal and zonal components of the wind           m2/s2            COVZZ

  p1=   0.192.6 Covariance between meridional and meridional components of the wind. Defined as [vv]-[v][v], where "[]" indicates the mean over the indicated time span.           m2/s2            COVMM
  p2=   0.192.6 Covariance between meridional and meridional components of the wind           m2/s2            COVMM

  p1=   0.192.7 Covariance between specific humidity and zonal components of the wind. Defined as [uq]-[u][q], where "[]" indicates the mean over the indicated time span.       kg/kg.m/s            COVQZ
  p2=   0.192.7 Covariance between specific humidity and zonal components of the wind       kg/kg.m/s            COVQZ

  p1=   0.192.8 Covariance between specific humidity and meridional components of the wind. Defined as [vq]-[v][q], where "[]" indicates the mean over the indicated time span.       kg/kg.m/s            COVQM
  p2=   0.192.8 Covariance between specific humidity and meridional components of the wind       kg/kg.m/s            COVQM

  p1=   0.192.9 Covariance between temperature and vertical components of the wind. Defined as [?T]-[?][T], where "[]" indicates the mean over the indicated time span.          K.Pa/s           COVTVV
  p2=   0.192.9 Covariance between temperature and vertical components of the wind          K.Pa/s           COVTVV

  p1=  0.192.10 Covariance between specific humidity and vertical components of the wind. Defined as [?q]-[?][q], where "[]" indicates the mean over the indicated time span.      kg/kg.Pa/s           COVQVV
  p2=  0.192.10 Covariance between specific humidity and vertical components of the wind      kg/kg.Pa/s           COVQVV

  p1=  0.192.11 Covariance between surface pressure and surface pressure. Defined as [Psfc]-[Psfc][Psfc], where "[]" indicates the mean over the indicated time span.           Pa.Pa          COVPSPS
  p2=  0.192.11 Covariance between surface pressure and surface pressure           Pa.Pa          COVPSPS

  p1=  0.192.12 Covariance between specific humidity and specific humidy. Defined as [qq]-[q][q], where "[]" indicates the mean over the indicated time span.     kg/kg.kg/kg            COVQQ
  p2=  0.192.12 Covariance between specific humidity and specific humidity     kg/kg.kg/kg            COVQQ

  p1=  0.192.13 Covariance between vertical and vertical components of the wind. Defined as [??]-[?][?], where "[]" indicates the mean over the indicated time span.          Pa2/s2          COVVVVV
  p2=  0.192.13 Covariance between vertical and vertical components of the wind          Pa2/s2          COVVVVV

  p1=  0.192.14 Covariance between temperature and temperature. Defined as [TT]-[T][T], where "[]" indicates the mean over the indicated time span.             K.K            COVTT
  p2=  0.192.14 Covariance between temperature and temperature             K.K            COVTT

Conflicts=14 extra=0 udunits=0


NcepTable{title='Momentum', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-2.shtml', tableName='Table4.2.0.2'}
  p1=     0.2.0      Wind Direction (from which blowing)                             WDIR
  p2= 4.2.0.2.0 Wind direction (from which blowing) (degree true)             deg             null

  ud=     0.2.0  != deg for 0.2.0 (Wind Direction (from which blowing))

  ud=     0.2.6 m2.s-1 (m2.s-1) != m2.s-2 for 0.2.6 (Montgomery Stream Function)

  p1=    0.2.15            Vertical U-Component of Shear             s-1            VUCSH
  p2=4.2.0.2.15               Vertical u-component shear             1/s             null

  p1=    0.2.16            Vertical V-Component of Shear             s-1            VVCSH
  p2=4.2.0.2.16               Vertical v-component shear             1/s             null

  p1=    0.2.25                     Vertical speed sheer             s-1             VWSH
  p2=4.2.0.2.25                     Vertical speed shear             1/s             null

  ud=    0.2.31 m2s-1 (1000.0 2s-1) != m2/s for 0.2.31 (Turbulent Diffusion Coefficient for Momentum)

  p1=   0.2.192                     Vertical speed sheer             s-1            VW SH
  p2=   0.2.192                     Vertical speed sheer             1/s             VWSH

  p1=   0.2.202                Latitude of Presure Point             deg             LAPP
  p2=   0.2.202               Latitude of Pressure Point             deg             LAPP

  p1=   0.2.203               Longitude of Presure Point             deg             LOPP
  p2=   0.2.203              Longitude of Pressure Point             deg             LOPP

  ud=   0.2.219 1/s/m (m-1.s-1) != 1/(s/m) for 0.2.219 (Potential Vorticity (Mass-Weighted))

Conflicts=7 extra=5 udunits=4


NcepTable{title='Atmospheric Chemical Constituents', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-20.shtml', tableName='Table4.2.0.20'}
  p1=    0.20.1 Column-Integrated Mass Density (See Note 1)          kg.m-2            COLMD
  p2=4.2.0.20.1           Column-integrated mass density          kg.m-2             null

  p1=    0.20.5 Atmosphere Net Production And Emision Mass Flux       kg.m-2s-1         ANPEMFLX
  p2=4.2.0.20.5 Atmosphere net production and emission mass flux      kg.m-2.s-1             null

  p1=   0.20.56 Changes Of Amount in Atmosphere (See Note 1)         mol.s-1            COAIA
  p2=4.2.0.20.56          Changes of amount in atmosphere           mol/s             null

  p1=   0.20.58 Total Yearly Average Atmospheric Loss (See Note 1)         mol.s-1            TYAAL
  p2=4.2.0.20.58   Total yearly averaged atmospheric loss           mol/s             null

Conflicts=4 extra=0 udunits=0


NcepTable{title='Mass', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-3.shtml', tableName='Table4.2.0.3'}
  p1=    0.3.21        Angle Of Sub-Grid Scale Orography             Rad           AOSGSO
  p2=4.2.0.3.21         Angle of sub-gridscale orography             rad             null

  p1=    0.3.22        Slope Of Sub-Grid Scale Orography         Numeric            SSGSO
  p2=4.2.0.3.22         Slope of sub-gridscale orography                             null

  p1=    0.3.23      Gravity Of Sub-Grid Scale Orography           W.m-2            GSGSO
  p2=4.2.0.3.23                 Gravity wave dissipation           W.m-2             null

  p1=    0.3.24   Anisotropy Of Sub-Grid Scale Orography         Numeric            ASGSO
  p2=4.2.0.3.24    Anisotropy of sub-gridscale orography                             null

Conflicts=4 extra=0 udunits=0


NcepTable{title='Short wave radiation', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-4.shtml', tableName='Table4.2.0.4'}
  p1=     0.4.6    Radiance (with respect to wavelength)      W.m-3.sr-1            SWRAD
  p2= 4.2.0.4.6   Radiance (with respect to wave length)      W.m-3.sr-1             null

  ud=    0.4.51 W.m-2 !=  for 0.4.51 (UV Index**)

  p1=   0.4.192       Downward Short-Wave Radiation Flux           W.m-2            DSWRF
  p2=   0.4.192            Downward Short-Wave Rad. Flux          W/(m2)            DSWRF

  p1=   0.4.193         Upward Short-Wave Radiation Flux           W.m-2            USWRF
  p2=   0.4.193              Upward Short-Wave Rad. Flux          W/(m2)            USWRF

Conflicts=3 extra=0 udunits=1


NcepTable{title='Long wave radiation', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-5.shtml', tableName='Table4.2.0.5'}
  p1=     0.5.3             Downward Long-Wave Rad. Flux           W.m-2            DLWRF
  p2= 4.2.0.5.3        Downward long-wave radiation flux           W.m-2             null

  p1=     0.5.4               Upward Long-Wave Rad. Flux           W.m-2            ULWRF
  p2= 4.2.0.5.4          Upward long-wave radiation flux           W.m-2             null

Conflicts=2 extra=0 udunits=0


NcepTable{title='Cloud', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-6.shtml', tableName='Table4.2.0.6'}
Conflicts=0 extra=0 udunits=0


NcepTable{title='Thermodynamic stability indices', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-7.shtml', tableName='Table4.2.0.7'}
  p1=     0.7.0          Parcel Lifted Index (to 500 mb)               K              PLI
  p2= 4.2.0.7.0         Parcel lifted index (to 500 hPa)               K             null

  p1=     0.7.1            Best Lifted Index (to 500 mb)               K              BLI
  p2= 4.2.0.7.1           Best lifted index (to 500 hPa)               K             null

  ud=    0.7.15 m2s-2 (1000000.0 2s-2) != m2.s-2 for 0.7.15 (Updraft Helicity)

  p1=   0.7.192                     Surface Lifted Index               K            LFT X
  p2=   0.7.192                     Surface Lifted Index               K             LFTX

  ud=   0.7.197 m2s-2 (1000000.0 2s-2) != m2/s2 for 0.7.197 (Updraft Helicity)

Conflicts=3 extra=1 udunits=2


NcepTable{title='Hydrology basic', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-1-0.shtml', tableName='Table4.2.1.0'}
  p1=     1.0.4  Snow Water Equivalent Percent of Normal               %           SWEPON
  p2= 4.2.1.0.4 Snow water equivalent per cent of normal               %             null

Conflicts=1 extra=0 udunits=0


NcepTable{title='Hydrology probabilities', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-1-1.shtml', tableName='Table4.2.1.1'}
  p1=     1.1.0 Conditional percent precipitation amount fractile for an overall period (encoded as an accumulation)          kg.m-2            CPPOP
  p2= 4.2.1.1.0 Conditional per cent precipitation amount fractile for an overall period (Encoded as an accumulation)          kg.m-2             null

  p1=     1.1.1 Percent Precipitation in a sub-period of an overall period (encoded as a percent accumulation over the sub-period)               %            PPOSP
  p2= 4.2.1.1.1 Per cent precipitation in a sub-period of an overall period (Encoded as per cent accumulation over the sub-period)               %             null

  p1=   1.1.195 Probability of Wetting Rain, exceeding in 0.10" in a given time period               %              CWR
  p2=   1.1.195 Probability of Wetting Rain; exceeding in 0.1 inch in a given time period               %              CWR

Conflicts=3 extra=0 udunits=0


NcepTable{title='Waves', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-0.shtml', tableName='Table4.2.10.0'}
  p1=    10.0.4                  Direction of Wind Waves                            WVDIR
  p2=4.2.10.0.4    Direction of wind waves (degree true)             deg             null

  ud=    10.0.4  != deg for 10.0.4 (Direction of Wind Waves)

  p1=    10.0.7                 Direction of Swell Waves                            SWDIR
  p2=4.2.10.0.7   Direction of swell waves (degree true)             deg             null

  ud=    10.0.7  != deg for 10.0.7 (Direction of Swell Waves)

  p1=   10.0.10                   Primary Wave Direction                            DIRPW
  p2=4.2.10.0.10     Primary wave direction (degree true)             deg             null

  ud=   10.0.10  != deg for 10.0.10 (Primary Wave Direction)

  p1=   10.0.12                 Secondary Wave Direction                            DIRSW
  p2=4.2.10.0.12   Secondary wave direction (degree true)             deg             null

  ud=   10.0.12  != deg for 10.0.12 (Secondary Wave Direction)

  p1=   10.0.14 Direction of Combined Wind Waves and Swell                           WWSDIR
  p2=4.2.10.0.14 Direction of combined wind waves and swell (degree true)             deg             null

  ud=   10.0.14  != deg for 10.0.14 (Direction of Combined Wind Waves and Swell)

  p1=   10.0.18                             Waves Stress           N.m-2       Validation
  p2=4.2.10.0.18                              Wave stress           N.m-2             null

  p1=   10.0.19                   Normalise Waves Stress                       Validation
  p2=4.2.10.0.19                   Normalised wave stress                             null

  p1=   10.0.20              iMean Square Slope of Waves                       Validation
  p2=4.2.10.0.20               Mean square slope of waves                             null

  p1=   10.0.31                     Wave Direction Width                       Validation
  p2=4.2.10.0.31                   Wave directional width                             null

  p1=   10.0.33           Directional Width Of The Swell                       Validation
  p2=4.2.10.0.33     Directional width of the total swell                             null

  p1=   10.0.40   10 Meter Neutral Wind Speed Over Waves            ms-1       Validation
  p2=4.2.10.0.40   10 metre neutral wind speed over waves           m.s-1             null

  ud=   10.0.40 ms-1 (1000.0 s-1) != m.s-1 for 10.0.40 (10 Meter Neutral Wind Speed Over Waves)

  p1=   10.0.41       10 Meter Wind Direction Over Waves                       Validation
  p2=4.2.10.0.41       10 metre wind direction over waves             deg             null

  ud=   10.0.41  != deg for 10.0.41 (10 Meter Wind Direction Over Waves)

  p1=   10.0.42                     Wave Engery Spectrum      m-2s.rad-1       Validation
  p2=4.2.10.0.42                     Wave energy spectrum      m2.s.rad-1             null

  ud=   10.0.42 m-2s.rad-1 (m-2.rad-1.s) != m2.s.rad-1 for 10.0.42 (Wave Engery Spectrum)

  p1=   10.0.46 2-Dimension Spectral Energy Density E(f,?)           m.s-2       Validation
  p2=4.2.10.0.46   2-dim spectral energy density E (f, ?)            m2.s             null

  ud=   10.0.46 m.s-2 (m.s-2) != m2.s for 10.0.46 (2-Dimension Spectral Energy Density E(f,?))

  p1=   10.0.47 Frequency Spectral Energy Density E(f)=?E(f,?)d?           m.s-2       Validation
  p2=4.2.10.0.47 Frequency spectral energy density E (f) = ? E (f,?) d?            m2.s             null

  ud=   10.0.47 m.s-2 (m.s-2) != m2.s for 10.0.47 (Frequency Spectral Energy Density E(f)=?E(f,?)d?)

  p1=   10.0.48 Directional Spectral Energy Density E(?)=?E(f,?)d?/m0           m.s-2       Validation
  p2=4.2.10.0.48 Directional spectral energy density E (?)= ? E (f,?) df / m0                             null

  ud=   10.0.48 m.s-2 !=  for 10.0.48 (Directional Spectral Energy Density E(?)=?E(f,?)d?/m0)

  ud=  10.0.192  != 0 for 10.0.192 (Wave Steepness)

Conflicts=16 extra=0 udunits=12


NcepTable{title='Currents', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-1.shtml', tableName='Table4.2.10.1'}
  p1=    10.1.0                        Current Direction     Degree.True            DIR C
  p2=4.2.10.1.0          Current direction (degree true)             deg             null

  ud=    10.1.0 Degree.True (1.7453292519943295E10 rad.rue) != deg for 10.1.0 (Current Direction)

Conflicts=1 extra=0 udunits=1


NcepTable{title='Miscellaneous', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-191.shtml', tableName='Table4.2.10.191'}
  ud=  10.191.1 m3s-1 (1000.0 3s-1) != m3/s for 10.191.1 (Meridional Overturning Stream Function)

Conflicts=0 extra=0 udunits=1


NcepTable{title='Ice', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-2.shtml', tableName='Table4.2.10.2'}
  p1=    10.2.2                   Direction of Ice Drift     Degree.True            DICED
  p2=4.2.10.2.2     Direction of ice drift (degree true)             deg             null

  ud=    10.2.2 Degree.True (1.7453292519943295E10 rad.rue) != deg for 10.2.2 (Direction of Ice Drift)

Conflicts=1 extra=0 udunits=1


NcepTable{title='Surface properties', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-3.shtml', tableName='Table4.2.10.3'}
  p1=  10.3.192                              Storm Surge               m            SURGE
  p2=  10.3.192                    Hurricane Storm Surge               m            SURGE

  ud=  10.3.199 degree.per.day (1.5079644737231007E-9 er.rad.s) != degree/day for 10.3.199 (Surface Temperature Trend)

  ud=  10.3.200 psu.per.day (8.639999999999999E-20 er.s.su) != psu/day for 10.3.200 (Surface Salinity Trend)

Conflicts=1 extra=0 udunits=2


NcepTable{title='Sub-surface properties', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-10-4.shtml', tableName='Table4.2.10.4'}
  ud=    10.4.4 m2s-1 (1000.0 2s-1) != m2.s-1 for 10.4.4 (Ocean Vertical Heat Diffusivity)

  ud=    10.4.5 m2s-1 (1000.0 2s-1) != m2.s-1 for 10.4.5 (Ocean Vertical Salt Diffusivity)

  ud=    10.4.6 m2s-1 (1000.0 2s-1) != m2.s-1 for 10.4.6 (Ocean Vertical Momentum Diffusivity)

  udunits cant parse=  10.4.192               c           deg.C
  p1=  10.4.194               Barotropic Kinectic Energy          J.kg-1            BKENG
  p2=  10.4.194                Barotropic Kinetic Energy            J/kg            BKENG

Conflicts=1 extra=0 udunits=3


NcepTable{title='Vegetation/Biomass', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-2-0.shtml', tableName='Table4.2.2.0'}
  p1=     2.0.0               Land Cover (0=sea, 1=land)      Proportion             LAND
  p2= 4.2.2.0.0           Land cover (0 = sea, 1 = land)                             null

  p1=    2.0.14          Blackadar's Mixing Length Scale               m            BMIXL
  p2=4.2.2.0.14          Blackadar�s mixing length scale               m             null

  p1=    2.0.19 Temperature parameter in canopy conductance      Proportion              RCT
  p2=4.2.2.0.19          Temperature parameter in canopy                             null

  p1=    2.0.20 Soil moisture parameter in canopy conductance      Proportion            RCSOL
  p2=4.2.2.0.20 Humidity parameter in canopy conductance                             null

  p1=    2.0.21 Humidity parameter in canopy conductance      Proportion              RCQ
  p2=4.2.2.0.21 Soil moisture parameter in canopy conductance                             null

  ud=    2.0.25 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.0.25 (Volumetric Soil Moisture)

  p1=    2.0.27              Volumetric Wilting Moisture           m3m-3           VWILTM
  p2=4.2.2.0.27                 Volumetric wilting point          m3.m-3             null

  ud=    2.0.27 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.0.27 (Volumetric Wilting Moisture)

  p1=    2.0.31 Normalized Differential Vegetation Index         Numeric       Validation
  p2=4.2.2.0.31 Normalized differential vegetation index (NDVI)                             null

  p1=   2.0.197           Blackadars Mixing Length Scale               m            BMIXL
  p2=   2.0.197          Blackadar's Mixing Length Scale               m            BMIXL

  udunits cant parse=   2.0.198  Integer.(0-13)           0..13
  ud=   2.0.206  != unknown for 2.0.206 (Rate of water dropping from canopy to ground)

  ud=   2.0.211 Kg/m2 (kg.m-2) != K.g/m2 for 2.0.211 (Surface water storage)

  ud=   2.0.212 Kg/m2 (kg.m-2) != K.g/m2 for 2.0.212 (Liquid soil moisture content (non-frozen))

  p1=   2.0.218 Land-sea coverage (nearest neighbor) [land=1,sea=0]                            LANDN
  p2=   2.0.218     Land-sea coverage (nearest neighbor)   0=sea;.1=land            LANDN

  p1=   2.0.222 Water Vapor Flux Convergance (Vertical Int)           kg/m2           WVCONV
  p2=   2.0.222 Water vapor flux convergence (vertical int)           kg/m2           WVCONV

  p1=   2.0.223 Water Condensate Flux Convergance (Vertical Int)           kg/m2           WCCONV
  p2=   2.0.223 Water condensate flux convergence (vertical int)           kg/m2           WCCONV

Conflicts=11 extra=0 udunits=5


NcepTable{title='Soil', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-2-3.shtml', tableName='Table4.2.2.3'}
  ud=    2.3.10 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.3.10 (Liquid Volumetric Soil Moisture (Non-Frozen))

  p1=    2.3.11 Volumetric Transpiration Stree-Onset(Soil Moisture)           m3m-3           VOLTSO
  p2=4.2.2.3.11 Volumetric transpiration stress-onset (soil moisture)          m3.m-3             null

  ud=    2.3.11 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.3.11 (Volumetric Transpiration Stree-Onset(Soil Moisture))

  p1=    2.3.12 Transpiration Stree-Onset(Soil Moisture)          kg.m-3           TRANSO
  p2=4.2.2.3.12 Transpiration stress-onset (soil moisture)          kg.m-3             null

  p1=    2.3.13 Volumetric Direct Evaporation Cease(Soil Moisture)           m3m-3           VOLDEC
  p2=4.2.2.3.13 Volumetric direct evaporation cease (soil moisture)          m3.m-3             null

  ud=    2.3.13 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.3.13 (Volumetric Direct Evaporation Cease(Soil Moisture))

  p1=    2.3.14  Direct Evaporation Cease(Soil Moisture)          kg.m-3            DIREC
  p2=4.2.2.3.14 Direct evaporation cease (soil moisture)          kg.m-3             null

  ud=    2.3.15 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.3.15 (Soil Porosity)

  ud=    2.3.16 m3m-3 (1.0E9 3m-3) != m3.m-3 for 2.3.16 (Volumetric Saturation Of Soil Moisture)

  ud=    2.3.17 kgm-3 (1.0E-9 gm-3) != kg.m-3 for 2.3.17 (Saturation Of Soil Moisture)

  ud=   2.3.198 W/m-2 (kg.m4.s-3) != W/m2 for 2.3.198 (Direct evaporation from bare soil)

Conflicts=4 extra=0 udunits=7


NcepTable{title='Fire Weather', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-2-4.shtml', tableName='Table4.2.2.4'}
Conflicts=0 extra=0 udunits=0


NcepTable{title='Image format', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-3-0.shtml', tableName='Table4.2.3.0'}
Conflicts=0 extra=0 udunits=0


NcepTable{title='Quantitative', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-3-1.shtml', tableName='Table4.2.3.1'}
  ud=     3.1.4 m-1 (m-1) != m/s for 3.1.4 (Estimated u-Component of Wind)

  ud=     3.1.5 m-1 (m-1) != m/s for 3.1.5 (Estimated v-Component of Wind)

  p1=     3.1.6                    Number Of Pixels Used         Numeric            NPIXU
  p2= 4.2.3.1.6                     Number of pixel used                             null

  ud=     3.1.7  != deg for 3.1.7 (Solar Zenith Angle)

  ud=     3.1.8  != deg for 3.1.8 (Relative Azimuth Angle)

  p1=    3.1.16 Clear Sky Radiance (with respect to wave number)       W.m-1sr-1          CSKYRAD
  p2=4.2.3.1.16 Cloudy radiance (with respect to wave number)      W.m-1.sr-1             null

  ud=    3.1.19 ms-1 (1000.0 s-1) != m/s for 3.1.19 (Wind Speed)

  p1=    3.1.20     Aerosol Optical Thickness at 0.635 m                            AOT06
  p2=4.2.3.1.20    Aerosol optical thickness at 0.635 ?m            null             null

  p1=    3.1.21     Aerosol Optical Thickness at 0.810 m                            AOT08
  p2=4.2.3.1.21    Aerosol optical thickness at 0.810 ?m            null             null

  p1=    3.1.22     Aerosol Optical Thickness at 1.640 m                            AOT16
  p2=4.2.3.1.22    Aerosol optical thickness at 1.640 ?m            null             null

  p1=    3.1.23                      Angstrom Coefficien                           ANGCOE
  p2=4.2.3.1.23                     Angstrom coefficient            null             null

  p1=   3.1.192 Scatterometer Estimated U Wind Component           m.s-1             USCT
  p2=   3.1.192           Scatterometer Estimated U Wind             m/s             USCT

  p1=   3.1.193 Scatterometer Estimated V Wind Component           m.s-1             VSCT
  p2=   3.1.193           Scatterometer Estimated V Wind             m/s             VSCT

Conflicts=8 extra=0 udunits=5


NcepTable{title='Forecast Satellite Imagery', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-3-192.shtml', tableName='Table4.2.3.192'}
  p1=   3.192.3 Simulated Brightness Temperature for GOES 12, Channel 6               K           SBT126
  p2=   3.192.3 Simulated Brightness Temperature for GOES 12, Channel 5               K           SBT125

  ud=   3.192.4 Byte != numeric for 3.192.4 (Simulated Brightness Counts for GOES 12, Channel 3)

  ud=   3.192.5 Byte != numeric for 3.192.5 (Simulated Brightness Counts for GOES 12, Channel 4)

  Missing Grib2Parameter{discipline=3, category=192, number=6, name='Simulated Brightness Temperature for GOES 11, Channel 2', unit='K', abbrev='SBT112'}
  Missing Grib2Parameter{discipline=3, category=192, number=7, name='Simulated Brightness Temperature for GOES 11, Channel 3', unit='K', abbrev='SBT113'}
  Missing Grib2Parameter{discipline=3, category=192, number=8, name='Simulated Brightness Temperature for GOES 11, Channel 4', unit='K', abbrev='SBT114'}
  Missing Grib2Parameter{discipline=3, category=192, number=9, name='Simulated Brightness Temperature for GOES 11, Channel 5', unit='K', abbrev='SBT115'}
  Missing Grib2Parameter{discipline=3, category=192, number=10, name='Simulated Brightness Temperature for AMSRE on Aqua, Channel 9', unit='K', abbrev='AMSRE9'}
  Missing Grib2Parameter{discipline=3, category=192, number=11, name='Simulated Brightness Temperature for AMSRE on Aqua, Channel 10', unit='K', abbrev='AMSRE10'}
  Missing Grib2Parameter{discipline=3, category=192, number=12, name='Simulated Brightness Temperature for AMSRE on Aqua, Channel 11', unit='K', abbrev='AMSRE11'}
  Missing Grib2Parameter{discipline=3, category=192, number=13, name='Simulated Brightness Temperature for AMSRE on Aqua, Channel 12', unit='K', abbrev='AMSRE12'}
Conflicts=1 extra=8 udunits=2


NcepTable{title='Temperature', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-0.shtml', tableName='Table4.2.4.0'}
  Missing Grib2Parameter{discipline=4, category=0, number=0, name='Temperature', unit='K', abbrev='TMP'}
  Missing Grib2Parameter{discipline=4, category=0, number=1, name='Electron Temperature', unit='K', abbrev='ELECTMP'}
  Missing Grib2Parameter{discipline=4, category=0, number=2, name='Proton Temperature', unit='K', abbrev='PROTTMP'}
  Missing Grib2Parameter{discipline=4, category=0, number=3, name='Ion Temperature', unit='K', abbrev='IONTMP'}
  Missing Grib2Parameter{discipline=4, category=0, number=4, name='Parallel Temperature', unit='K', abbrev='PRATMP'}
  Missing Grib2Parameter{discipline=4, category=0, number=5, name='Perpendicular Temperature', unit='K', abbrev='PRPTMP'}
Conflicts=0 extra=6 udunits=0


NcepTable{title='Momentum', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-1.shtml', tableName='Table4.2.4.1'}
  Missing Grib2Parameter{discipline=4, category=1, number=0, name='Velocity Magnitude (Speed)', unit='m.s-1', abbrev='SPEED'}
  Missing Grib2Parameter{discipline=4, category=1, number=1, name='1st Vector Component of Velocity (Coordinate system dependent)', unit='m.s-1', abbrev='VEL1'}
  Missing Grib2Parameter{discipline=4, category=1, number=2, name='2nd Vector Component of Velocity (Coordinate system dependent)', unit='m.s-1', abbrev='VEL2'}
  Missing Grib2Parameter{discipline=4, category=1, number=3, name='3rd Vector Component of Velocity (Coordinate system dependent)', unit='m.s-1', abbrev='VEL3'}
Conflicts=0 extra=4 udunits=0


NcepTable{title='Charged Particle Mass and Number', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-2.shtml', tableName='Table4.2.4.2'}
  Missing Grib2Parameter{discipline=4, category=2, number=0, name='Particle Number Density', unit='m-3', abbrev='PLSMDEN'}
  Missing Grib2Parameter{discipline=4, category=2, number=1, name='Electron Density', unit='m-3', abbrev='ELCDEN'}
  Missing Grib2Parameter{discipline=4, category=2, number=2, name='Proton Density', unit='m-3', abbrev='PROTDEN'}
  Missing Grib2Parameter{discipline=4, category=2, number=3, name='Ion Density', unit='m-3', abbrev='IONDEN'}
  Missing Grib2Parameter{discipline=4, category=2, number=4, name='Vertical Electron Content', unit='m-2', abbrev='VTEC'}
  Missing Grib2Parameter{discipline=4, category=2, number=5, name='HF Absorption Frequency', unit='Hz', abbrev='ABSFRQ'}
  Missing Grib2Parameter{discipline=4, category=2, number=6, name='HF Absorption', unit='dB', abbrev='ABSRB'}
  Missing Grib2Parameter{discipline=4, category=2, number=7, name='Spread F', unit='m', abbrev='SPRDF'}
  Missing Grib2Parameter{discipline=4, category=2, number=8, name='h'F', unit='m', abbrev='HPRIMF'}
  Missing Grib2Parameter{discipline=4, category=2, number=9, name='Critical Frequency', unit='Hz', abbrev='CRTFRQ'}
  Missing Grib2Parameter{discipline=4, category=2, number=10, name='Scintillation', unit='Numeric', abbrev='SCINT'}
Conflicts=0 extra=11 udunits=0


NcepTable{title='Electric and Magnetic Fields', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-3.shtml', tableName='Table4.2.4.3'}
  Missing Grib2Parameter{discipline=4, category=3, number=0, name='Magnetic Field Magnitude', unit='T', abbrev='BTOT'}
  Missing Grib2Parameter{discipline=4, category=3, number=1, name='1st Vector Component of Magnetic Field', unit='T', abbrev='BVEC1'}
  Missing Grib2Parameter{discipline=4, category=3, number=2, name='2nd Vector Component of Magnetic Field', unit='T', abbrev='BVEC2'}
  Missing Grib2Parameter{discipline=4, category=3, number=3, name='3rd Vector Component of Magnetic Field', unit='T', abbrev='BVEC3'}
  Missing Grib2Parameter{discipline=4, category=3, number=4, name='Electric Field Magnitude', unit='V.m-1', abbrev='ETOT'}
  Missing Grib2Parameter{discipline=4, category=3, number=5, name='1st Vector Component of Electric Field', unit='V.m-1', abbrev='EVEC1'}
  Missing Grib2Parameter{discipline=4, category=3, number=6, name='2nd Vector Component of Electric Field', unit='V.m-1', abbrev='EVEC2'}
  Missing Grib2Parameter{discipline=4, category=3, number=7, name='3rd Vector Component of Electric Field', unit='V.m-1', abbrev='EVEC3'}
Conflicts=0 extra=8 udunits=0


NcepTable{title='Energetic Particles', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-4.shtml', tableName='Table4.2.4.4'}
  Missing Grib2Parameter{discipline=4, category=4, number=0, name='Proton Flux (Differential)', unit='(m2.s.sr.eV)-1', abbrev='DIFPFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=1, name='Proton Flux (Integral)', unit='(m2.s.sr)-1', abbrev='INTPFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=2, name='Electron Flux (Differential)', unit='(m2.s.sr.eV)-1', abbrev='DIFEFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=3, name='Electron Flux (Integral)', unit='(m2.s.sr)-1', abbrev='INTEFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=4, name='Heavy Ion Flux (Differential)', unit='(m2.s.sr.eV/nuc)-1', abbrev='DIFIFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=5, name='Heavy Ion Flux (iIntegral)', unit='(m2.s.sr)-1', abbrev='INTIFLUX'}
  Missing Grib2Parameter{discipline=4, category=4, number=6, name='Cosmic Ray Neutron Flux', unit='h-1', abbrev='NTRNFUX'}
Conflicts=0 extra=7 udunits=0


NcepTable{title='Waves', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-5.shtml', tableName='Table4.2.4.5'}
Conflicts=0 extra=0 udunits=0


NcepTable{title='Solar Electromagnetic Emissions', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-6.shtml', tableName='Table4.2.4.6'}
  Missing Grib2Parameter{discipline=4, category=6, number=0, name='Integrated Solar Irradiance', unit='W.m-2', abbrev='TSI'}
  Missing Grib2Parameter{discipline=4, category=6, number=1, name='Solar X-ray Flux (XRS Long)', unit='W.m-2', abbrev='XLONG'}
  Missing Grib2Parameter{discipline=4, category=6, number=2, name='Solar X-ray Flux (XRS Short)', unit='W.m-2', abbrev='XSHRT'}
  Missing Grib2Parameter{discipline=4, category=6, number=3, name='Solar EUV Irradiance', unit='W.m-2', abbrev='EUVIRR'}
  Missing Grib2Parameter{discipline=4, category=6, number=4, name='Solar Spectral Irradiance', unit='W.m-2nm-1', abbrev='SPECIRR'}
  Missing Grib2Parameter{discipline=4, category=6, number=5, name='F10.7', unit='W.m-2Hz-1', abbrev='F107'}
  Missing Grib2Parameter{discipline=4, category=6, number=6, name='Solar Radio Emissions', unit='W.m-2Hz-1', abbrev='SOLRF'}
Conflicts=0 extra=7 udunits=0


NcepTable{title='Terrestrial Electromagnetic Emissions', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-7.shtml', tableName='Table4.2.4.7'}
  Missing Grib2Parameter{discipline=4, category=7, number=0, name='Limb Intensity', unit='m-2s-1', abbrev='LMBINT'}
  Missing Grib2Parameter{discipline=4, category=7, number=1, name='Disk Intensity', unit='m-2s-1', abbrev='DSKINT'}
  Missing Grib2Parameter{discipline=4, category=7, number=2, name='Disk Intensity Day', unit='m-2s-1', abbrev='DSKDAY'}
  Missing Grib2Parameter{discipline=4, category=7, number=3, name='Disk Intensity Night', unit='m-2s-1', abbrev='DSKNGT'}
Conflicts=0 extra=4 udunits=0


NcepTable{title='Imagery', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-8.shtml', tableName='Table4.2.4.8'}
  Missing Grib2Parameter{discipline=4, category=8, number=0, name='X-Ray Radiance', unit='W.sr-1m-2', abbrev='XRAYRAD'}
  Missing Grib2Parameter{discipline=4, category=8, number=1, name='EUV Radiance', unit='W.sr-1m-2', abbrev='EUVRAD'}
  Missing Grib2Parameter{discipline=4, category=8, number=2, name='H-Alpha Radiance', unit='W.sr-1m-2', abbrev='HARAD'}
  Missing Grib2Parameter{discipline=4, category=8, number=3, name='White Light Radiance', unit='W.sr-1m-2', abbrev='WHTRAD'}
  Missing Grib2Parameter{discipline=4, category=8, number=4, name='CaII-K Radiance', unit='W.sr-1m-2', abbrev='CAIIRAD'}
  Missing Grib2Parameter{discipline=4, category=8, number=5, name='White Light Coronagraph Radiance', unit='W.sr-1m-2', abbrev='WHTCOR'}
  Missing Grib2Parameter{discipline=4, category=8, number=6, name='Heliospheric Radiance', unit='W.sr-1m-2', abbrev='HELCOR'}
  Missing Grib2Parameter{discipline=4, category=8, number=7, name='Thematic Mask', unit='Numeric', abbrev='MASK'}
Conflicts=0 extra=8 udunits=0


NcepTable{title='Ion-Neutral Coupling', source='http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-4-9.shtml', tableName='Table4.2.4.9'}
  Missing Grib2Parameter{discipline=4, category=9, number=0, name='Pedersen Conductivity', unit='S.m-1', abbrev='SIGPED'}
  Missing Grib2Parameter{discipline=4, category=9, number=1, name='Hall Conductivity', unit='S.m-1', abbrev='SIGHAL'}
  Missing Grib2Parameter{discipline=4, category=9, number=2, name='Parallel Conductivity', unit='S.m-1', abbrev='SIGPAR'}
Conflicts=0 extra=3 udunits=0

01/18/2012 NAM_Firewxnest_20111231_1800.grib2
 - missing parameter 2-4-3, not in Table4.2.2.4

 <parameterMap>
  <table>Table4.2.2.4</table>
  <title>Fire Weather</title>
  <source>http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-2-4.shtml</source>
  <parameter code="0">
    <shortName>FIREOLK</shortName>
    <description>Fire Outlook</description>
    <units>See Table 4.224</units>
  </parameter>
  <parameter code="1">
    <shortName>FIREODT</shortName>
    <description>Fire Outlook Due to Dry Thunderstorm</description>
    <units>See Table 4.224</units>
  </parameter>
  <parameter code="2">
    <shortName>HINDEX</shortName>
    <description>Haines Index</description>
    <units>Numeric</units>
  </parameter>
</parameterMap>

 - email from boi.vuong@noaa.gov:
  "I find that the parameter 2-4-3 (Haines Index) now is parameter 2 in WMO version 8.
   The NAM fire weather nested  will take change in next implementation of cnvgrib (NCEP conversion program)."
  so i could modify the table to duplicate  2-4-2 to 2-4-3 for now (!)
  really its a defect that should be corrected on the dataset level.

01/23/2012 E:/datasets/cfsr/dss/flxf01.gdas.2008080100.grb2
 - missing parameter 2-0-209
  Discipline   2     = Land surface products
  Category   0       = Maintenance mode

 - missing in resources\grib2\ncep\Table4.2.2.0.xml
 - present in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-2-0.shtml
   so perhaps was updated or bug in scraper ?
 = messing up because didnt have an abbreviation (name). seems to be the only one. now fixed.

01/25/2012  RUC2 CONUS 20 km on pressure levels
 - Im seeing a parameter 0-19-242 not in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-19.shtml

 -reply from boi:
     The parameter 0-19-242 (Relative Humidity with Respect to Precipitable Water)  was in
     http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml

     It was a mistake in table conversion (from grib1 to grib2) in cnvgrib. It will be fixed in next implementation of cnvgrib in
     June or July, 2012.

     RHPW  in grib1 in table 129 parameter 230  and in grib2 in 0-1-242

  - so in NcepLocalParam, redirect
     0-19-242 -> 0-1-242
     2-4-3    -> 2-4-2


05/16/2013 NCDC fsanl-4 is using both 2-4-2 and 2-4-3, but ncep tables had them duplicated. rescrape the NCEP HTML GRIB2 pages

     - screenscrape all tables from http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
     - using  ucar.nc2.grib.grib2.table.NcepHtmlScraper
     - put into directory grib\src\main\resources\resources\grib2\ncep

 remove this crap in NcepLocalTables:

    /* email from boi.vuong@noaa.gov 1/19/2012
     "I find that the parameter 2-4-3 (Haines Index) now is parameter 2 in WMO version 8.
      The NAM fire weather nested  will take change in next implementation of cnvgrib (NCEP conversion program)."  */
    //if (makeHash(discipline, category, number) == makeHash(2,4,3))
    //  return getParameter(2,4,2);

    /* email from boi.vuong@noaa.gov 1/26/2012
     The parameter 0-19-242 (Relative Humidity with Respect to Precipitable Water)  was in http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-2-0-1.shtml
     It was a mistake in table conversion (from grib1 to grib2) in cnvgrib. It will be fixed in next implementation of cnvgrib in June or July, 2012.
     RHPW  in grib1 in table 129 parameter 230  and in grib2 in 0-1-242  */
   // if (makeHash(discipline, category, number) == makeHash(0, 19, 242))
   //   return getParameter(0, 1, 242);

 but likely some older files will now be wrong. yeah for GRIB!


09/10/2014 caron

 screen scraped NCEP again. Havent yet integerated, put into ncep/v13.0.0 to compare with old.

11/17/2016 sarms
  - updated ncep grib2 table using ucar/nc2/grib/grib2/table/NcepHtmlScraper.java

12/01/2017 sarms
  - updated ncep grib2 tables to v20.0.0 using ucar/nc2/grib/grib2/table/NcepHtmlScraper.java.
    Noticed a warning that "*** Cant parse 0-90 == 0-90 Elevation in increments of 100 m" on
    http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table4-216.shtml. Not sure how to interpret
    without an example file, so will leave out for now. Has been left out since 2015, when this
    table appeared.