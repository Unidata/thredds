/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.bufr.tables;

import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ncep local table overrides
 *
 * @author caron
 * @since 8/22/13
 */
public class NcepTable {

  static private void readNcepTable(String location) throws IOException {
    InputStream ios = BufrTables.openStream(location);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF8")));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;
      count++;

      String[] flds = line.split(";");
      if (flds.length < 3) {
        System.out.printf("%d BAD split == %s%n", count, line);
        continue;
      }

      int fldidx = 0;
      try {
        int cat = Integer.parseInt(flds[fldidx++].trim());
        int subcat = Integer.parseInt(flds[fldidx++].trim());
        String desc = StringUtil2.remove(flds[fldidx++], '"');
        entries.add(new TableEntry(cat,subcat,desc));
      } catch (Exception e) {
        System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
    dataIS.close();
  }

  private static List<TableEntry> entries = null;
  private static class TableEntry {
     public int cat, subcat;
     public String value;

    public TableEntry(int cat, int subcat, String value) {
      this.cat = cat;
      this.subcat = subcat;
      this.value = value.trim();
      //System.out.printf(" %3d %3d: %s%n", cat, subcat, value);
    }
  }

  private static void init() {
    entries = new ArrayList<TableEntry>(100);
    String location = "resource:/resources/bufrTables/local/ncep/DataSubCategories.csv";
    try {
      readNcepTable(location);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static String getDataSubcategory(int cat, int subcat) {
    if (entries == null) init();

    for (TableEntry p : entries) {
      if ((p.cat == cat) && (p.subcat == subcat)) return p.value;
    }
    return null;
  }


}
