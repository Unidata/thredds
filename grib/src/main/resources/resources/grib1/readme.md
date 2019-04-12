
#Grib1 Table Management

Grib1 Parameter tables are difficult because the parameter number is a byte, so restricted to 256 slots.
Supposedly the first 128 are reserved to the WMO, and the second 128 are allocated by each center. Each
GRIB record contains the (center, subcenter, tableVersion), and in principle that should uniquely determine
the table. In practice the centers are mostly concerned with operational meteorology, and so are 
careless about changing the table version as their parameters change. They sometimes (eg FNMOC) dont 
keep historical tables, and so their historical GRIB data cannot be accurately interpreted.

There are many other anomolies and variations which make GRIB1 especially difficult. See 
[On the suitability of BUFR and GRIB for archiving data](https://www.unidata.ucar.edu/publications/caron/GRIBarchivals.pdf)
for more details.

Also see [Unidata online docs](https://www.unidata.ucar.edu/software/thredds/v4.3/netcdf-java/formats/GribTables.html)
which have pointers to WMO documentation. Where are the 5.0 docs?


## Grib1 Table Management
 
**_Grib1ParamTables_** deals with the Parameter tables.

**_Grib1Customizer_** deals with the variations on all other GRIB tables, mostly center-specific variants.

GRIB reading is always done as _collections_ of GRIB records (even when theres only one record).
Thus all access to GRIB goes through the **_GribCollection_** interface. 


##Grib1 Standard Lookup
 
**_Grib1ParamTables_** is the interface to manage GRIB-1 Parameter Table lookups.
A lookup is a Map:(center, subcenter, version) --> Parameter Table path.

The lookups are loaded at startup, but the Parameter Tables are loaded on demand, when they are requested
by _getParameter(int center, int subcenter, int tableVersion, int param_number)_.

Grib1ParamTables.java has a set of lookup tables hardcoded in. For example:

````
standardLookup.readLookupTable("resources/grib1/lookupTables.txt");
standardLookup.readLookupTable("resources/grib1/ecmwfEcCodes/lookupTables.txt");
standardLookup.readLookupTable("resources/grib1/ecmwf/lookupTables.txt");
standardLookup.readLookupTable("resources/grib1/ncl/lookupTables.txt");
standardLookup.readLookupTable("resources/grib1/dss/lookupTables.txt");
standardLookup.readLookupTable("resources/grib1/wrf/lookupTables.txt");
````

These lookup tables are ascii text files in the following format. 
Each row contains the center, subcenter and table version, and the table filename, 
with a colon (:) separating the fields.

Example:

````
# resources\grib1\lookupTables.txt
# cen   sub  version      table path
  0:    -1:    -1:   dss/WMO_GRIB1.xml
  7:    -1:   128:   ncep/ncepGrib1-128.xml
  7:    -1:   129:   ncep/ncepGrib1-129.xml
  7:     4:   130:   ncep/ncepGrib1-7-4-130.xml
  7:    12:   130:   ncep/ncepGrib1-7-12-130.xml
  7:   138:   130:   ncep/ncepGrib1-7-138-130.xml
  7:    -1:   130:   ncep/ncepGrib1-130.xml
  7:    -1:   131:   ncep/ncepGrib1-131.xml
  7:    -1:   133:   ncep/ncepGrib1-133.xml
  7:    -1:   140:   ncep/ncepGrib1-140.xml
  7:    -1:   141:   ncep/ncepGrib1-141.xml
  7:    -1:    -1:   ncep/ncepGrib1-2.xml
  9:    -1:   128:   noaa_rfc/params9-128.tab
 57:    -1:     2:   afwa/afwa.tab
 57:     1:   132:   afwa/afwa_133.tab
 57:     1:   133:   afwa/afwa_133.tab
 58:    42:     2:   afwa/af_2.tab
 58:    -1:    -1:   fnmoc/US058MMTA-ALPdoc.pntabs-prodname-masterParameterTableOrdered.GRIB1.Tbl2.xml
 60:   255:     2:   local/wrf_amps.wrf
173:     4:   130:   ncep/ncepGrib1-173-4-130.xml
````

You can view all the standard tables used by the CDM in ToolsUI, using the IOSP/GRIB-1/GRIB1-TABLES tab.
This is the definitive standard set of tables read in at runtime, called the 
**_Grib1 Standard Lookup_**.

The lookups are kept in order of being read, and when being searched, the first match is used. 


## Grib1ParamTables Parameter Lookup

A Grib1ParamTables does the table and parameter lookup. The caller may override the standard lookup by
specifying the parameter table path or by specifying a lookup table:

````
  /**
   * Get a Grib1ParamTables object, optionally specifying a parameter table or lookup table specific to this dataset.
   *
   * @param paramTablePath  path to a parameter table, in format Grib1ParamTable can read.
   * @param lookupTablePath path to a lookup table, in format Lookup.readLookupTable() can read.
   * @return Grib1Tables
   * @throws IOException on read error
   */
  public static Grib1ParamTables factory(String paramTablePath, String lookupTablePath) throws IOException {
    if (paramTablePath == null && lookupTablePath == null) return new Grib1ParamTables();
    ...
    
````

When an override is present, parameters are used when found in the override, but default to the standard lookup 
when not found:

   1. If theres a dataset-specific Grib1ParamTables, match on parameter number.
   2. If theres a dataset-specific Lookup, use it to do Table lookup.
   3. Use the Standard Lookup, and see if the parameter is in there.
   4. If a parameter is still not found then "Unknown Parameter center-subcenter-version-param" is 
   used as the name, and an empty string for the units.
 
Table Lookup uses (center, subcenter, version) to find a matching Grib1ParamTables with the following rules: 

   1. First look for the first exact match on (subcenter, center, and version), use it if found.
   2. Look for a wildcard match on (subcenter, center, and version), use it if found.
   3. Otherwise use the WMO table.
 
## Strict mode Table Lookup:

_(This is my interpretations of WMO rules, but likely its wrong, and certainly not followed, esp by ecmwf)_

   1. If (param < 128 and version < 128) the WMO table is always used.
   2. If (param >= 128 or version >= 128) a table must be found that matches the subcenter, center, and version,
   either an exact match or a wildcard match.

You can set strictMode programatically via _ucar.nc2.grib.grib1.tables.Grib1StandardTables.setStrict(true)_
or in the RunTime configuration file by adding

```` 
  <grib1Table strict="true"/>. 
````

If strict=true, when a table is not matched, and local parameters are used, the GRIB file will fail to open.
If strict=false, tables may override the global parameters (param < 128). 
The default for strict is false. 