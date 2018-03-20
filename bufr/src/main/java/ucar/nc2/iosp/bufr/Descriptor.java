/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.bufr;

import ucar.nc2.iosp.bufr.tables.TableB;
import ucar.nc2.iosp.bufr.tables.TableC;
import ucar.nc2.iosp.bufr.tables.TableD;

import java.util.Formatter;

/**
 * Static methods to manipulate the f-x-y descriptors
 * @author caron
 * @since Oct 25, 2008
 */
public class Descriptor {

  static public String makeString(short fxy) {
    int f = (fxy & 0xC000) >> 14;
    int x = (fxy & 0x3F00) >> 8;
    int y = fxy & 0xFF;
    return makeString(f,x,y);
  }

  static public String makeString(int f, int x, int y) {
    Formatter out = new Formatter();
    out.format("%d-%d-%d",f,x,y);
    return out.toString();
  }

  static public boolean isWmoRange(short fxy) {
    int x = (fxy & 0x3F00) >> 8;
    int y = fxy & 0xFF;
    return (x < 48 && y < 192);
  }

  static public short getFxy(String name) {
    String[] tok = name.split("-");
    int f = (tok.length > 0) ? Integer.parseInt(tok[0]) : 0;
    int x = (tok.length > 1) ? Integer.parseInt(tok[1]) : 0;
    int y = (tok.length > 2) ? Integer.parseInt(tok[2]) : 0;
    return (short) ((f << 14) + (x << 8) + (y));
  }

  static public short getFxy2(String fxxyyy) {
    int fxy = Integer.parseInt(fxxyyy.trim());
    int y = fxy % 1000;
    fxy /= 1000;
    int x = fxy % 100;
    int f1 = fxy / 100;
    return (short) ((f1 << 14) + (x << 8) + (y));
  }

  // contains a BUFR table entry
  static public boolean isBufrTable(short fxy) {
    int f = (fxy & 0xC000) >> 14;
    int x = (fxy & 0x3F00) >> 8;
    int y = (fxy & 0xFF);
    return (f == 0) && (x == 0) && (y < 13);
  }

  static public short getFxy(short f, short x, short y) {
    return (short) ((f << 14) + (x << 8) + (y));
  }

  static private final String[] descType = {"tableB", "replication", "tableC-operators", "tableD"};

  static public void show(Formatter out, short fxy, BufrTableLookup lookup) {
    int f = (fxy & 0xC000) >> 14;

    if (f == 0) {
      TableB.Descriptor b = lookup.getDescriptorTableB( fxy);
      if (b == null) out.format("%-8s: NOT FOUND!!", makeString(fxy) );
      else out.format("%-8s: %s", b.getFxy(), b.getName());

    } else if (f == 1)
      out.format("%-8s: %s", makeString(fxy), descType[1]);

    else if (f == 2) {
      int x = (fxy & 0x3F00) >> 8;
      out.format("%-8s: Operator= %s", makeString(fxy), TableC.getOperatorName(x));

    } else if (f == 3) {
      TableD.Descriptor d = lookup.getDescriptorTableD( fxy);
      if (d == null) out.format("%-8s: NOT FOUND!!", makeString(fxy) );
      else out.format("%-8s: %s", d.getFxy(), d.getName());
    }
  }

  static public String getName(short fxy, BufrTableLookup lookup) {
    int f = (fxy & 0xC000) >> 14;

    if (f == 0) {
      TableB.Descriptor b = lookup.getDescriptorTableB( fxy);
      if (b == null) return("**NOT FOUND!!");
      else return b.getName();

    } else if (f == 1)
      return descType[1];

    else if (f == 2) {
      int x = (fxy & 0x3F00) >> 8;
      return TableC.getOperatorName(x);

    } else if (f == 3) {
      TableD.Descriptor d = lookup.getDescriptorTableD( fxy);
      if (d == null) return "**NOT FOUND!!";
      else return d.getName();
    }

    return "illegal F="+f;
  }
}
