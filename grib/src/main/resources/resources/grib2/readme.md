#Grib2 Table Management

## Grib2Customizer

All code (except for debugging) accesses tables through Grib2Customizer.

````
public static Grib2Customizer factory(int center, int subCenter, int masterVersion, int localVersion, int genProcessId);
````  

This allows local overrides and table augmentation by center, subcenter, localVersion and genProcessId.
In principle it could allow masterVersion override, but in practice that doesnt seem to be used.

Grib2Table reads standardTableMap.txt to assign
(center, subCenter, masterVersion, localVersion, genProcessId) -> Grib2Table.

A Grib2Customizer is obtained by passing in the appropriate Grib2Table, and the 
proper subclass is returned:

````
  public static Grib2Customizer factory(Grib2Table grib2Table) {
    switch (grib2Table.type) {
      case cfsr: return CfsrLocalTables.getCust(grib2Table);
      case gempak: return GempakLocalTables.getCust(grib2Table);
      case gsd: return FslLocalTables.getCust(grib2Table);
      case kma: return KmaLocalTables.getCust(grib2Table);
      case ncep: return NcepLocalTables.getCust(grib2Table);
      case ndfd: return NdfdLocalTables.getCust(grib2Table);
      case mrms: return MrmsLocalTables.getCust(grib2Table);
      case nwsDev: return NwsMetDevTables.getCust(grib2Table);
      default:
        if (wmoStandardTable == null) wmoStandardTable = new Grib2Customizer(grib2Table);
        return wmoStandardTable;
    }
  }
````

The center id indicates who created the data, but it may use a table from another center.
For example, center 7 (ncep), 9 (usnws) and 54 (canandian met) all use ncep tables.


## WmoCodeFlagTables

This reads in the standard WMO tables from the canonical XML files, puplished by the WMO.
It appears that ECMWF and NCEP doe not override anything in here, and any discrepencies are inadvertant.
  * ECMWF: see EcmwfTableCompare comparing against eccodes program.
  * NCEP: adds abbreviations. (from screen scraping tables published on the web)
  
Therefore WmoCodeFlagTables are the base tables used, and local tables override or augment these.
  