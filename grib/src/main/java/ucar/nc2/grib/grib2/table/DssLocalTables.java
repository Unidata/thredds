/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Read screen scraped DSS tables
 *
 * @author caron
 * @since 11/3/11
 */
public class DssLocalTables extends LocalTables {
  private static final String tableName = "resources/grib2/local/cfsr.txt";
  private static boolean debug = true;

  DssLocalTables(int center, int subCenter, int masterVersion, int localVersion) {
    super(center, subCenter, masterVersion, localVersion);
  }

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  protected void initLocalTable() {
    ClassLoader cl = KmaLocalTables.class.getClassLoader();
    InputStream is = cl.getResourceAsStream(tableName);
    if (is == null) throw new IllegalStateException("Cant find "+tableName);
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    try {
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

       TableEntry s = new TableEntry(p1,p2,p3,name,unit,abbrev);
       local.put(makeHash(p1,p2,p3), s);
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

  public static void main(String arg[]) {
    DssLocalTables t = new DssLocalTables(7,0,0,0);
    t.initLocalTable();
  }

}
