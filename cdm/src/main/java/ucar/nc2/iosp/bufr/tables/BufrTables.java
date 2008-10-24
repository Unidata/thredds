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

import java.lang.*;     // Standard java functions
import java.util.*;
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

  /**
   * Pattern to get categories.
   */
  private static final Pattern category =
      Pattern.compile("^\\s*(\\w+)\\s+(.*)");

  /**
   * Pattern to get 3 integers from beginning of line.
   */
  private static final Pattern threeInts =
      Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)");

  /**
   * Pattern to get check for -1 sequence terminator.
   */
  private static final Pattern negOne = Pattern.compile("^\\s*-1");

  /**
   * debug.
   */
  private static final boolean debugTable = false;

  /**
   * tableA HashMap to store multple BUFR tables used by the BUFR file reads.
   */
  //public static final Map<String, Map<String, String>> tablesA = new HashMap<String, Map<String, String>>();
  public static final Map<String, TableADataCategory> tablesA = new HashMap<String, TableADataCategory>();
  /**
   * tableB HashMap to store multple BUFR tables used by the BUFR file reads.
   */
  //public static final Map<String, Map<String, DescriptorTableB>> tablesB = new HashMap<String, Map<String, DescriptorTableB>>();
  public static final Map<String, TableB> tablesB = new HashMap<String,TableB>();
  /**
   * tableD HashMap to store multple BUFR tables used by the BUFR file reads.
   */
  //public static final Map<String, Map<String, DescriptorTableD>> tablesD = new HashMap<String, Map<String, DescriptorTableD>>();
  public static final Map<String, TableD> tablesD = new HashMap<String, TableD>();

  private static final String RESOURCE_PATH = "resources/bufr/tables/";

  /**
   * Used to open BUFR tables.
   *
   * @param location URL or local filename of BUFR table file
   * @return InputStream
   * @throws IOException
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

  static public TableADataCategory getTableA(String tablename, boolean WMO) throws IOException {
    if (!tablesA.containsKey(tablename))
      readTableA(tablename, WMO);
    return tablesA.get(tablename);
  }

  static public TableB getTableB(String tablename, boolean WMO) throws IOException {
    if (!tablesB.containsKey(tablename))
      readTableB(tablename, WMO);
    return tablesB.get(tablename);
  }

  static public TableD getTableD(String tablename, boolean WMO) throws IOException {
    if (!tablesD.containsKey(tablename))
      readTableD(tablename, WMO);
    return tablesD.get(tablename);
  }

  /**
   * reads in table A descriptors.
   *
   * @param tablename
   * @throws IOException
   */
  public synchronized static void readTableA(String tablename, boolean WMO) throws IOException {

    // check if table has already been processed
    if (tablesA.containsKey(tablename)) return;

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
        //if( m.group(2).equals( "FOR EXPERIMENTAL USE" ) ) continue;
        String cat = m.group(2).trim();
        //categories.put(m.group(1), cat);
        categories.put( Short.valueOf( m.group(1) ), cat);
        //System.out.println( "key, category = " + m.group(1) +", "+  cat );

      }
    }
    dataIS.close();
    //tablesA.put(tablename, categories);
    TableADataCategory a = new BufrTableA(tablename, null,  categories );
    tablesA.put(tablename, a);
  }

  /**
   * reads in table B descriptors.
   *
   * @param tablename
   * @throws IOException
   */
  public synchronized static void readTableB(String tablename, boolean WMO) throws IOException {

    // check if table has already been processed
    if (tablesB.containsKey(tablename)) return;

    //System.out.println("Table B tablename =" + tablename);
    
    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    //Map<String, DescriptorTableB> descriptors = new HashMap<String, DescriptorTableB>();
    //Map<Short, DescriptorTableB> descriptors = new HashMap<Short, DescriptorTableB>();
    Map<Short, TableBdescriptor> descriptors = new HashMap<Short, TableBdescriptor>();

    // add time observation, place holder descriptor to all tables
    DescriptorTableB d;
    //d = new DescriptorTableB("0-0-7", "0", "0", "0", "CCITT_IA5", "bogus entry no width");
    //descriptors.put(d.getKey(), d);
    //d = new DescriptorTableB("0-21-192", "0", "0", "7", "dB", "Spectral peak power 0th moment");
    //descriptors.put(d.getKey(), d);
    //d = new DescriptorTableB("0-63-255", "0", "0", "1", "Numeric", "bit pad", false);
    //descriptors.put(d.getKey(), d);

    // read table B looking for descriptors
    Matcher m;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#") || line.length() == 0)
        continue;
      //System.out.println("Table B line =" + line);

      StringTokenizer stoke = new StringTokenizer(line, ";");
      String f = stoke.nextToken();
      String x = stoke.nextToken();
      String y = stoke.nextToken();
      String key = f.trim() + "-" + x.trim() + "-" + y.trim();
      String scale = stoke.nextToken();
      String refVal = stoke.nextToken();
      String width = stoke.nextToken();
      String units = stoke.nextToken();
      String name = stoke.nextToken();
      if (debugTable) {
        System.out.println("Table B line =" + line);
        System.out.println("key = " + key);
        System.out.println("scale = " + scale);
        System.out.println("refVal = " + refVal);
        System.out.println("width = " + width);
        System.out.println("units = " + units);
        System.out.println("name = " + name);
      }
      //System.out.println("key ="+ key );
      d = new DescriptorTableB(key, scale, refVal, width, units, name, WMO);
      //descriptors.put(d.getKey(), d);
      short id = BufrDataDescriptionSection.getDesc(key);
      descriptors.put(Short.valueOf( id ), d);
    }
    dataIS.close();
    String pre = "";
    if( WMO )
      pre = "Lastest WMO table ";
    BufrTableB b = new BufrTableB( pre + tablename, null, descriptors );
    //tablesB.put(tablename, descriptors);
    tablesB.put(tablename, b);
  }

  /**
   * reads in table D descriptors.
   *
   * @param tablename
   * @throws IOException
   */
  public synchronized static void readTableD(String tablename, boolean WMO) throws IOException {

    // check if table has already been processed
    if (tablesD.containsKey(tablename)) return;

    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    DescriptorTableD d;
    //Map<String, DescriptorTableD> sequences = new HashMap<String, DescriptorTableD>();
    Map<Short, TableDdescriptor> sequences = new HashMap<Short, TableDdescriptor>();

    // read table D to store sequences and their descriptors
    Matcher m;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      // check for comment lines
      if (line.startsWith("#") || line.length() == 0)
        continue;
      m = threeInts.matcher(line);
      // sequence found
      if (m.find()) {
        String key = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        if (debugTable) {
          System.out.println("key = " + key);
        }
        List al = new ArrayList<String>();
        // look for descriptors within sequence terminated by -1
        while (true) {
          line = dataIS.readLine();
          if (line == null) break;
          // check for comment lines
          if (line.startsWith("#") || line.length() == 0)
            continue;
          m = threeInts.matcher(line);
          // descriptor found
          if (m.find()) {
            String dkey = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
            if (debugTable) {
              System.out.println("dkey = " + dkey);
            }
            al.add(dkey);
          } else {
            m = negOne.matcher(line);
            if (m.find()) {
              // store this sequence
              d = new DescriptorTableD("", key, al, WMO);
              //sequences.put(key, al);
              //sequences.put(key, d);
              short id = BufrDataDescriptionSection.getDesc(key);
              sequences.put( Short.valueOf( id ), d);
              break;
            }
          }
        }
      }
    }
    dataIS.close();
    String pre = "";
    if( WMO )
      pre = "Lastest WMO table ";
    BufrTableD td = new BufrTableD( pre + tablename, null, sequences );
    //tablesD.put(tablename, sequences);
    tablesD.put(tablename, td);
  }

   /**
   * Checks reading in a set of BUFR tables.
   *
   * @param args tablename
   * @throws IOException
   */
  public static void main(String args[]) throws IOException {

    // Function References
    String tableName;
    if (args.length == 1) {             
      tableName = args[0];
    } else {
      tableName = "B3L-059-003-ABD.diff";
      //tableName = "B4M-000-014-ABD";
    }

    // only can be element descriptors in tableB
    TableB tableB = BufrTables.getTableB(tableName.replace( "-ABD", "-B"), false);
    TableBdescriptor b;
    ArrayList<Short> v = new ArrayList( tableB.getMap().keySet());
    Collections.sort( v );
    System.out.println("Elements Descriptors from table "+ tableB.getName() +"\n");
    for ( Short id : v ) {
        b = tableB.getDescriptor( id);
        System.out.println("FXY ="+ b.getFxy() +" name ="+ b.getName() +" isWMO ="+ b.isWMO());
    }

    // sequences can be other sequences as well as element descriptors
    System.out.println();
    TableD tableD = BufrTables.getTableD(tableName.replace( "-ABD", "-D"), false);
    TableDdescriptor d;
    List<String> al;
    v = new ArrayList( tableD.getMap().keySet());
    Collections.sort( v );
    System.out.println("Sequences from table "+ tableB.getName() +"\n");
    for ( Short id : v ) {
        d = tableD.getDescriptor(id);
        al = d.getDescList();
        System.out.println("FXY " + d.getFxy() +" isWMO ="+ d.isWMO());
        System.out.println("List =" + al);
    }

  }
}
