/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.nc2.util.TableParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * LocalTables from KMA (Japan)
 *
 * @author John
 * @since 6/22/11
 */
public class KmaLocalTables extends LocalTables {
  KmaLocalTables(Grib2TableConfig config) {
    super(config);
    initLocalTable();
  }

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  private void initLocalTable() {
    String tablePath = config.getPath();
    ClassLoader cl = KmaLocalTables.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(tablePath)) {
    if (is == null) throw new IllegalStateException("Cant find "+tablePath);

    List<TableParser.Record> recs = TableParser.readTable(is, "41,112,124i,136i,148i,160", 1000);
    for (TableParser.Record record : recs) {
      String name = (String) record.get(0);
      int disc = (Integer) record.get(2);
      int cat = (Integer) record.get(3);
      int param = (Integer) record.get(4);
      String unit = (String) record.get(5);

      Grib2Parameter s = new Grib2Parameter(disc,cat,param,name,unit,null,null);
      localParams.put(makeParamId(disc, cat, param), s);
    }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
/*
          1         2         3         4         5         6         7         8         9         10        11        12        13        14        15        16        17
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
U_COMPNT_OF_WIND_AFTER_TIMESTEP                   56           0          65           2         128          33           0           2           2         m/s          UU
############################################################################################################################################################################
#                                      ||                      UM                      ||         GRIB1        ||               GRIB2              ||          ||          |
#            Parameter Name            ||     Field||Processing||     Level||     Stash||     Table|| Parameter||   Product|| Parameter|| Parameter||   Unit   ||   Grid   |
#                                      ||      Code||      Code||      Type||      Item||   Version||    Number|| Disciplin||  Category||    Number||          ||          |
#                                      ||  23(LBFC)||25(LBPROC)||  26(LBVC)||42(LBUSER)||  SEC 1(4)||  SEC 1(9)||  SEC 0(7)|| SEC 4(10)|| SEC 4(11)||          ||          |
#                  40                  ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    ||    12    |
############################################################################################################################################################################
          1         2         3         4         5         6         7         8         9         10        11        12        13        14        15        16        17
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789

*/


}
