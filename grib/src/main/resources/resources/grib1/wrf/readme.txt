WRF GRIB1 table notes

9/7/2011 sent to use from wrfhelp
  "see attached. Here gribmap.txt is for grib1 output"

9/12/2011 download wrf version 3.3 from http://www.mmm.ucar.edu/wrf/users/download/get_sources.html
  - found gribmap.txt in the run directory
  - also grb2map.tbl, apparenty for grib2
  - in external directoy, there are io_grib1, io_grib2, io_grib_share. also io_netcdf etc
  - io_grib1 has wgrib and mel_grib1

9/12/2011 took apart gribmap.txt. In the NCEP "tab" format, so broke into files:
  - wrfN.tab, seems to be 255:255:N
  - wrf7-129.tab for 7:-1:129
  - wrf7-130.tab for 7:-1:130

9/12/2011
 - wrf7-129 agrees mostly with ncl/ncep_129_gtb.h, execpt latter has more entries. entry 142 seems to be an exception
 - wrf2 agrees mostly with local/wrf_amps.wrf (from amps group), execpt:
     248 udunits
        kg/m2/s
        m
      249 desc
        Ground Reservoir Temperature
        Inversion height
      249 udunits
        K
        m
      253 desc
        Cumulative Large scale precipitation
        Column integrated cloud ice
      254 desc
        Cumulative Convective precipitation
        Column integrated cloud liquid water
