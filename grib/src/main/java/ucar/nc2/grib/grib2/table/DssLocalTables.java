/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;

/**
 * Read screen scraped DSS tables.
 * CFSR 093 not needed - screen scraped NCEP ok 2/2/2012
 *
 * @author caron
 * @since 11/3/11
 */
public class DssLocalTables extends LocalTables {
  private static final String tableName = "resources/grib2/local/cfsr.txt";
  private static boolean debug = false;
  private static DssLocalTables single;

  public static DssLocalTables getCust(Grib2Table table) {
    if (single == null) single = new DssLocalTables(table);
    return single;
  }

  private DssLocalTables(Grib2Table grib2Table) {
    super(grib2Table);
    if (grib2Table.getPath() == null)
      grib2Table.setPath(tableName);
    initLocalTable();
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return tableName;
  }

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  protected void initLocalTable() {
    ClassLoader cl = this.getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(tableName)) {
      if (is == null) throw new IllegalStateException("Cant find " + tableName);
      BufferedReader br = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));

      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] flds = StringUtil2.splitString(line);

        int p1 = Integer.parseInt(flds[0].trim()); // must have a number
        int p2 = Integer.parseInt(flds[1].trim()); // must have a number
        int p3 = Integer.parseInt(flds[2].trim()); // must have a number
        StringBuilder b = new StringBuilder();
        int count = 3;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String abbrev = b.toString().trim();
        b.setLength(0);
        count++;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String name = b.toString().trim();
        b.setLength(0);
        count++;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String unit = b.toString().trim();

        Grib2Parameter s = new Grib2Parameter(p1, p2, p3, name, unit, abbrev, null);
        local.put(makeParamId(p1, p2, p3), s);
        if (debug) System.out.printf(" %s%n", s);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
/*
0	0	0	TMP	. Temperature	. K
0	0	2	POT	. Potential temperature	. K
0	0	4	T MAX	. Maximum temperature	. K
0	0	5	T MIN	. Minimum temperature	. K
0	0	6	DPT	. Dewpoint temperature	. K
*/

  public static void main(String[] args) {
    DssLocalTables t = new DssLocalTables(new Grib2Table("DSS",7, 0, 0, 0, -1, null, Grib2Table.Type.dss));
    Formatter f = new Formatter();
    Grib2Parameter.compareTables("DSS-093", "Standard WMO version 8", t.getParameters(), Grib2Customizer.factory(0,0,0,0,0), f);
    System.out.printf("%s%n", f);

    Formatter f2 = new Formatter();
    Grib2Parameter.compareTables("DSS-093", "NCEP Table", t.getParameters(), Grib2Customizer.factory(7,0,0,0,0), f2);
    System.out.printf("%s%n", f2);

  }
}
