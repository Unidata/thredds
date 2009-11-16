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

import java.util.Formatter;

/**
 * Count size of compressed fields
 *
 * @author caron
 * @since Jul 4, 2008
 */
public class BitCounterCompressed {

  private final DataDescriptor dkey; // the field to count
  private final int nrows;           // number of (obs) in the compression
  private final int bitOffset;       // starting position of the compressed data, reletive to start of data section
  private int dataWidth;             // bitWidth of incremental values
  private BitCounterCompressed[][] nested;     // used if the dkey is a structure = nested[innerRows][dkey.subkeys.size]

  /**
   * This counts the size of an array of Structures or Sequences, ie Structure(n)
   *
   * @param dkey                 is a structure or a sequence - so has subKeys
   * @param n                    numbers of rows in the table
   * @param bitOffset number of bits taken up by the count variable (non-zero only for sequences)
   */
  public BitCounterCompressed(DataDescriptor dkey, int n, int bitOffset) {
    this.dkey = dkey;
    this.nrows = n;
    this.bitOffset = bitOffset;
  }

  void setDataWidth( int dataWidth) {
    this.dataWidth = dataWidth;
  }

  public int getStartingBitPos() {
    return bitOffset;
  }

  public int getBitPos(int msgOffset) {
    return bitOffset + dkey.bitWidth + 6 + dataWidth * msgOffset;
  }

  public int getTotalBits() {
    if (nested == null)
      return dkey.bitWidth + 6 + dataWidth * nrows;
    else {
      int totalBits = 0;
      for (BitCounterCompressed[] counters : nested) {
        if (counters == null) continue;
        for (BitCounterCompressed counter : counters)
          if (counter != null) totalBits += counter.getTotalBits();
      }
      if (dkey.replicationCountSize > 0)
        totalBits += dkey.replicationCountSize + 6; // 6 boit count, 6 bit extra
      return totalBits;
    }
  }

  public BitCounterCompressed[] getNestedCounters(int innerIndex) {
    return nested[innerIndex];
  }

  public void addNestedCounters(int innerDimensionSize) {
    nested = new BitCounterCompressed[innerDimensionSize][dkey.getSubKeys().size()];
  }

  /**
   * Number of nested fields
   * @return 1 if no nested fields, otherwise count of nested fields
   */
  public int ncounters() {
    if (nested == null)
      return 1;
    else {
      int ncounters = 0;
      for (BitCounterCompressed[] counters : nested) {
        if (counters == null) continue;
        for (BitCounterCompressed counter : counters)
          if (counter != null) ncounters += counter.ncounters();
      }
      return ncounters;
    }
  }

  public void show(Formatter out, int indent) {
    for (int i=0; i<indent; i++) out.format(" ");
    out.format("%8d %8d %4d %s %n", getTotalBits(), bitOffset, dataWidth, dkey.name);
    if (nested != null) {
      for (BitCounterCompressed[] counters : nested) {
        if (counters == null) continue;
        for (BitCounterCompressed counter : counters)
          if (counter != null) counter.show(out, indent+2);
      }
    }
  }

}

