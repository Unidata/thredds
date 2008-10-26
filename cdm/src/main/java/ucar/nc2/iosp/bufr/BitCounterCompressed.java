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

}

