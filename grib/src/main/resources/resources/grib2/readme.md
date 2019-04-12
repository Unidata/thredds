#Grib2 Table Management

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

The center id indicates who created the data, but it map use a table from anoth center.
For example, center 7 (ncep), 9 (usnws) and 54 (canandian met) all use ncep tables.
