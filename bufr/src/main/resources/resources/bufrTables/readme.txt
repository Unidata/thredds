BUFR Tables log

4/30/2013

 1) from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
    download http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/BUFRCREX_19_1_1.zip
    put xml files into /resources/bufrTables/wmo: BUFR_19_1_1*.xml

 2) http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/PrevVERSIONS/20120502/20120502.html
    get http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/PrevVERSIONS/20120502/BUFRCREX_18_0_0.zip

 3) tablelookup.csv
    add version 17, 18 and 19
    make 19 the default instead of resource:/resources/bufrTables/cypher/B_d00v13.htm

 4) remove cruft from  /resources/bufrTables/wmo

 Compare Current Table (resource:/resources/bufrTables/wmo/BUFRCREX_19_1_1_TableB_en.xml) to resource:/resources/bufrTables/wmo/BUFRCREX_16_0_0_TableB_E.xml
  0-0-26 bitWidth 48 != 16
  0-1-12 name
    Direction of motion of moving observing platform
    Direction of motion of moving observing platform degree true
  0-1-12 units
    degree true
    deg
 **No key 0-1-40 in second table; local=false
 **No key 0-1-79 in second table; local=false
  0-1-103 name
    IMO Number Unique Lloyd's register
    IMO Number Unique Lloyd's registry
  0-1-103 bitWidth 24 != 14
 **No key 0-2-17 in second table; local=false
  0-2-119 name
    RA-2 instrument operations
    Instrument operations
 **No key 0-2-139 in second table; local=false
  0-2-158 name
    RA-2 instrument
    RA-2 instruments
  0-2-159 name
    MWR instrument
    MWR instruments
 **No key 0-2-170 in second table; local=false
  0-5-21 name
    Bearing or azimuth
    Bearing or azimuth degree true
  0-5-21 units
    degree true
    deg
  0-5-22 name
    Solar azimuth
    Solar azimuth degree true
  0-5-22 units
    degree true
    deg
  0-8-79 name
    Product status
    Aviation product status
 **No key 0-10-79 in second table; local=false
  0-11-1 name
    Wind direction
    Wind direction degree true
  0-11-1 units
    degree true
    deg
  0-11-10 name
    Wind direction associated with wind speed which follows
    Wind direction associated with wind speed which follows degree true
  0-11-10 units
    degree true
    deg
  0-11-11 name
    Wind direction at 10 m
    Wind direction at 10 m degree true
  0-11-11 units
    degree true
    deg
  0-11-13 name
    Wind direction at 5 m
    Wind direction at 5 m degree true
  0-11-13 units
    degree true
    deg
  0-11-16 name
    Extreme counterclockwise wind direction of a variable wind
    Extreme counterclockwise wind direction of a variable wind degree true
  0-11-16 units
    degree true
    deg
  0-11-17 name
    Extreme clockwise wind direction of a variable wind
    Extreme clockwise wind direction of a variable wind degree true
  0-11-17 units
    degree true
    deg
  0-11-43 name
    Maximum wind gust direction
    Maximum wind gust direction degree true
  0-11-43 units
    degree true
    deg
  0-11-44 name
    Mean wind direction for surface - 1 500 m 5 000 feet
    Mean wind direction for surface - 1500 m 5000 feet degree true
  0-11-44 units
    degree true
    deg
  0-11-49 name
    Standard deviation of wind direction
    Standard deviation of wind direction degree true
  0-11-49 units
    degree true
    deg
  0-11-53 name
    Formal uncertainty in wind direction
    Formal uncertainty in wind direction degree true
  0-11-53 units
    degree true
    deg
  0-11-54 name
    Mean wind direction for 1 500 - 3 000 m
    Mean wind direction for 1500 m - 3000 m degree true
  0-11-54 units
    degree true
    deg
  0-11-55 name
    Mean wind speed for 1 500 - 3 000 m
    Mean wind speed for 1500 m - 3000 m
  0-11-81 name
    Model wind direction at 10 m
    Model wind direction at 10m degree true
  0-11-81 units
    degree true
    deg
  0-11-100 name
    Aircraft true airspeed
    True aircraft speed
  0-11-101 name
    Aircraft ground speed u-component
    Aircraft velocity u-component
  0-11-102 name
    Aircraft ground speed v-component
    Aircraft velocity v-component
  0-11-103 name
    Aircraft ground speed w-component
    Aircraft velocity w-component
  0-11-104 units
    degree true
    deg
  0-12-2 name
    Wet-bulb temperature
    Air temperature
  0-12-4 name
    Air temperature at 2 m
    Dry-bulb temperature at 2 m
  0-12-167 name
    Radiometric accuracy pure polarization
    Radiometric accuracy pure polarisation
  0-12-168 name
    Radiometric accuracy cross polarization
    Radiometric accuracy cross polarisation
 **No key 0-13-99 in second table; local=false
 **No key 0-13-100 in second table; local=false
 **No key 0-13-101 in second table; local=false
  0-13-118 refVal -2 != 0
  0-14-54 name
    Photosynthetically active radiation integrated over period specified
    Photosyntetically active radiation integrated over period specified
  0-14-57 scale -2 != -1
  0-14-57 refVal -1048574 != -1000
  0-14-57 bitWidth 21 != 11
 **No key 0-15-52 in second table; local=false
 **No key 0-15-53 in second table; local=false
 **No key 0-15-54 in second table; local=false
 **No key 0-15-55 in second table; local=false
  0-19-5 name
    Direction of motion of feature
    Direction of motion of feature degree true
  0-19-5 units
    degree true
    deg
  0-20-32 name
    Rate of ice accretion estimated
    Rate of ice accretion
  0-20-38 name
    Bearing of ice edge
    Bearing of ice edge degree true
  0-20-38 units
    degree true
    deg
  0-20-54 name
    True direction from which a phenomenon or clouds are moving
    True direction from which a phenomenon or clouds are moving degree true
  0-20-54 units
    degree true
    deg
  0-20-126 name
    Lightning rate of discharge
    Lightning rates of discharge
  0-20-128 name
    Lightning - direction from station
    Lightning - direction from station degree true
  0-20-128 units
    degree true
    deg
  0-21-150 name
    Beam collocation
    Beam co-location
  0-21-158 name
    ASCAT Kp estimate quality
    ASCAT KP quality estimate
  0-21-161 name
    ASCAT synthetic data quantity
    ASCAT synthetic data quality
 **No key 0-21-176 in second table; local=false
 **No key 0-21-177 in second table; local=false
 **No key 0-21-178 in second table; local=false
 **No key 0-21-179 in second table; local=false
 **No key 0-21-180 in second table; local=false
 **No key 0-21-181 in second table; local=false
 **No key 0-21-182 in second table; local=false
  0-22-1 name
    Direction of waves
    Direction of waves degree true
  0-22-1 units
    degree true
    deg
  0-22-2 name
    Direction of wind waves
    Direction of wind waves degree true
  0-22-2 units
    degree true
    deg
  0-22-3 name
    Direction of swell waves
    Direction of swell waves degree true
  0-22-3 units
    degree true
    deg
  0-22-4 name
    Direction of current
    Direction of current degree true
  0-22-4 units
    degree true
    deg
  0-22-5 name
    Direction of sea-surface current
    Direction of sea surface current degree true
  0-22-5 units
    degree true
    deg
  0-22-76 name
    Direction from which dominant waves are coming
    Direction from which dominant waves are coming degree true
  0-22-76 units
    degree true
    deg
  0-22-86 name
    Mean direction from which waves are coming
    Mean direction from which waves are coming degree true
  0-22-86 units
    degree true
    deg
  0-22-87 name
    Principal direction from which waves are coming
    Principal direction from which waves are coming degree true
  0-22-87 units
    degree true
    deg
  0-22-99 name
    Mean direction at low wave numbers wavelength > 731 m
    Mean direction at low wave numbers wavelength > 731 m degree true
  0-22-99 units
    degree true
    deg
 **No key 0-22-142 in second table; local=false
 **No key 0-22-143 in second table; local=false
 **No key 0-22-144 in second table; local=false
 **No key 0-22-145 in second table; local=false
 **No key 0-22-146 in second table; local=false
 **No key 0-22-147 in second table; local=false
 **No key 0-22-148 in second table; local=false
 **No key 0-22-149 in second table; local=false
  0-22-177 scale 0 != 1
  0-22-177 bitWidth 6 != 9
  0-23-27 name
    Main transport direction in the atmosphere
    Main transport direction in the atmosphere degree true
  0-23-27 units
    degree true
    deg
  0-23-28 name
    Main transport direction in water
    Main transport direction in water degree true
  0-23-28 units
    degree true
    deg
  0-23-29 name
    Main transport direction in ground water
    Main transport direction in ground water degree true
  0-23-29 units
    degree true
    deg
  0-24-22 name
    Concentration in precipitation of named isotope type
    Concentration in precipitation of names isotope type
 **No key 0-25-180 in second table; local=false
 **No key 0-25-181 in second table; local=false
 **No key 0-25-182 in second table; local=false
 **No key 0-25-183 in second table; local=false
 **No key 0-25-184 in second table; local=false
 **No key 0-26-21 in second table; local=false
 **No key 0-26-22 in second table; local=false
 **No key 0-26-23 in second table; local=false
  0-27-80 name
    Viewing azimuth angle
    Viewing azimuth angle degree true
  0-27-80 units
    degree true
    deg
 **No key 0-33-84 in second table; local=false
 **No key 0-33-86 in second table; local=false
  0-40-15 name
    Normalized differential vegetation index NDVI
    Normalised differential vegetation index NDVI
  0-40-17 name
    Non-normalized principal component score
    Non-normalised principal component score

  Missing in first table
    0-1-97
    0-1-98
    0-1-113
    0-2-7
    0-2-98
    0-2-147
    0-4-8
    0-7-11
    0-8-15
    0-8-27
    0-8-32
    0-8-44
    0-8-45
    0-10-12
    0-12-60
    0-14-71
    0-14-73
    0-15-7
    0-15-9
    0-15-10
    0-15-22
    0-15-23
    0-15-28
    0-15-40
    0-15-43
    0-15-44
    0-20-79
    0-20-80
    0-20-137
    0-22-176
    0-22-179
    0-22-180
    0-22-181
    0-22-186
    0-22-187
    0-25-144
    0-25-145
    0-25-151
    0-25-152
    0-25-153
    0-33-9

11/23/2014 caron
  download src/main/sources/wmo/BUFRCREX_22_0_1.zip
  from http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html
