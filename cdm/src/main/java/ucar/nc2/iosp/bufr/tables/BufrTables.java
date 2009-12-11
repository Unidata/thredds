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
import ucar.nc2.iosp.bufr.BufrIdentificationSection;
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

  private static final boolean debugTable = false;
  private static final boolean showReadErrs = false;

  private static List<TableConfig> tables;
  private static TableA tableA;
  private static Map<String, TableB> tablesB = new ConcurrentHashMap<String, TableB>();
  private static Map<String, TableD> tablesD = new ConcurrentHashMap<String, TableD>();

  private static final Pattern category = Pattern.compile("^\\s*(\\w+)\\s+(.*)");
  private static final Pattern threeInts = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)");  // get 3 integers from beginning of line
  private static final Pattern negOne = Pattern.compile("^\\s*-1");  // check for -1 sequence terminator

  // center,subcenter,master,local,cat,tableB,tableBformat,tableD,tableDformat
  static private void readTableLookup() {
    tables = new ArrayList<TableConfig>();

    String name = RESOURCE_PATH + "local/tablelookup.csv";
    try {
      InputStream ios = openStream("resource:" + name);
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, Charset.forName("UTF8")));
      int count = 0;
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.startsWith("#")) continue;
        count++;

        // first line is the header
        if (count == 1) {
          if (showReadErrs) System.out.println("header line == " + line);
          continue;
        }

        String[] flds = line.split(",");
        if (flds.length < 8) {
          if (showReadErrs) System.out.printf("%d BAD line == %s%n", count, line);
          continue;
        }

        int fldidx = 0;
        try {
          TableConfig table = new TableConfig();
          table.center = Integer.parseInt(flds[fldidx++].trim());
          table.subcenter = Integer.parseInt(flds[fldidx++].trim());
          table.master = Integer.parseInt(flds[fldidx++].trim());
          table.local = Integer.parseInt(flds[fldidx++].trim());
          table.cat = Integer.parseInt(flds[fldidx++].trim());
          table.tableBname = flds[fldidx++].trim();
          table.tableBformat = flds[fldidx++].trim();
          table.tableDname = flds[fldidx++].trim();
          table.tableDformat = flds[fldidx++].trim();

          tables.add(table);

        } catch (Exception e) {
          if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
        }
      }
      dataIS.close();
    } catch (IOException ioe) {
      String mess = "Need BUFR tables in path; looking for " + name;
      throw new RuntimeException(mess, ioe);
    }
  }

  private static class TableConfig {
    int center, subcenter, master, local, cat;
    String tableBname,tableBformat,tableDname,tableDformat;

    boolean matches(int center, int subcenter, int master, int local, int cat) {
      if ((this.center >= 0) && (center >= 0) && (center != this.center)) return false;
      if ((this.subcenter >= 0) && (subcenter >= 0) && (subcenter != this.subcenter)) return false;
      if ((this.master >= 0) && (master >= 0) && (master != this.master)) return false;
      if ((this.local >= 0) && (local >= 0) && (local != this.local)) return false;
      if ((this.cat >= 0) && (cat >= 0) && (cat != this.cat)) return false;
      return true;
    }
  }

  private static TableConfig matchTableConfig(int center, int subcenter, int master, int local, int cat) {
    if (tables == null) readTableLookup();

    for (TableConfig tc : tables) {
      if (tc.matches(center,subcenter,master,local,cat))
        return tc;
    }
    return null;
  }

  private static TableConfig matchTableConfig(BufrIdentificationSection ids) {
    if (tables == null) readTableLookup();

    int center = ids.getCenterId();
    int subcenter = ids.getSubCenterId();
    int master = ids.getMasterTableVersion();
    int local = ids.getLocalTableVersion();
    int cat = ids.getCategory();

    return matchTableConfig(center, subcenter, master, local, cat);
  }


  /* public void tableLookup(BufrIndicatorSection is, BufrIdentificationSection ids) throws IOException {
    init();
    this.wmoTableB = BufrTables.getWmoTableB(is.getBufrEdition());
    this.wmoTableD = BufrTables.getWmoTableD(is.getBufrEdition());

    // check tablelookup for special local table
    // create key from category and possilbly center id

    String localTableName = tablelookup.getCategory( makeLookupKey(ids.getCategory(), ids.getCenterId()));

    if (localTableName == null) {
      //this.localTableB = BufrTables.getWmoTableB(is.getBufrEdition());
      //this.localTableD = BufrTables.getWmoTableD(is.getBufrEdition());
      return;

    } else if (localTableName.contains("-ABD")) {
      localTableB = BufrTables.getTableB(localTableName.replace("-ABD", "-B"));
      localTableD = BufrTables.getTableD(localTableName.replace("-ABD", "-D"));
      return;

      // check if localTableName(Mnemonic) table has already been processed
    } else if (BufrTables.hasTableB(localTableName)) {
      localTableB = BufrTables.getTableB(localTableName); // LOOK localTableName needs "-B" or something
      localTableD = BufrTables.getTableD(localTableName);
      return;
    }

    // Mnemonic tables
    ucar.nc2.iosp.bufr.tables.BufrReadMnemonic brm = new ucar.nc2.iosp.bufr.tables.BufrReadMnemonic();
    brm.readMnemonic(localTableName);
    this.localTableB = brm.getTableB();
    this.localTableD = brm.getTableD();
  }

  private short makeLookupKey(int cat, int center_id) {
    if ((center_id == 7) || (center_id == 8) || (center_id == 9)) {
      if (cat < 240 || cat > 254)
        return (short) center_id;
      return (short) (center_id * 1000 + cat);
    }
    return (short) center_id;
  }

  

    // LOOK
  static public TableA getLookupTable() throws IOException {
    String tablename = "tablelookup.txt";
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

  static private TableB readRobbTableB(String tablename) throws IOException {
    InputStream ios = open(tablename);
    TableB b = new TableB(tablename, tablename);

    readRobbTableB(ios, b);
    return b;
  }

     static public TableB getTableB(String tablename) throws IOException {
    TableB b = tablesB.get(tablename);
    if (b == null) {
      b = readRobbTableB(tablename);
      tablesB.put(tablename, b);
    }
    return b;
  }

  */

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

  public static class Tables {
    public TableB b;
    public TableD d;
    Tables() {}
    Tables(TableB b, TableD d) {
      this.b = b;
      this.d = d;
    }
  }

  static public Tables getLocalTables(BufrIdentificationSection ids) throws IOException {
    TableConfig tc = matchTableConfig(ids);
    if (tc == null) return null;

    if (tc.tableBformat.equals("ncep-nm")) {
      // see if we already have it
      TableB b = tablesB.get(tc.tableBname);
      TableD d = tablesD.get(tc.tableBname);
      if ((b != null) && (d != null)) return new Tables(b, d);

      // read it
      b = new TableB(tc.tableBname, tc.tableBname);
      d = new TableD(tc.tableBname, tc.tableBname);
      Tables t = new Tables(b, d);
      InputStream ios = openStream(tc.tableBname);
      NcepMnemonic.read(ios, t);

      // cache 
      tablesB.put(tc.tableBname, t.b);
      tablesD.put(tc.tableBname, t.d);
      return t;
    }

    Tables tables = new Tables();
    tables.b = readTableB(tc.tableBname, tc.tableBformat);
    tables.d = readTableD(tc.tableDname, tc.tableDformat);

    return tables;
  }

  static private String version13 = "wmo.v13.composite";
  static private String version14 = "wmo.v14";

  static public TableB getWmoTableB(BufrIdentificationSection ids) throws IOException {
    return getWmoTableB( ids.getMasterTableVersion());
  }

  static public TableB getWmoTableB(int version) throws IOException {
    String tableName = (version == 14) ? version14 : version13;
    TableB tb = tablesB.get(tableName);
    if (tb != null) return tb;

    // always read 14 in
    TableConfig tc14 = matchTableConfig(0, 0, 14, 0, -1);
    TableB result = readTableB(tc14.tableBname, tc14.tableBformat);
    tablesB.put(version14, result); // hash by standard name

    // everyone else uses 13 : cant override - do it in local if needed
    if (version < 14) {
      TableConfig tc = matchTableConfig(0, 0, 13, 0, -1);
      TableB b13 = readTableB(tc.tableBname, tc.tableBformat);
      TableB.Composite bb = new TableB.Composite(version13, version13);
      bb.addTable(b13); // check in 13 first, so it overrides
      bb.addTable(result); // then in 14
      result = bb;
      tablesB.put(version13, result); // hash by standard name
    }

    return result;
  }

  static public TableB readTableB(String location, String format) throws IOException {
    TableB tb = tablesB.get(location);
    if (tb != null) return tb;

    InputStream ios = openStream(location);
    TableB b = new TableB(location, location);
    if (format.equals("wmo"))
      readWmoTableB(ios, b);
    else if (format.equals("ncep"))
      readNcepTableB(ios, b);
    else if (format.equals("ncep-nm")) {
      Tables t = new Tables(b,null);
      NcepMnemonic.read(ios, t);
    } else if (format.equals("ecmwf"))
      readEcmwfTableB(ios, b);
    else if (format.equals("ukmet"))
      readBmetTableB(ios, b);
    else if (format.equals("mel-bufr"))
      readMelbufrTableB(ios, b);
    else {
      System.out.printf("Unknown format= %s %n",format);
      return null;
    }

    tablesB.put(location, b);
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
        int classId = Integer.parseInt(flds[fldidx++].trim());
        int xy = Integer.parseInt(flds[fldidx++].trim());
        String name = StringUtil.remove(flds[fldidx++], '"');
        String units = StringUtil.remove(flds[fldidx++], '"');
        int scale = Integer.parseInt(clean(flds[fldidx++].trim()));
        int refVal = Integer.parseInt(clean(flds[fldidx++].trim()));
        int width = Integer.parseInt(clean(flds[fldidx++].trim()));

        int x = xy / 1000;
        int y = xy % 1000;

        b.addDescriptor((short) x, (short) y, scale, refVal, width, name, units);

      } catch (Exception e) {
        if (showReadErrs) System.out.printf("%d %d BAD line == %s%n", count, fldidx, line);
      }
    }
    dataIS.close();
  }

  static private String clean(String s) {
    return StringUtil.remove(s, ' ');
  }

  // tables are in mel-bufr format
  static private TableB readMelbufrTableB(InputStream ios, TableB b) throws IOException {

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
        continue;
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
      if (record.nfields() < 7) {
        continue;
      }
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

  /*  static public TableD getTableD(String tablename) throws IOException {
    TableD d = tablesD.get(tablename);
    if (d == null) {
      d = readRobbTableD(tablename);
      tablesD.put(tablename, d);
    }
    return d;
  }

  // local tables are in robb format
  static public TableD readRobbTableD(String tablename) throws IOException {
    InputStream ios = open(tablename);
    TableD t = new TableD(tablename, tablename);

    readRobbTableD(ios, t);
    return t;
  } */

 static public TableD getWmoTableD(BufrIdentificationSection ids) throws IOException {
    TableD tb = tablesD.get(version14);
    if (tb != null) return tb;

    // always use version 14
    TableConfig tc14 = matchTableConfig(0, 0, 14, 0, -1);
    TableD result = readTableD(tc14.tableDname, tc14.tableDformat);
    tablesD.put(version14, result); // hash by standard name

    return result;
  }

  static public TableD readTableD(String location, String format) throws IOException {
    if (location == null) return null;
    if (location.trim().length() == 0) return null;

    TableD tb = tablesD.get(location);
    if (tb != null) return tb;

    InputStream ios = openStream(location);
    TableD d = new TableD(location, location);
    if (format.equals("wmo"))
      readWmoTableD(ios, d);
    else if (format.equals("ncep"))
      readNcepTableD(ios, d);
    else if (format.equals("ncep-nm")) {
      Tables t = new Tables(null, d);
      NcepMnemonic reader = new NcepMnemonic();
      NcepMnemonic.read(ios, t);
    } else if (format.equals("ecmwf"))
      readEcmwfTableD(ios, d);
    else if (format.equals("mel-bufr"))
      readMelbufrTableD(ios, d);
    else {
      System.out.printf("Unknown format= %s %n", format);
      return null;
    }

    tablesD.put(location, d);
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

  static private void readMelbufrTableD(InputStream ios, TableD t) throws IOException {

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

  static InputStream openStream(String location) throws IOException {
    InputStream ios = null;

    if (location.startsWith("resource:")) {
      location = location.substring(9);
      ios = BufrTables.class.getResourceAsStream(location);
      if (ios == null)
        throw new RuntimeException("resource not found=<"+location+">");
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

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Table A
  
  static public String getDataCategory(int cat) {
    String tablename = RESOURCE_PATH + "wmo/TableA-11-2008.txt";
    if (tableA == null) {
      InputStream ios = BufrTables.class.getResourceAsStream(tablename);
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
      Map<Short, String> categories = new HashMap<Short, String>();

      try {
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
            String cats = m.group(2).trim();
            categories.put(Short.valueOf(m.group(1)), cats);
          }
        }
        dataIS.close();

        tableA = new TableA(tablename, tablename, categories);
      } catch (IOException ioe) {
        String mess = "Need BUFR tables in path; looking for "+tablename;
        throw new RuntimeException(mess, ioe);
      }
    }

    return tableA.getDataCategory((short) cat);
  }

  ///////////////////////////////
  // debug
  public static void main(String args[]) throws IOException {
    Formatter out = new Formatter(System.out);

    BufrTables.getDataCategory(1); // trigger read
    tableA.show(out);

    TableB tableB = BufrTables.getWmoTableB(13);
    tableB.show(out);

    TableD tableD = BufrTables.getWmoTableD(null);
    tableD.show(out);
  }
}
