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
    Formatter out = new Formatter();
    out.format("%d-%02d-%03d",f,x,y);
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

  static public short getFxy(short f, short x, short y) {
    return (short) ((f << 14) + (x << 8) + (y));
  }

  static private final String[] descType = {"tableB", "replication", "tableC-operators", "tableD"};

  static public void show(Formatter out, short fxy, TableLookup lookup) {
    int f = (fxy & 0xC000) >> 14;

    if (f == 0) {
      TableB.Descriptor b = lookup.getDescriptorTableB( fxy);
      if (b == null) out.format("**NOT FOUND!!");
      else out.format("%-8s: %s", b.getFxy(), b.getName());

    } else if (f == 1)
      out.format("%-8s: %s", makeString(fxy), descType[1]);

    else if (f == 2) {
      int x = (fxy & 0x3F00) >> 8;
      out.format("%-8s: Operator= %s", makeString(fxy), TableC.getOperatorName(x));

    } else if (f == 3) {
      TableD.Descriptor d = lookup.getDescriptorTableD( fxy);
      if (d == null) out.format("**NOT FOUND!!");
      else out.format("%-8s: %s", d.getFxy(), d.getName());
    }
  }

  static public String getName(short fxy, TableLookup lookup) {
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
