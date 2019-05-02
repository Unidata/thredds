/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import javax.annotation.Nullable;
import ucar.nc2.grib.GribResourceReader;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.nc2.util.TableParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read tables from https://github.com/Unidata/gempak/tree/master/gempak/tables/grid/g2*.tbl
 *
 * eg:
 * https://raw.githubusercontent.com/Unidata/gempak/master/gempak/tables/grid/g2varsfsl1.tbl
 *
 * LOOK: not used in standardTableMap.txt, so cannot be found in any search in Grib2Tables.
 *
 * @author caron
 * @since 9/14/2014
 */
public class GempakLocalTables extends LocalTables {
  private static boolean debug = false;

  GempakLocalTables(Grib2TableConfig config) {
    super(config);
    Formatter f = new Formatter();
    localParams = initLocalTable(config.getPath(), f);
  }

  /*
    ! GRIB2 parameter table
    !
    !D#  = Discipline number
    !CT# = Category number (Octet 10, Code Table 4.2)
    !ID# = Parameter number (Octet 11)
    !PD# = Product Definition Template number (Octet 8-9, Code Table 4.0)
    !
    ! temperature
    !D# CT# ID# PD# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP  DIRECTION
    !23|123|123|123|12345678901234567890123456789012|12345678901234567890|123456789012|12345|123456.89|12345678|1234567890
              1         2         3         4         5         6         7         8         9         10        11        12
    0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
    000 000 000 000 Temperature                      K                    TMPK             0  -9999.00        0          0
    000 000 000 019 Temperature Below Normal         %                    PTBN             0  -9999.00        0          0
    000 000 000 029 Temperature Near Normal          %                    PTNN             0  -9999.00        0          0
    000 000 000 039 Temperature Above Normal         %                    PTAN             0  -9999.00        0          0
    000 000 001 000 Virtual Temperature              K                    TVRK             0  -9999.00        0          0
    000 000 002 000 Potential Temperature            K                    THTA             0  -9999.00        0          0
    000 000 003 000 Equivalent Potential Temp        K                    THTE             0  -9999.00        0          0
    000 000 004 008 Maximum Temperature              K                    TMXK             0  -9999.00        0          0
    000 000 005 008 Minimum Temperature              K                    TMNK             0  -9999.00        0          0
    000 000 006 000 Dew Point Temperature            K                    DWPK             0  -9999.00        0          0
    000 000 007 000 Dew Point Depression             K                    DPDK             0  -9999.00        0          0
    000 000 008 000 Lapse Rate                       K m**-1              LAPS             0  -9999.00        0          0
    000 000 009 000 Temperature Anomaly              K                    TMPKA            0  -9999.00        0          0
    000 000 010 000 Latent Heat Net Flux             W m**-2              FXLH             0  -9999.00        0          0
    000 000 011 000 Sensible Heat Net Flux           W m**-2              FXSH             0  -9999.00        0          0
    000 000 012 000 Heat Index                       K                    HEAT             0  -9999.00        0          0
    000 000 013 000 Wind Chill Factor                K                    CHILL            0  -9999.00       0          0
    !000 000 014 000 Minimum Dew Point Depression     K                    ????             0  -9999.00       0          0
    !000 000 015 000 Virtual Potential Temperature    K                    ????             0  -9999.00       0          0
    !
    ! moisture
    !D# CT# ID# PD# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP  DIRECTION
    !23|123|123|123|12345678901234567890123456789012|12345678901234567890|123456789012|12345|123456.89|12345678|1234567890
    000 001 000 000 Specific Humidity                kg kg**-1            SPFH             0  -9999.00        0          0
   */

  private Map<Integer, Grib2Parameter> initLocalTable(String resourcePath, @Nullable Formatter f) {
    Map<Integer, Grib2Parameter> result = new HashMap<>(100);

    try (InputStream is = GribResourceReader.getInputStream(resourcePath)) {
      if (f != null) f.format("%s, %-20s, %-20s, %-20s%n", "id", "name", "units", "gname");
      TableParser parser = new TableParser("3i,7i,11i,15i,49,69,74,");
      parser.setComment("!");
      List<TableParser.Record> recs = parser.readAllRecords(is, 50000);
      for (TableParser.Record record : recs) {
        int disc =  (Integer) record.get(0);
        int cat =  (Integer) record.get(1);
        int id =  (Integer) record.get(2);
        int template =  (Integer) record.get(3);               // LOOK - 19, 29, 39 ???
        String name = ((String) record.get(4)).trim();
        String units = ((String) record.get(5)).trim();
        String gname = ((String) record.get(6)).trim();

        String ids = disc+"-"+cat+"-"+id;
        if (f != null) f.format("%s == %-20s, %-20s, %-20s%n", ids, name, units, gname);

        Grib2Parameter gp = new Grib2Parameter(disc, cat, id, gname, units, null, name);
        result.put(Grib2Tables.makeParamId(disc, cat, id), gp);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return result;
  }

}
