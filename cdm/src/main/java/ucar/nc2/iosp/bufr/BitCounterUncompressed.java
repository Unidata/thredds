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

import java.util.Map;
import java.util.HashMap;

/**
 * Counts the size of nested tables, for uncompressed messages.
 * A top-level BitCounterUncompressed counts bits for one row = obs = dataset.
 *   obs = new BitCounterUncompressed(root, 1, 0);
 * @author caron
 * @since May 10, 2008
 */
public class BitCounterUncompressed {
  private final DataDescriptor parent; // represents the table - fields/cols are the subKeys of dkey
  private final int nrows; // number of rows in this table
  private final int replicationCountSize; // number of bits taken up by the count variable (non-zero only for sequences)

  private Map<DataDescriptor, Integer> bitPosition;
  private Map<DataDescriptor, BitCounterUncompressed[]> subCounters; // nested tables; null for regular fields
  private int[] startBit; // from start of data section, for each row
  private int countBits; // total nbits in this table
  private int bitOffset = 0; // count bits

  static private boolean debug = false;

  /**
   * This counts the size of an array of Structures or Sequences, ie Structure(n)
   * @param parent is a structure or a sequence - so has subKeys
   * @param nrows numbers of rows in the table, equals 1 for top level
   * @param replicationCountSize number of bits taken up by the count variable (non-zero only for sequences)
   */
  public BitCounterUncompressed(DataDescriptor parent, int nrows, int replicationCountSize) {
    this.parent = parent;
    this.nrows = nrows;
    this.replicationCountSize = replicationCountSize;
  }

  // not used yet
  public void setBitOffset(DataDescriptor dkey) {
    if (bitPosition == null)
      bitPosition = new HashMap<DataDescriptor, Integer>(2 * parent.getSubKeys().size());
    bitPosition.put(dkey, bitOffset);
    bitOffset += dkey.getBitWidth();
  }
  public int getOffset(DataDescriptor dkey) {
    return bitPosition.get(dkey);
  }


  /**
   * Track nested Tables.
   * @param subKey  subKey is a structure or a sequence - so itself has subKeys
   * @param n numbers of rows in the nested table
   * @param row which row in the parent Table this belongs to
   * @param replicationCountSize number of bits taken up by the count (non-zero for sequences)
   * @return  nested ReplicationCounter
   */
  public BitCounterUncompressed makeNested(DataDescriptor subKey, int n, int row, int replicationCountSize) {
    if (subCounters == null)
      subCounters = new HashMap<DataDescriptor, BitCounterUncompressed[]>(5); // assumes DataDescriptor.equals is ==

    BitCounterUncompressed[] subCounter = subCounters.get(subKey);
    if (subCounter == null) {
      subCounter = new BitCounterUncompressed[nrows]; // one for each row in this table
      subCounters.put(subKey, subCounter);
    }

    BitCounterUncompressed rc = new BitCounterUncompressed(subKey, n, replicationCountSize);
    subCounter[row] = rc;

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

      for (DataDescriptor nd : parent.subKeys) {
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
