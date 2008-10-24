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

import java.util.Map;
import java.util.HashMap;

/**
 * Counts the size of nested tables, for non-compressed messages
 * @author caron
 * @since May 10, 2008
 */
public class BitCounterUncompressed {
  private final DataDescriptor dkey; // represents the table - fields/cols are the subKeys of dkey
  private final int nrows; // number of rows in this table
  private final int replicationCountSize; // number of bits taken up by the count variable (non-zero only for sequences)
  private Map<DataDescriptor, BitCounterUncompressed[]> subCounters; // null for regular fields
  private int[] startBit; // from start of data section, for each row
  private int countBits; // total nbits in this table

  static private boolean debug = false;

  /**
   * This counts the size of an array of Structures or Sequences, ie Structure(n)
   * @param dkey is a structure or a sequence - so has subKeys
   * @param nrows numbers of rows in the table, equals 1 for top level
   * @param replicationCountSize number of bits taken up by the count variable (non-zero only for sequences)
   */
  public BitCounterUncompressed(DataDescriptor dkey, int nrows, int replicationCountSize) {
    this.dkey = dkey;
    this.nrows = nrows;
    this.replicationCountSize = replicationCountSize;
  }

  /**
   * Track nested Tables.
   * @param subKey  subKey is a structure or a sequence - so itself has subKeys
   * @param n numbers of rows in the nested table
   * @param index which row in the parent Table this belongs to
   * @param replicationCountSize number of bits taken up by the count (non-zero for sequences)
   * @return  nested ReplicationCounter
   */
  public BitCounterUncompressed makeNested(DataDescriptor subKey, int n, int index, int replicationCountSize) {
    if (subCounters == null)
      subCounters = new HashMap<DataDescriptor, BitCounterUncompressed[]>(5);

    BitCounterUncompressed[] subCounter = subCounters.get(subKey);
    if (subCounter == null) {
      subCounter = new BitCounterUncompressed[nrows];
      subCounters.put(subKey, subCounter);
    }

    BitCounterUncompressed rc = new BitCounterUncompressed(subKey, n, replicationCountSize);
    subCounter[index] = rc;

    return rc;
  }

  public BitCounterUncompressed[] getNested(DataDescriptor subKey) {
    return (subCounters == null) ? null : subCounters.get(subKey);
  }

  // total bits of this table and all subtables
  int countBits(int startBit) {
    countBits = replicationCountSize;
    this.startBit = new int[nrows];

    for (int i=0; i<nrows; i++) {
      this.startBit[i] = startBit + countBits;
      if (debug) System.out.println(" BitCounterUncompressed row "+i+" startBit="+ this.startBit[i]);

      for (DataDescriptor nd : dkey.subKeys) {
        BitCounterUncompressed[] bitCounter = (subCounters == null) ? null : subCounters.get(nd);
        if (bitCounter == null) // a regular field
          countBits += nd.getBitWidth();
        else {
          if (debug) System.out.println(" ---------> nested "+nd.getFxyName()+" starts at ="+ (startBit + countBits));
          countBits += bitCounter[i].countBits(startBit + countBits);
          if (debug) System.out.println(" <--------- nested "+nd.getFxyName()+" ends at ="+ (startBit + countBits));
        }
      }
    }
    return countBits;
  }

  public int getCountBits() { return countBits; }
  public int getNumberRows() { return nrows; }

  public int getStartBit(int row) {
    if (row >= startBit.length)
      throw new IllegalStateException();
    return startBit[row];
  }

}
