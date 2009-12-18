/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.iosp.bufr.tables;

import ucar.nc2.util.TableParser;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.Charset;

/**
 * COMMON CODE TABLE C-1: Identification of originating/generating centre
 * COMMON CODE TABLE C-12: Sub-Centres of Originating Centres
 * COMMON CODE TABLE C-13: Data sub categories of categories defined by entries in BUFR Table A
 * Table A: data categories
 * 
 * Apparently C-11 is the same as C-1, but C-11 is for GRIB and C-1 is for BUFR
 *
 * @author caron
 * @see "http://www.wmo.int/pages/prog/www/WMOCodes/Operational/CommonTables/BufrCommon-11-2008.doc"
 * @since Dec 15, 2009
 */
public class CommonCodeTables {
  private static String[] tableC1 = null;
  private static String[] tableA = null;
  private static Map<Integer, String> tableC12 = null;
  private static Map<Integer, String> tableC13 = null;

  static private void initTableA() {
    String location = BufrTables.RESOURCE_PATH + "wmo/TableA-11-2008.txt";
    InputStream ios = BufrTables.class.getResourceAsStream(location);
    tableA = new String[256];

    try {
      List<TableParser.Record> recs = TableParser.readTable(ios, "3i,60", 255);
      for (TableParser.Record record : recs) {
        int no = (Integer) record.get(0);
        String name = (String) record.get(1);
        name = name.trim();
        tableA[no] = name;
        //System.out.printf("add %d %s%n", no, name);
      }

    } catch (IOException ioe) {

    } finally {
      if (ios != null)
        try {
          ios.close();
        }
        catch (IOException ioe) {
        }
    }
  }

  static private void initC1() {
    String location = BufrTables.RESOURCE_PATH + "wmo/wmoTableC1.txt";
    InputStream ios = BufrTables.class.getResourceAsStream(location);
    tableC1 = new String[256];

    try {
      String prev = null;
      List<TableParser.Record> recs = TableParser.readTable(ios, "8,13i,120", 500);
      for (TableParser.Record record : recs) {
        int no = (Integer) record.get(1);
        String name = (String) record.get(2);
        name = name.trim();
        tableC1[no] = name.equals(")") ? prev : name;
        prev = name;
      }

    } catch (IOException ioe) {

    } finally {
      if (ios != null)
        try {
          ios.close();
        }
        catch (IOException ioe) {
        }
    }
  }

  static private void initC12() {
    String location = BufrTables.RESOURCE_PATH + "wmo/wmoTableC12.txt";
    InputStream ios = BufrTables.class.getResourceAsStream(location);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF-8")));
    tableC12 = new HashMap<Integer, String>(200);
    int count = 0;

    int center_id = 0, subcenter_id = 0;

    try {
      while (true) {
        String line = dataIS.readLine();
        count++;
        if (line == null) break;
        if (line.startsWith("#")) continue;

        String[] flds = line.split("[ \t]+"); // 1 or more whitespace

        if (flds[0].startsWith("00")) {
          center_id =  Integer.parseInt(flds[0]);
        } else {
          subcenter_id =  Integer.parseInt(flds[1]);
          StringBuffer sbuff = new StringBuffer();
          for (int i=2; i<flds.length; i++) {
            if (i>2) sbuff.append(" ");
            sbuff.append(flds[i]);
          }
          // System.out.printf("add %d %d %s %n",center_id, subcenter_id, sbuff);
          int subid = center_id << 16 + subcenter_id;
          tableC12.put(subid, sbuff.toString());
        }
      }

    } catch (IOException ioe) {

    } finally {
      if (ios != null)
        try {
          ios.close();
        }
        catch (IOException ioe) {
        }
    }
  }

  static private void initC13() {
    String location = BufrTables.RESOURCE_PATH + "wmo/wmoTableC13.txt";
    InputStream ios = BufrTables.class.getResourceAsStream(location);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF-8")));
    tableC13 = new HashMap<Integer, String>(200);
    int count = 0;

    int cat = 0, subcat = 0;

    try {
      while (true) {
        String line = dataIS.readLine();
        count++;
        if (line == null) break;
        if (line.startsWith("#")) continue;

        String[] flds = line.split("[ \t]+"); // 1 or more whitespace
        //System.out.printf("flds[0] = <%s>%n",flds[0]);

        if (flds[0].length() > 0) {
          cat =  Integer.parseInt(flds[0]);
        } else {
          subcat =  Integer.parseInt(flds[1]);
          StringBuffer sbuff = new StringBuffer();
          for (int i=2; i<flds.length; i++) {
            if (i>2) sbuff.append(" ");
            sbuff.append(flds[i]);
          }
          //System.out.printf("add %d %d %s %n",cat, subcat, sbuff);
          int subid = cat << 16 + subcat;
          tableC13.put(subid, sbuff.toString());
        }
      }

    } catch (IOException ioe) {

    } finally {
      if (ios != null)
        try {
          ios.close();
        }
        catch (IOException ioe) {
        }
    }
  }

  /**
   * Center name, from table C-1
   *
   * @param center_id center id
   * @return center name, or "unknown"
   */
  static public String getCenterName(int center_id) {
    if (tableC1 == null) initC1();
    String result = ((center_id < 0 || center_id > 255)) ? null : tableC1[center_id];
    return result != null ? result : "Unknown center=" + center_id;
  }

  /**
   * Subcenter name, from table C-12
   *
   * @param center_id    center id
   * @param subcenter_id subcenter id
   * @return subcenter name, or null if not found
   */
  static public String getSubCenterName(int center_id, int subcenter_id) {
    if (tableC12 == null) initC12();
    int subid = center_id << 16 + subcenter_id;
    return tableC12.get(subid);
  }

  /**
   * data subcategory name, from table C-13
   *
   * @param cat    data category
   * @param subcat data subcategory
   * @return subcategory name, or null if not found
   */
  static public String getDataSubcategoy(int cat, int subcat) {
    if (tableC13 == null) initC13();
    int subid = cat << 16 + subcat;
    return tableC13.get(subid);
  }

  /**
   * data category name, from table A
   *
   * @param cat    data category
   * @return category name, or null if not found
   */
  static public String getDataCategory(int cat) {
    if (tableA == null) initTableA();
    String result = ((cat < 0 || cat > 255)) ? null : tableA[cat];
    return result != null ? result : "Unknown category=" + cat;
  }

  /////////////////////////////////////////////////////

  public static void main(String arg[]) throws IOException {
    initTableA();
  }

}
