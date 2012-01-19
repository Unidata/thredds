notes for tables in local subdir

 8/24/2011 Spot check on WRF AMPS (Grib1) showed it was using ncar_0_200.tab. Correct table is taken from
    http://www.mmm.ucar.edu/rt/amps/wrf_grib and is now named wrf_amps.wrf. These differ in all entries >= 248.
  - center=60,subcenter=255,version=2: Correspond with kevin manning (kmanning@ucar.edu) to get a subcenter assigned to
    AMPS and to properly version their tables.
  - "As far as I can tell from my repository history, aside from one correction (in late 2009) where the rain water and ice water were
     getting all confused, it looks like I've used the same table since at least early 2009."

 11/7/2011 afwa.tab has "Pressure" as param 131 but this conflicts with us of param no 1 in file Q:\cdmUnitTest\formats\grib1\us057g1010t04a000000000
   change to "Pressure AFWA"

 01/18/2012 WRF AMPS
 old (ncar_0_200.tab)
    248:CPRATE:Convective Precip Rate[kg/m^2/s]:RAINCV:7
    249:TMN:Ground Reservoir Temperature [K]:TMN:2
    250:SWHR:Solar radiative heating [K/s]::
    251:LWHR:Longwave radiative heating [K/s]::
    252:PSTAR:psfc - 100 [Pa]::
    253:TNCPCP:Total Large scale precipitation [kg/m^2]:RAINNC:2
    254:TACPCP:Total Convective precipitation [kg/m^2]:RAINC:2

 correct (wrf_amps.wrf):
     248 |                            Cloud Fraction |                    m  |               CLDFRC |
     249 |                          Inversion height |                    m  |               INVHGT |
     250 |                        Inversion strength |                    K  |               INVSTR |
     251 |                    Column integrated snow |                kg/m2  |               INTSNW |
     252 |              Column integrated rain water |                kg/m2  |               INTRNW |
     253 |               Column integrated cloud ice |                kg/m2  |               INTCLI |
     254 |      Column integrated cloud liquid water |                kg/m2  |               INTCLW |
