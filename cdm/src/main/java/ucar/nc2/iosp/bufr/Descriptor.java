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
