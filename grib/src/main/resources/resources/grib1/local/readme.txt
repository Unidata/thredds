notes for tables in local subdir

 8/24/2011 Spot check on WRF AMPS (Grib1) showed it was using ncar_0_200.tab. Correct table is taken from
    http://www.mmm.ucar.edu/rt/amps/wrf_grib and is now named wrf_amps.wrf. These differ in all entries >= 248.
  - center=60,subcenter=255,version=2: Correspond with kevin manning (kmanning@ucar.edu) to get a subcenter assigned to
    AMPS and to properly version their tables.
  - "As far as I can tell from my repository history, aside from one correction (in late 2009) where the rain water and ice water were
     getting all confused, it looks like I've used the same table since at least early 2009."

 11/7/2011 afwa.tab has "Pressure" as param 131 but this conflicts with us of param no 1 in file Q:\cdmUnitTest\formats\grib1\us057g1010t04a000000000
   change to "Pressure AFWA"
