/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.bufr.tables;

import ucar.nc2.iosp.bufr.BufrDataDescriptionSection;
import ucar.nc2.iosp.bufr.Descriptor;
import ucar.unidata.util.StringUtil;

import java.lang.*;     // Standard java functions
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

/**
 * BUFR files use external tables to interpret the data. TableA is for the type of the data,
 * TableB is for descriptors used to decode/name a raw section of data, and TableD is used
 * to expand a descriptor into a sequence of descriptors.
 * The basic operations of this class is to load BUFR tables stored in files.
 */

public class BufrTables {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrTables.class);

  /**
   * Pattern to get categories.
   */
  private static final Pattern category = Pattern.compile("^\\s*(\\w+)\\s+(.*)");

  /**
   * Pattern to get 3 integers from beginning of line.
   */
  private static final Pattern threeInts = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)");

  /**
   * Pattern to get check for -1 sequence terminator.
   */
  private static final Pattern negOne = Pattern.compile("^\\s*-1");

  /**
   * debug.
   */
  private static final boolean debugTable = false;

  //private static TableA tableA;
  private static Map<String, TableA> tablesA = new ConcurrentHashMap<String, TableA>();
  private static Map<String, TableB> tablesB = new ConcurrentHashMap<String, TableB>();
  private static Map<String, TableD> tablesD = new ConcurrentHashMap<String, TableD>();

  private static final String RESOURCE_PATH = "resources/bufr/tables/";

  /**
   * Used to open BUFR tables.
   *
   * @param location URL or local filename of BUFR table file
   * @return InputStream
   * @throws IOException on read error
   */
  private static InputStream open(String location) throws IOException {
    InputStream ios = null;

    // Try class loader to get resource
    ClassLoader cl = BufrTables.class.getClassLoader();
    String tmp = RESOURCE_PATH + location;
    ios = cl.getResourceAsStream(tmp);
    if (ios != null) return ios;

    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }
    return ios;
  }

  static public TableA getTableA(String tablename) throws IOException {
    TableA a = tablesA.get(tablename);
    if (a == null) {
      a = readTableA(tablename);
      tablesA.put(tablename, a);
    }
    return a;
  }

  static public boolean hasTableB(String tablename) {
    return tablesB.get(tablename) != null;
  }

  static public TableB getTableB(String tablename) throws IOException {
    TableB b = tablesB.get(tablename);
    if (b == null) {
      b = readTableB(tablename);
      tablesB.put(tablename, b);
    }
    return b;
  }

  static public TableD getTableD(String tablename) throws IOException {
    TableD d = tablesD.get(tablename);
    if (d == null) {
      d = readTableD(tablename);
      tablesD.put(tablename, d);
    }
    return d;
  }

  static void addTableB(String tablename, TableB b) {
    tablesB.put(tablename, b);
  }

  static void addTableD(String tablename, TableD d) {
    tablesD.put(tablename, d);
  }


  private static TableA readTableA(String tablename) throws IOException {

    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    //Map<String, String> categories = new HashMap<String, String>();
    Map<Short, String> categories = new HashMap<Short, String>();

    // read table A to store categories
    Matcher m;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      // check for comment lines
      if (line.startsWith("#") || line.length() == 0)
        continue;

      m = category.matcher(line);
      if (m.find()) {
        if (m.group(2).equals("RESERVED")) continue;
        if (m.group(2).equals("FOR EXPERIMENTAL USE")) continue;
        String cat = m.group(2).trim();
        categories.put(Short.valueOf(m.group(1)), cat);

      }
    }
    dataIS.close();

    return new TableA(tablename, tablename, categories);
  }

  public static TableB readTableB(String tablename) throws IOException {

    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    TableB b = new TableB(tablename, tablename);

    // read table B looking for descriptors
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#") || line.length() == 0)
        continue;
      //System.out.println("Table B line =" + line);

      try {
        String[] split = line.split("; ");
        short x = Short.parseShort(split[1].trim());
        short y = Short.parseShort(split[2].trim());
        int scale = Integer.parseInt(split[3].trim());
        int refVal = Integer.parseInt(split[4].trim());
        int width = Integer.parseInt(split[5].trim());

        b.addDescriptor(x, y, scale, refVal, width, split[7].trim(), split[6].trim());
      } catch (Exception e) {
        log.error("Bad table " + tablename + " entry=<" + line + ">", e);
      }
    }
    dataIS.close();

    return b;
  }

  /* Note Robb has this cleanup in DescriptorTableB
        desc = desc.replaceFirst( "\\w+\\s+TABLE B ENTRY( - )?", "" );
      desc = desc.trim();
      this.description = desc;
      desc = desc.replaceAll( "\\s*\\(.*\\)", "" );
      desc = desc.replaceAll( "\\*", "" );
      desc = desc.replaceAll( "\\s+", "_" );
      desc = desc.replaceAll( "\\/", "_or_" );
      desc = desc.replaceFirst( "1-", "One-" );
      desc = desc.replaceFirst( "2-", "Two-" );
   */

  public static TableD readTableD(String tablename) throws IOException {

    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;

    TableD d = new TableD(tablename, tablename);

    // read table D to store sequences and their descriptors
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      count++;
      // check for comment lines
      if (line.startsWith("#") || line.length() == 0)
        continue;

      String[] split = line.split("[ ]+"); // 1 or more whitespace
      try {
        short seqF = Short.parseShort(split[0]);
        short seqX = Short.parseShort(split[1]);
        short seqY = Short.parseShort(split[2]);
        assert seqF == 3;

        String seqName = "";
        if (split.length > 3) {
          StringBuilder sb = new StringBuilder(40);
          for (int i = 3; i < split.length; i++)
            sb.append(split[i]).append(" ");
          seqName = sb.toString();
          seqName = StringUtil.remove( seqName, "()");
        }

        List<Short> seq = new ArrayList<Short>();
        // look for descriptors within sequence terminated by -1
        while (true) {
          line = dataIS.readLine();
          if (line == null) break;
          count++;
          // check for comment lines
          if (line.startsWith("#") || line.length() == 0)
            continue;

          Matcher m = threeInts.matcher(line);
          // descriptor found
          if (m.find()) {
            short f = Short.parseShort(m.group(1));
            short x = Short.parseShort(m.group(2));
            short y = Short.parseShort(m.group(3));
            seq.add(Descriptor.getFxy(f, x, y));

          } else {
            m = negOne.matcher(line);
            if (m.find()) {
              // store this sequence
              d.addDescriptor(seqX, seqY, seqName, seq);
              break;
            }
          }
        }
      } catch (Exception e) {
        log.warn("TableD " + tablename + " Failed on line " + count + " = " + line+"\n "+e);
        e.printStackTrace();
      }
    }
    dataIS.close();

    return d;
  }

  // debug
  public static void main(String args[]) throws IOException {
    Formatter out = new Formatter(System.out);

   TableA tableA = BufrTables.getTableA("B4M-000-013-A");
   tableA.show(out);

   //TableB tableB = BufrTables.getTableB("B3L-059-003-B.diff");
   //tableB.show(out);

   // TableD tableD = BufrTables.getTableD("B4M-000-013-D");
    //tableD.show(out);

  }
}
