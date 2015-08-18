/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

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
 *
 * https://raw.githubusercontent.com/Unidata/gempak/master/gempak/tables/grid/g2varsfsl1.tbl
 *
 * @author caron
 * @since 9/14/2014
 */
public class GempakLocalTables extends LocalTables {

  private static boolean debug = false;
  private static GempakLocalTables single;

  public static GempakLocalTables getCust(Grib2Table table) {
    if (single == null) single = new GempakLocalTables(table);
    return single;
  }

  GempakLocalTables(Grib2Table grib2Table) {
    super(grib2Table);
    Formatter f = new Formatter();
    local = initLocalTable(grib2Table.getPath(), f);
    System.out.printf("%s%n", f); // debug
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

  private Map<Integer, Grib2Parameter> initLocalTable(String resourcePath, Formatter f) {
    Map<Integer, Grib2Parameter> result = new HashMap<>(100);

    try (InputStream is = GribResourceReader.getInputStream(resourcePath)) {
      if (is == null) throw new IllegalStateException("Cant find " + resourcePath);

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
        result.put(Grib2Customizer.makeParamId(disc, cat, id), gp);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return result;
  }

}
