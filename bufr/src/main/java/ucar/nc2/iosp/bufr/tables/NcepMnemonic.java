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
package ucar.nc2.iosp.bufr.tables;

/*
 * BufrRead mnemonic.java  1.0  05/09/2008
 * @author  Robb Kambic
 * @version 1.0
 */

import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

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
  private static final Pattern fields3 = Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s*\\|");
  private static final Pattern fields2 = Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");
  private static final Pattern fields5 = Pattern.compile("^\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|\\s+(.*)\\s+\\|");

  /**
   * Pattern to get 3 integers from beginning of line.
   */
  private static final Pattern ints6 = Pattern.compile("^\\d{6}");

  private static final int XlocalCutoff = 48;
  private static final int YlocalCutoff = 192;

  private static final boolean debugTable = false;

  /**
   * Read NCEP mnemonic BUFR tables.
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

    HashMap<String, String> number = new HashMap<>(); // key = mnemonic value = fxy
    HashMap<String, String> desc = new HashMap<>(); // key = mnemonic value = description
    HashMap<String, String> mnseq = new HashMap<>();

    try {
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios, CDM.utf8Charset));

      // read  mnemonic table
      Matcher m;
      // read header info and disregard
      while (true) {
        String line = dataIS.readLine();
        if (line == null) throw new RuntimeException("Bad NCEP mnemonic BUFR table ");
        if (line.contains("MNEMONIC")) break;
      }
      // read mnemonic, number, and description
      //| HEADR    | 362001 | TABLE D ENTRY - PROFILE COORDINATES                      |
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.contains("MNEMONIC")) break;
        if (line.contains("----")) continue;
        if (line.startsWith("*")) continue;
        if (line.startsWith("|       ")) continue;
        m = fields3.matcher(line);
        if (m.find()) {
          String mnu = m.group(1).trim();
          String fxy = m.group(2).trim();
          if (fxy.startsWith("3")) {
            number.put(mnu, fxy);
            desc.put(mnu, m.group(3).replace("TABLE D ENTRY - ", "").trim());
          } else if (fxy.startsWith("0")) {
            number.put(mnu, fxy);
            desc.put(mnu, m.group(3).replace("TABLE B ENTRY - ", "").trim());
          } else if (fxy.startsWith("A")) {
            number.put(mnu, fxy);
            desc.put(mnu, m.group(3).replace("TABLE A ENTRY - ", "").trim());
          }
        } else if (debugTable) {
          System.out.println("bad mnemonic, number, and description: " + line);
        }
      }
      // read in sequences using mnemonics
      //| ETACLS1  | HEADR {PROFILE} SURF FLUX HYDR D10M {SLYR} XTRA                   |
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        if (line.contains("MNEMONIC")) break;
        if (line.contains("----")) continue;
        if (line.startsWith("|       ")) continue;
        if (line.startsWith("*")) continue;
        m = fields2.matcher(line);
        if (m.find()) {
          String mnu = m.group(1).trim();
          if (mnseq.containsKey(mnu)) { // concat lines with same mnu
            String value = mnseq.get(mnu);
            value = value + " " + m.group(2);
            mnseq.put(mnu, value);
          } else {
            mnseq.put(mnu, m.group(2));
          }
        } else if (debugTable) {
          System.out.println("bad sequence mnemonic: " + line);
        }
      }

      // create sequences, replacing mnemonics with numbers
      for (Map.Entry<String, String> ent : mnseq.entrySet()) {
        String seq = ent.getValue();
        seq = seq.replaceAll("\\<", "1-1-0 0-31-0 ");
        seq = seq.replaceAll("\\>", "");
        seq = seq.replaceAll("\\{", "1-1-0 0-31-1 ");
        seq = seq.replaceAll("\\}", "");
        seq = seq.replaceAll("\\(", "1-1-0 0-31-2 ");
        seq = seq.replaceAll("\\)", "");

        StringTokenizer stoke = new StringTokenizer(seq, " ");
        List<Short> list = new ArrayList<>();
        while (stoke.hasMoreTokens()) {
          String mn = stoke.nextToken();
          if (mn.charAt(1) == '-') {
            list.add(Descriptor.getFxy(mn));
            continue;
          }
          // element descriptor needs hyphens
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

        String fxy = number.get(ent.getKey());
        String X = removeLeading0(fxy.substring(1, 3));
        String Y = removeLeading0(fxy.substring(3));
        // these are in latest tables
        if (XlocalCutoff > Integer.parseInt(X) && YlocalCutoff > Integer.parseInt(Y))
          continue;
        //key = F + "-" + X + "-" + Y;

        short seqX = Short.parseShort(X.trim());
        short seqY = Short.parseShort(Y.trim());

        tables.d.addDescriptor(seqX, seqY, ent.getKey(), list);
        //short id = Descriptor.getFxy(key);
        //sequences.put(Short.valueOf(id), tableD);
      }

      // add some static repetition sequences
      // LOOK why?
      List<Short> list = new ArrayList<>();
      // 16 bit delayed repetition
      list.add(Descriptor.getFxy("1-1-0"));
      list.add(Descriptor.getFxy("0-31-2"));
      tables.d.addDescriptor((short) 60, (short) 1, "", list);
      //tableD = new DescriptorTableD("", "3-60-1", list, false);
      //tableD.put( "3-60-1", d);
      //short id = Descriptor.getFxy("3-60-1");
      //sequences.put(Short.valueOf(id), tableD);

      list = new ArrayList<>();
      // 8 bit delayed repetition
      list.add(Descriptor.getFxy("1-1-0"));
      list.add(Descriptor.getFxy("0-31-1"));
      tables.d.addDescriptor((short) 60, (short) 2, "", list);
      //tableD = new DescriptorTableD("", "3-60-2", list, false);
      //tableD.put( "3-60-2", d);
      //id = Descriptor.getFxy("3-60-2");
      //sequences.put(Short.valueOf(id), tableD);

      list = new ArrayList<>();
      // 8 bit delayed repetition
      list.add(Descriptor.getFxy("1-1-0"));
      list.add(Descriptor.getFxy("0-31-1"));
      tables.d.addDescriptor((short) 60, (short) 3, "", list);
      //tableD = new DescriptorTableD("", "3-60-3", list, false);
      //tableD.put( "3-60-3", d);
      //id = Descriptor.getFxy("3-60-3");
      //sequences.put(Short.valueOf(id), tableD);

      list = new ArrayList<>();
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
            String X = fxy.substring(1, 3);
            String Y = fxy.substring(3);
            String mnu = m.group(1).trim();
            String descr = desc.get(mnu);

            short x = Short.parseShort(X.trim());
            short y = Short.parseShort(Y.trim());

            // these are in latest tables so skip  LOOK WHY
            if (XlocalCutoff > x && YlocalCutoff > y)
              continue;

            int scale = Integer.parseInt(m.group(2).trim());
            int refVal = Integer.parseInt(m.group(3).trim());
            int width = Integer.parseInt(m.group(4).trim());
            String units = m.group(5).trim();

            tables.b.addDescriptor(x, y, scale, refVal, width, mnu, units, descr);

          } else if (debugTable) {
            System.out.println("bad element descriptors: " + line);
          }
        }
      }

    } finally {
      ios.close();
    }

    // LOOK why ?
    // default for NCEP
    // 0; 63; 0; 0; 0; 16; Numeric; Byte count
    tables.b.addDescriptor((short) 63, (short) 0, 0, 0, 16, "Byte count", "Numeric", null);

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
  }

  /**
   * Read NCEP mnemonic BUFR tables.
   */
  private static void readSubCategories(String fileIn, PrintStream out, String token) throws IOException {
    System.out.printf("%s%n", fileIn);
    try (FileInputStream in = new  FileInputStream(fileIn)) {
      BufferedReader dataIS = new BufferedReader(new InputStreamReader(in,
              CDM.utf8Charset));
      while (true) {
        String line = dataIS.readLine();
        if (line == null) break;
        int posb = line.indexOf("DISCONTINUED");
        if (posb > 0) continue;
        posb = line.indexOf("NO LONGER");
        if (posb > 0) continue;
        posb = line.indexOf("WAS REPLACED");
        if (posb > 0) continue;

        int pos = line.indexOf(token);
        if (pos < 0) continue;
        System.out.printf("%s%n", line);

        boolean is31 = token.equals("031-");
        String subline = is31 ? line.substring(pos) : line.substring(pos + token.length());
        //if (is31) System.out.printf(" '%s'%n", subline);

        int pos2 = subline.indexOf(' ');
        String catS = subline.substring(0, pos2);
        String desc = subline.substring(pos2 + 1);
        //System.out.printf("   cat='%s'%n", catS);
        //System.out.printf("  desc='%s'%n", desc);
        int cat = Integer.parseInt(catS.substring(0, 3));
        int subcat = Integer.parseInt(catS.substring(4, 7));
        desc = StringUtil2.remove(desc, '|').trim();
        //System.out.printf("  cat=%d subcat=%d%n", cat,subcat);
        //System.out.printf("  desc='%s'%n", desc);
        //System.out.printf("%d, %d, %s%n", cat, subcat, desc);
        out.printf("%d; %d; %s%n", cat, subcat, desc);
      }
    }


  }

  public static void main(String args[]) throws IOException {
    String fileOut = "resource:/resources/bufrTables/local/ncep/DataSubCategories.csv";
    try (PrintStream pout = new PrintStream(fileOut, CDM.UTF8)) {
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.000.txt", pout, "MSG TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.001.txt", pout, "MESSAGE TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.002.txt", pout, "MSG TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.003.txt", pout, "MTYP ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.004.txt", pout, "MSG TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.005.txt", pout, "MSG TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.006.txt", pout, "M TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.007.txt", pout, "MTYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.008.txt", pout, "MSG TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.012.txt", pout, "M TYPE ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.021.txt", pout, "MTYP ");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.031.txt", pout, "031-");
      readSubCategories("C:/dev/github/thredds/bufr/src/main/sources/ncep/bufrtab.255.txt", pout, "MTYP ");
    }
    System.out.printf("=======================================%n");
    System.out.printf("%s%n", IO.readFile(fileOut));
    System.exit(0);

    String location = "resource:/resources/bufrTables/local/ncep/ncep.bufrtab.ETACLS1";
    try (InputStream ios = BufrTables.openStream(location)) {
      BufrTables.Tables tables = new BufrTables.Tables();
      NcepMnemonic.read(ios, tables);

      Formatter out = new Formatter(System.out);
      tables.b.show(out);
      tables.d.show(out);
    }

  }

}