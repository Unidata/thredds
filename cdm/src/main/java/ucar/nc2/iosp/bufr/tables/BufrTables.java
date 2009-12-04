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

import ucar.nc2.iosp.bufr.Descriptor;
import ucar.nc2.util.TableParser;
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
import java.nio.charset.Charset;

import org.jdom.input.SAXBuilder;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * Reads BUFR tables of various forms. Interacts with TableLookup.
 * Some hardcoding - TODO make user configurable
 */

public class BufrTables {
  static final String RESOURCE_PATH = "/resources/bufrTables/";

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BufrTables.class);

  private static final boolean debugTable = true;
  private static final boolean showReadErrs = false;

  private static TableA tableA;
  private static Map<String, TableB> tablesB = new ConcurrentHashMap<String, TableB>();
  private static Map<String, TableD> tablesD = new ConcurrentHashMap<String, TableD>();

  private static final Pattern category = Pattern.compile("^\\s*(\\w+)\\s+(.*)");
  private static final Pattern threeInts = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)");  // get 3 integers from beginning of line
  private static final Pattern negOne = Pattern.compile("^\\s*-1");  // check for -1 sequence terminator

  static public TableA getWmoTableA() throws IOException {
    String tablename = RESOURCE_PATH + "wmo/TableA-11-2008.txt";
    if (tableA == null) {
      InputStream ios = BufrTables.class.getResourceAsStream(tablename);
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
      Map<Short, String> categories = new HashMap<Short, String>();

      // read table A to store categories
      Matcher m;
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.startsWith("#") || line.length() == 0)
          continue;

        m = category.matcher(line); // overkill
        if (m.find()) {
          if (m.group(2).equals("RESERVED")) continue;
          if (m.group(2).equals("FOR EXPERIMENTAL USE")) continue;
          String cat = m.group(2).trim();
          categories.put(Short.valueOf(m.group(1)), cat);
        }
      }
      dataIS.close();

      tableA = new TableA(tablename, tablename, categories);
    }

    return tableA;
  }

  // LOOK
  static public TableA readLookupTable(String tablename) throws IOException {
    InputStream ios = open(tablename);
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
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

  ///////////////////////

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

  static public boolean hasTableB(String tablename) {
    return tablesB.get(tablename) != null;
  }

  static public TableB getTableB(String tablename) throws IOException {
    TableB b = tablesB.get(tablename);
    if (b == null) {
      b = readRobbTableB(tablename);
      tablesB.put(tablename, b);
    }
    return b;
  }

  static void addTableB(String tablename, TableB b) {
    tablesB.put(tablename, b);
  }  

  static public TableB getWmoTableB() throws IOException {
    String name = "wmo version 14 table B";
    String location = RESOURCE_PATH + "wmo/BC_TableB.csv";
    InputStream ios = BufrTables.class.getResourceAsStream(location);
    TableB b = new TableB(name, location);
    readWmoTableB(ios, b);
    return b;
  }

  static public TableB readTableB(String location, String mode) throws IOException {
    InputStream ios = openStream(location);
    TableB b = new TableB(location, location);
    if (mode.equals("wmo"))
      readWmoTableB(ios, b);
    else if (mode.equals("ncep"))
      readNcepTableB(ios, b);
    else if (mode.equals("ecmwf"))
      readEcmwfTableB(ios, b);
    else if (mode.equals("bmet"))
      readBmetTableB(ios, b);
    else
      readRobbTableB(ios, b);

    return b;
  }

  static private void readWmoTableB(InputStream ios, TableB b) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF8")));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;
      count++;

      if (count == 1) {
        if (showReadErrs) System.out.println("header line == " + line);
        continue;
      }

      // commas embedded in quotes - replace with blanks for now
      int pos1 = line.indexOf('"');
      if (pos1 >= 0) {
        int pos2 = line.indexOf('"', pos1 + 1);
        StringBuffer sb = new StringBuffer(line);
        for (int i = pos1; i < pos2; i++)
          if (sb.charAt(i) == ',') sb.setCharAt(i, ' ');
        line = sb.toString();
      }

      String[] flds = line.split(",");
      if (flds.length < 7) {
        if (showReadErrs) System.out.printf("%d BAD line == %s%n", count, line);
        continue;
      }

      int fldidx = 0;
      try {
        int classId = Integer.parseInt(flds[fldidx++]);
        int xy = Integer.parseInt(flds[fldidx++]);
        String name = StringUtil.remove(flds[fldidx++], '"');
        String units = StringUtil.remove(flds[fldidx++], '"');
        int scale = Integer.parseInt(clean(flds[fldidx++]));
        int refVal = Integer.parseInt(clean(flds[fldidx++]));
        int width = Integer.parseInt(clean(flds[fldidx++]));

        int x = xy / 1000;
        int y = xy % 1000;

        b.addDescriptor((short) x, (short) y, scale, refVal, width, name, units);

      } catch (Exception e) {
        if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
  }

  static private String clean(String s) {
    return StringUtil.remove(s, ' ');
  }

  static public TableB readRobbTableB(String tablename) throws IOException {
    InputStream ios = open(tablename);
    TableB b = new TableB(tablename, tablename);

    readRobbTableB(ios, b);
    return b;
  }

  // tables are in robb's format
  static private TableB readRobbTableB(InputStream ios, TableB b) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

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

        b.addDescriptor(x, y, scale, refVal, width, split[7], split[6]);
      } catch (Exception e) {
        log.error("Bad table " + b.getName() + " entry=<" + line + ">", e);
      }
    }
    dataIS.close();

    return b;
  }

  // F-XX-YYY |SCALE| REFERENCE   | BIT |      UNIT      | MNEMONIC ;DESC ;  ELEMENT NAME
  static private TableB readNcepTableB(InputStream ios, TableB b) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    dataIS.readLine(); // throw first line away

    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#") || line.length() == 0)
        continue;

      try {
        String[] flds = line.split("[\\|;]");
        if (flds[0].equals("END")) break;

        if (flds.length < 8) {
          log.error("Bad line in table " + b.getName() + " entry=<" + line + ">");
          continue;
        }

        String fxys = flds[0];
        int scale = Integer.parseInt(clean(flds[1]));
        int refVal = Integer.parseInt(clean(flds[2]));
        int width = Integer.parseInt(clean(flds[3]));
        String units = StringUtil.remove(flds[4], '"');
        String name = StringUtil.remove(flds[7], '"');

        String[] xyflds = fxys.split("-");
        short x = Short.parseShort(clean(xyflds[1]));
        short y = Short.parseShort(clean(xyflds[2]));

        b.addDescriptor(x, y, scale, refVal, width, name, units);

        /* System.out.println("Table B line =" + line);
       System.out.printf("%s = %d %d, %d %d %d %s %s %n", fxys, x, y, scale, refVal, width, name, units);
       if (count > 10) break;
       count++; */

      } catch (Exception e) {
        log.error("Bad table " + b.getName() + " entry=<" + line + ">", e);
      }


    }
    dataIS.close();

    return b;
  }

  /*
  fxy    name                                                             units                   scale  ref         w  units
  01234567                                                                 72                       97   102            119
   001015 STATION OR SITE NAME                                             CCITTIA5                   0            0 160 CHARACTER                 0        20
   001041 ABSOLUTE PLATFORM VELOCITY - FIRST COMPONENT (SEE NOTE 6)        M/S                        5  -1073741824  31 M/S                       5        10
  */
  static private TableB readEcmwfTableB(InputStream ios, TableB b) throws IOException {
    int count = 0;
    List<TableParser.Record> recs = TableParser.readTable(ios, "4i,7i,72,97,102i,114i,119i", 50000);
    for (TableParser.Record record : recs) {
      int x = (Integer) record.get(0);
      int y = (Integer) record.get(1);
      String name = (String) record.get(2);
      String units = (String) record.get(3);
      int scale = (Integer) record.get(4);
      int ref = (Integer) record.get(5);
      int width = (Integer) record.get(6);

      b.addDescriptor((short) x, (short) y, scale, ref, width, name, units);

      /* System.out.println("Table B line =" + record);
    System.out.printf("%d %d, %d %d %d %s %s %n", x, y, scale, ref, width, name, units);
    if (count > 10) break;
    count++;  */
    }
    ios.close();

    return b;
  }

  static private void readBmetTableB(InputStream ios, TableB b) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(ios);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    Element root = doc.getRootElement();
    List<Element> featList = root.getChildren("featureCatalogue");
    for (Element featureCat : featList) {
      List<Element> features = featureCat.getChildren("feature");

      for (Element feature : features) {
        String name = feature.getChild("annotation").getChildTextNormalize("documentation");
        int f = Integer.parseInt(feature.getChildText("F"));
        int x = Integer.parseInt(feature.getChildText("X"));
        int y = Integer.parseInt(feature.getChildText("Y"));
        int fxy = (f << 16) + (x << 8) + y;

        Element bufrElem = feature.getChild("BUFR");
        String units = bufrElem.getChildTextNormalize("BUFR_units");
        int scale = 0, reference = 0, width = 0;

        String s = null;
        try {
          s = bufrElem.getChildTextNormalize("BUFR_scale");
          scale = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad scale='%s'%n", fxy, name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_reference");
          reference = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad reference='%s' %n", fxy, name, s);
        }

        try {
          s = bufrElem.getChildTextNormalize("BUFR_width");
          width = Integer.parseInt(clean(s));
        } catch (NumberFormatException e) {
          System.out.printf(" key %s name '%s' has bad width='%s' %n", fxy, name, s);
        }

        b.addDescriptor((short) x, (short) y, scale, reference, width, name, units);
      }
    }
  }


  ///////////////////////////////////////////////////////

  static public TableD readTableD(String location, String mode) throws IOException {
    InputStream ios = openStream(location);
    TableD b = new TableD(location, location);
    if (mode.equals("wmo"))
      readWmoTableD(ios, b);
    else if (mode.equals("ncep"))
      readNcepTableD(ios, b);
    else if (mode.equals("ecmwf"))
      readEcmwfTableD(ios, b);
    else
      readRobbTableD(ios, b);

    return b;
  }

  static public TableD getWmoTableD() throws IOException {
    String tablename = RESOURCE_PATH + "wmo/B_TableD.csv";
    InputStream ios = BufrTables.class.getResourceAsStream(tablename);
    TableD d = new TableD(tablename, tablename);

    readWmoTableD(ios, d);
    return d;
  }

  static private void readWmoTableD(InputStream ios, TableD tableD) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF-8")));
    int count = 0;
    int currSeqno = -1;
    TableD.Descriptor currDesc = null;

    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#")) continue;
      count++;

      if (count == 1) {
        if (showReadErrs) System.out.println("header line == " + line);
        continue;
      }

      // commas embedded in quotes - replace with blanks for now
      int pos1 = line.indexOf('"');
      if (pos1 >= 0) {
        int pos2 = line.indexOf('"', pos1 + 1);
        StringBuffer sb = new StringBuffer(line);
        for (int i = pos1; i < pos2; i++)
          if (sb.charAt(i) == ',') sb.setCharAt(i, ' ');
        line = sb.toString();
      }

      String[] flds = line.split(",");
      if (flds.length < 5) {
        if (showReadErrs) System.out.printf("%d INCOMPLETE line == %s%n", count, line);
        continue;
      }

      int fldidx = 0;
      try {
        int sno = Integer.parseInt(flds[fldidx++]);
        int cat = Integer.parseInt(flds[fldidx++]);
        int seq = Integer.parseInt(flds[fldidx++]);
        String seqName = flds[fldidx++];
        String featno = flds[fldidx++];
        if (featno.trim().length() == 0) {
          if (showReadErrs) System.out.printf("%d skip line == %s%n", count, line);
          continue;
        }
        String featName = (flds.length > 5) ? flds[fldidx++] : "n/a";

        if (currSeqno != seq) {
          int y = seq % 1000;
          int w = seq / 1000;
          int x = w % 100;
          seqName = StringUtil.remove(seqName, '"');
          currDesc = tableD.addDescriptor((short) x, (short) y, seqName, new ArrayList<Short>());
          currSeqno = seq;
        }

        int fno = Integer.parseInt(featno);
        int y = fno % 1000;
        int w = fno / 1000;
        int x = w % 100;
        int f = w / 100;

        int fxy = (f << 14) + (x << 8) + y;
        currDesc.addFeature((short) fxy);

      } catch (Exception e) {
        if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
  }

  static public TableD getTableD(String tablename) throws IOException {
    TableD d = tablesD.get(tablename);
    if (d == null) {
      d = readRobbTableD(tablename);
      tablesD.put(tablename, d);
    }
    return d;
  }


  static void addTableD(String tablename, TableD d) {
    tablesD.put(tablename, d);
  }

  // local tables are in robb format
  static public TableD readRobbTableD(String tablename) throws IOException {
    InputStream ios = open(tablename);
    TableD t = new TableD(tablename, tablename);

    readRobbTableD(ios, t);
    return t;
  }

  static private void readRobbTableD(InputStream ios, TableD t) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;

    // read table D to store sequences and their descriptors
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      count++;
      // check for comment lines
      if (line.startsWith("#") || line.length() == 0)
        continue;

      String[] split = line.split("[ \t]+"); // 1 or more whitespace
      if (split[0].equals("END")) break;
      
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
          seqName = StringUtil.remove(seqName, "()");
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
              t.addDescriptor(seqX, seqY, seqName, seq);
              break;
            }
          }
        }
      } catch (Exception e) {
        log.warn("TableD " + t.getName() + " Failed on line " + count + " = " + line + "\n " + e);
        e.printStackTrace();
      }
    }
    dataIS.close();
  }

  /*
  3-00-010 | DELAYREP   ;     ; Table D sequence definition
           | 3-00-003 > | Table D descriptor to be defined
           | 1-01-000 > | Delayed replication of 1 descriptor
           | 0-31-001 > | Delayed descriptor replication factor
           | 0-00-030   | Descriptor defining sequence

  3-01-001 | WMOBLKST   ;     ;
           | 0-01-001 > | WMO block number
           | 0-01-002   | WMO station number

    */
  static private void readNcepTableD(InputStream ios, TableD t) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    dataIS.readLine(); // throw first line away

    TableD.Descriptor currDesc = null;

    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#") || line.trim().length() == 0)
        continue;
      //System.out.println("Table D line =" + line);

      try {
        String[] flds = line.split("[\\|;]");
        if (flds[0].equals("END")) break;

        String fxys = flds[0].trim();

        if (fxys.length() > 0) {
          String[] xyflds = fxys.split("-");
          short x = Short.parseShort(clean(xyflds[1]));
          short y = Short.parseShort(clean(xyflds[2]));
          String seqName = (flds.length > 3) ? flds[3].trim() : "";
          currDesc = t.addDescriptor((short) x, (short) y, seqName, new ArrayList<Short>());
          //System.out.printf("Add seq %s = %d %d %s %n", fxys, x, y, seqName);
        } else {
          fxys = StringUtil.remove(flds[1], '>');
          String[] xyflds = fxys.split("-");
          short f = Short.parseShort(clean(xyflds[0]));
          short x = Short.parseShort(clean(xyflds[1]));
          short y = Short.parseShort(clean(xyflds[2]));
          int fxy = (f << 14) + (x << 8) + y;
          currDesc.addFeature((short) fxy);
          //System.out.printf("Add %s = %d %d %d%n", fxys, f, x, y);
        }

       //if (count > 10) break;
       //count++;

      } catch (Exception e) {
        log.error("Bad table " + t.getName() + " entry=<" + line + ">", e);
      }


    }
    dataIS.close();
  }

  /*
 300002  2 000002
           000003
 300003  3 000010
           000011
           000012
   */
   static private void readEcmwfTableD(InputStream ios, TableD t) throws IOException {

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    TableD.Descriptor currDesc = null;

    int n = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.startsWith("#") || line.length() == 0)
        continue;
      //System.out.println("Table D line =" + line);

      try {
        String fxys;
        String[] flds = line.split("[\\s]+");
        if (n == 0) {
          fxys = flds[0].trim();
          int fxy = Integer.parseInt(fxys);
          int y = fxy % 1000;
          fxy /= 1000;
          int x = fxy % 100;
          currDesc = t.addDescriptor((short) x, (short) y, "", new ArrayList<Short>());
          //System.out.printf("Add seq %s = %d %d%n", fxys, x, y);
          n = Integer.parseInt(flds[1]);
          fxys = flds[2].trim();
        } else {
          fxys = flds[0].trim();
        }

        int fxy = Integer.parseInt(fxys);
        int y = fxy % 1000;
        fxy /= 1000;
        int x = fxy % 100;
        int f = fxy /= 100;
        fxy = (f << 14) + (x << 8) + y;
        currDesc.addFeature((short) fxy);
        n--;
        //System.out.printf("Add %s = %d %d %d%n", fxys, f, x, y);
        //if (count > 10) break;
        //count++;

      } catch (Exception e) {
        log.error("Bad table " + t.getName() + " entry=<" + line + ">", e);
      }
    }
    dataIS.close();
  }
  /////////////////////////////////////////////////////////////////////////////////////////

  private static InputStream open(String location) throws IOException {
    InputStream ios = null;

    // Try class loader to get resource
    String tmp = RESOURCE_PATH + "local/" + location;
    ios = BufrTables.class.getResourceAsStream(tmp);
    if (ios != null) {
      if (debugTable) System.out.printf("BufrTables open %s %n", tmp);
      return ios;
    }

    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }
    return ios;
  }

  private static InputStream openStream(String location) throws IOException {
    InputStream ios = null;

    if (location.startsWith("resource:")) {
      location = location.substring(9);
      String tmp = RESOURCE_PATH + location;
      ios = BufrTables.class.getResourceAsStream(tmp);
      if (ios != null) {
        if (debugTable) System.out.printf("BufrTables open %s %n", tmp);
        return ios;
      }
    }

    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }
    return ios;
  }


  // debug
  public static void main(String args[]) throws IOException {
    Formatter out = new Formatter(System.out);

    TableA tableA = BufrTables.getWmoTableA();
    tableA.show(out);

    TableB tableB = BufrTables.getWmoTableB();
    tableB.show(out);

    TableD tableD = BufrTables.getWmoTableD();
    tableD.show(out);
  }
}
