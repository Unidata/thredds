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

import ucar.nc2.iosp.bufr.tables.*;

import java.util.*;
import java.io.IOException;

/**
 * Encapsolates lookup into the BUFR Tables.
 *
 * @author caron
 * @since Jul 14, 2008
 */
public final class TableLookup {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableLookup.class);

  static private final boolean showErrors = true;

  /////////////////////////////////////////
  private TableB localTableB;
  private TableD localTableD;

  private TableB wmoTableB;
  private TableD wmoTableD;
  public BufrTables.Mode mode = BufrTables.Mode.wmoOnly;

  public TableLookup(BufrIdentificationSection ids) throws IOException {
    this.wmoTableB = BufrTables.getWmoTableB(ids);
    this.wmoTableD = BufrTables.getWmoTableD(ids);

    BufrTables.Tables tables = BufrTables.getLocalTables(ids);
    if (tables != null) {
      this.localTableB = tables.b;
      this.localTableD = tables.d;
      this.mode = (tables.mode == null) ? BufrTables.Mode.wmoOnly : tables.mode;
    }
  }

  public final String getWmoTableBName() {
    return wmoTableB.getName();
  }

 public final String getLocalTableBName() {
    return localTableB == null ? "none" : localTableB.getName();
  }

  public final String getLocalTableDName() {
    return localTableD == null ? "none" : localTableD.getName();
  }

  public final String getWmoTableDName() {
    return wmoTableD.getName();
  }

  public BufrTables.Mode getMode() {
    return mode;
  }

   public TableB.Descriptor getDescriptorTableB(short fxy) {
    TableB.Descriptor b = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && (mode == BufrTables.Mode.wmoOnly)) {
      b = wmoTableB.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.wmoLocal)) {
      b = wmoTableB.getDescriptor(fxy);
      if ((b == null) && (localTableB != null))
        b = localTableB.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.localOverride)) {
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
      if (b == null)
        b = wmoTableB.getDescriptor(fxy);
      else
        b.setLocalOverride(true);

    } else if (!isWmoRange) {
      if (localTableB != null)
        b = localTableB.getDescriptor(fxy);
    }

    if (b == null && showErrors)
      System.out.printf(" TableLookup cant find Table B descriptor = %s in tables %s, %s mode=%s%n", Descriptor.makeString(fxy),
          getLocalTableBName(), getWmoTableBName(), mode);
    return b;
  }

  public List<Short> getDescriptorsTableD(short id) {
    TableD.Descriptor d = getDescriptorTableD(id);
    if (d != null)
      return d.getSequence();
    return null;
  }

  public List<String> getDescriptorsTableD(String fxy) {
    short id = Descriptor.getFxy(fxy);
    List<Short> seq = getDescriptorsTableD(id);
    if (seq == null) return null;
    List<String> result = new ArrayList<String>( seq.size());
    for (Short s : seq)
      result.add( Descriptor.makeString(s));
    return result;
  }
  
  public TableD.Descriptor getDescriptorTableD(short fxy) {
    TableD.Descriptor d = null;
    boolean isWmoRange = Descriptor.isWmoRange(fxy);

    if (isWmoRange && (mode == BufrTables.Mode.wmoOnly)) {
      d = wmoTableD.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.wmoLocal)) {
      d = wmoTableD.getDescriptor(fxy);
      if ((d == null) && (localTableD != null))
        d = localTableD.getDescriptor(fxy);

    } else if (isWmoRange && (mode == BufrTables.Mode.localOverride)) {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
      if (d == null)
        d = wmoTableD.getDescriptor(fxy);
      else
        d.setLocalOverride(true);

    } else {
      if (localTableD != null)
        d = localTableD.getDescriptor(fxy);
    }

    if (d == null && showErrors)
      System.out.printf(" TableLookup cant find Table D descriptor %s in tables %s,%s mode=%s%n", Descriptor.makeString(fxy),
          getLocalTableDName(), getWmoTableDName(), mode);
    return d;
  }

}
