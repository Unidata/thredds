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

/*
 * BufrRead mnemonic.java  1.0  05/09/2008
 * @author  Robb Kambic
 * @version 1.0
 */

import ucar.nc2.iosp.bufr.*;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.Integer;

/**
 * A class that reads a  mnemonic table. It doesn't include X < 48 and Y < 192 type of
 * descriptors because they are already stored in the latest WMO tables.
 */

public class NcepMnemonic {

  //| HEADR    | 362001 | TABLE D ENTRY - PROFILE COORDINATES                      |                      |
  private static final Pattern fields3 =
      Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s*\\|");
  private static final Pattern fields2 =
      Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");
  private static final Pattern fields5 =
      Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");
  /**
   * Pattern to get 3 integers from beginning of line.
   */
  private static final Pattern ints0123 = Pattern.compile("^(0|1|2|3)");
  private static final Pattern ints6 = Pattern.compile("^\\d{6}");

  private static final int XlocalCutoff = 48;
  private static final int YlocalCutoff = 192;

  /**
   * debug.
   */
  private static final boolean debugTable = false;

  /**
   * Used to read mnemonic tables.
   *
   * @return boolean
   * @throws IOException
   */
  public static boolean read(InputStream ios, BufrTables.Tables tables) throws IOException {
    if (ios == null)
      return false;

    if (tables.b == null)
      tables.b = new TableB("fake", "fake");
    if (tables.d == null)
      tables.d = new TableD("fake", "fake");

    // read this mnemonic table from NWS
    HashMap<String, String> number = new HashMap<String, String>();
    HashMap<String, String> desc = new HashMap<String, String>();
    HashMap<String, String> mnseq = new HashMap<String, String>();
    //Map<Short, TableB.Descriptor> descriptors = new HashMap<Short, TableB.Descriptor>();
    //Map<Short, TableD.Descriptor> sequences = new HashMap<Short, TableD.Descriptor>();


    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));

    // read  mnemonic table
    Matcher m;
    // read header info and disregard
    while (true) {
      String line = dataIS.readLine();
      //if (line == null) break;
      if (line.contains("MNEMONIC")) break;
    }
    // read mnemonic, number, and description
    //| HEADR    | 362001 | TABLE D ENTRY - PROFILE COORDINATES                      |
    while (true) {
      String line = dataIS.readLine();
      //if (line == null) break;
      if (line.contains("MNEMONIC")) break;
      if (line.contains("----")) continue;
      if (line.startsWith("*")) continue;
      if (line.startsWith("|       ")) continue;
      m = fields3.matcher(line);
      if (m.find()) {
        if (m.group(2).startsWith("3")) {
          number.put(m.group(1).trim(), m.group(2));
          desc.put(m.group(1).trim(), m.group(3).replace("TABLE D ENTRY - ", "").trim());
        } else if (m.group(2).startsWith("0")) {
          number.put(m.group(1).trim(), m.group(2));
          desc.put(m.group(1).trim(), m.group(3).replace("TABLE B ENTRY - ", "").trim());
        } else if (m.group(2).startsWith("A")) {
          number.put(m.group(1).trim(), m.group(2));
          desc.put(m.group(1).trim(), m.group(3).replace("TABLE A ENTRY - ", "").trim());
        }
      } else if (debugTable) {
        System.out.println("bad mnemonic, number, and description: " + line);
      }
    }
    // read in sequences using mnemonics
    //| ETACLS1  | HEADR {PROFILE} SURF FLUX HYDR D10M {SLYR} XTRA                   |
    while (true) {
      String line = dataIS.readLine();
      //if (line == null) break;
      if (line.contains("MNEMONIC")) break;
      if (line.contains("----")) continue;
      if (line.startsWith("|       ")) continue;
      if (line.startsWith("*")) continue;
      m = fields2.matcher(line);
      if (m.find()) {
        if (mnseq.containsKey(m.group(1).trim())) {
          String value = mnseq.get(m.group(1).trim());
          value = value + " " + m.group(2);
          mnseq.put(m.group(1).trim(), value);
        } else {
          mnseq.put(m.group(1).trim(), m.group(2));
        }
      } else if (debugTable) {
        System.out.println("bad sequence mnemonic: " + line);
      }
    }

    //tableD = new TableD(name, location);

    // create sequences, replacing mnemonics with numbers
    Iterator it = mnseq.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      String seq = mnseq.get(key);
      seq = seq.replaceAll("\\<", "1-1-0 0-31-0 ");
      seq = seq.replaceAll("\\>", "");
      seq = seq.replaceAll("\\{", "1-1-0 0-31-1 ");
      seq = seq.replaceAll("\\}", "");
      seq = seq.replaceAll("\\(", "1-1-0 0-31-2 ");
      seq = seq.replaceAll("\\)", "");

      StringTokenizer stoke = new StringTokenizer(seq, " ");
      List<Short> list = new ArrayList<Short>();
      while (stoke.hasMoreTokens()) {
        String mn = stoke.nextToken();
        if (mn.charAt(1) == '-') {
          list.add(Descriptor.getFxy(mn));
          continue;
        }
        // element descriptor needs hypens
        m = ints6.matcher(mn);
        if (m.find()) {
          String F = mn.substring(0, 1);
          String X = removeLeading0(mn.substring(1, 3));
          String Y = removeLeading0(mn.substring(3));
          list.add(Descriptor.getFxy(F + "-" + X + "-" + Y));
          continue;
        }
        if (mn.startsWith("\"")) {
          int idx = mn.lastIndexOf('"');
          String count = mn.substring(idx + 1);
          list.add(Descriptor.getFxy("1-1-" + count));
          mn = mn.substring(1, idx);

        }
        if (mn.startsWith(".")) {
          String des = mn.substring(mn.length() - 4);
          mn = mn.replace(des, "....");

        }
        String fxy = number.get(mn);
        String F = fxy.substring(0, 1);
        String X = removeLeading0(fxy.substring(1, 3));
        String Y = removeLeading0(fxy.substring(3));
        list.add(Descriptor.getFxy(F + "-" + X + "-" + Y));
      }

      String fxy = number.get(key);
      String F = fxy.substring(0, 1);
      if (F.equals("A"))
        F = "3";
      String X = removeLeading0(fxy.substring(1, 3));
      String Y = removeLeading0(fxy.substring(3));
      // these are in latest tables
      if (XlocalCutoff > Integer.parseInt(X) && YlocalCutoff > Integer.parseInt(Y))
        continue;
      //key = F + "-" + X + "-" + Y;

      short seqX = Short.parseShort(X.trim());
      short seqY = Short.parseShort(Y.trim());

      tables.d.addDescriptor(seqX, seqY, key, list);
      //short id = Descriptor.getFxy(key);
      //sequences.put(Short.valueOf(id), tableD);
    }

    // add some static repetition sequences
    // LOOK why?
    List<Short> list = new ArrayList<Short>();
    // 16 bit delayed repetition
    list.add(Descriptor.getFxy("1-1-0"));
    list.add(Descriptor.getFxy("0-31-2"));
    tables.d.addDescriptor((short) 60, (short) 1, "", list);
    //tableD = new DescriptorTableD("", "3-60-1", list, false);
    //tableD.put( "3-60-1", d);
    //short id = Descriptor.getFxy("3-60-1");
    //sequences.put(Short.valueOf(id), tableD);

    list = new ArrayList<Short>();
    // 8 bit delayed repetition
    list.add(Descriptor.getFxy("1-1-0"));
    list.add(Descriptor.getFxy("0-31-1"));
    tables.d.addDescriptor((short) 60, (short) 2, "", list);
    //tableD = new DescriptorTableD("", "3-60-2", list, false);
    //tableD.put( "3-60-2", d);
    //id = Descriptor.getFxy("3-60-2");
    //sequences.put(Short.valueOf(id), tableD);

    list = new ArrayList<Short>();
    // 8 bit delayed repetition
    list.add(Descriptor.getFxy("1-1-0"));
    list.add(Descriptor.getFxy("0-31-1"));
    tables.d.addDescriptor((short) 60, (short) 3, "", list);
    //tableD = new DescriptorTableD("", "3-60-3", list, false);
    //tableD.put( "3-60-3", d);
    //id = Descriptor.getFxy("3-60-3");
    //sequences.put(Short.valueOf(id), tableD);

    list = new ArrayList<Short>();
    // 1 bit delayed repetition
    list.add(Descriptor.getFxy("1-1-0"));
    list.add(Descriptor.getFxy("0-31-0"));
    tables.d.addDescriptor((short) 60, (short) 4, "", list);
    //tableD = new DescriptorTableD("", "3-60-4", list, false);
    //tableD.put( "3-60-4", d);
    //id = Descriptor.getFxy("3-60-4");
    //sequences.put(Short.valueOf(id), tableD);

    // add in element descriptors
    //  MNEMONIC | SCAL | REFERENCE   | BIT | UNITS
    //| FTIM     |    0 |           0 |  24 | SECONDS                  |-------------|

    //tableB = new TableB(tablename, tablename);

    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.contains("MNEMONIC")) break;
      if (line.startsWith("|       ")) continue;
      if (line.startsWith("*")) continue;
      m = fields5.matcher(line);
      if (m.find()) {
        if (m.group(1).equals("")) {
          continue;
        } else if (number.containsKey(m.group(1).trim())) { // add descriptor to tableB
          String fxy = number.get(m.group(1).trim());
          String F = fxy.substring(0, 1);
          String X = fxy.substring(1, 3);
          String Y = fxy.substring(3);
          String name = desc.get(m.group(1).trim());
          // scale = m.group(2)
          // reference = m.group(3)
          // width = m.group(4)
          // units = m.group(5)
          // these are in latest tables so skip
          if (XlocalCutoff > Integer.parseInt(X) && YlocalCutoff > Integer.parseInt(Y))
            continue;

          //   void addDescriptor(short x, short y, int scale, int refVal, int width, String name, String units) {

          short x = Short.parseShort(X.trim());
          short y = Short.parseShort(Y.trim());
          int scale = Integer.parseInt(m.group(2).trim());
          int refVal = Integer.parseInt(m.group(3).trim());
          int width = Integer.parseInt(m.group(4).trim());
          tables.b.addDescriptor(x, y, scale, refVal, width, name, m.group(5).trim());

          //  public DescriptorTableB(String f, String x, String y, String scale, String refVal,
          //    String width, String uStr, String desc )
          // DescriptorTableB des = new DescriptorTableB(F, X, Y, m.group(2), m.group(3),
          //       m.group(4), m.group(5), name);
          //tableB.put(des.getKey(), des);
          //short bid = Descriptor.getFxy(des.getKey());
          //descriptors.put(Short.valueOf( bid ), des);

        } else if (debugTable) {
          System.out.println("bad element descriptors: " + line);
        }
      }
    }

    // LOOK why ?
    // default for NCEP
    // 0; 63; 0; 0; 0; 16; Numeric; Byte count
    //DescriptorTableB des = new DescriptorTableB( "0", "63", "0", "0", "0", "16", "Numeric", "Byte count");
    //short bid = Descriptor.getFxy("0-63-0");
    //descriptors.put(Short.valueOf( bid ), des);
    tables.b.addDescriptor((short) 63, (short) 0, 0, 0, 16, "Byte count", "Numeric");

    // add new tables to master MAP of all tables
    //BufrTables.tablesA.put( mnemonic, tableA );
    //BufrTables.tablesB.put(tablename, tableB );
    //BufrTables.tablesD.put(tablename, this.tableD);

    dataIS.close();

    return true;
  }

  private static String removeLeading0(String number) {
    if (number.length() == 2 && number.startsWith("0")) {
      number = number.substring(1);
    } else if (number.length() == 3 && number.startsWith("00")) {
      number = number.substring(2);
    } else if (number.length() == 3 && number.startsWith("0")) {
      number = number.substring(1);
    }
    return number;
  } // end

  // getters and setters

  // debug

  public static void main(String args[]) throws IOException {

    // Try class loader to get resource
    InputStream ios = BufrTables.openStream("bufrtab.ETACLS1");
    BufrTables.Tables tables = new BufrTables.Tables();
    NcepMnemonic.read(ios, tables);

    Formatter out = new Formatter(System.out);
    tables.b.show(out);
    tables.d.show(out);

    //TableB tableB = BufrTables.getTableB("B3L-059-003-B.diff");
    //tableB.show(out);

    // TableD tableD = BufrTables.getTableD("B4M-000-013-D");
    //tableD.show(out);

  }

}