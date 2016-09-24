/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.cache;

import ucar.nc2.util.Misc;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Formatter;

/**
 * integer array in which we want to quickly find the index for a given value.
 * optimize for the case of 1) constant, 2) sequential, 3) sorted
 *
 * @author caron
 * @since 11/4/2014
 */
@Immutable
public class SmartArrayInt {
  private final int[] raw;
  private final int start;
  private final boolean isSequential;   // if elem[i] = constant + i; LOOK could generalize to strided
  private final boolean isConstant;   // if elem[i] = constant
  private final boolean isSorted;    // if elements are sorted, can use a binary search
  private final int n;

  public SmartArrayInt(int[] raw) {
    this.n = raw.length;
    if (raw.length == 0) {
      this.start = -1;
      this.isConstant = true;
      this.isSequential = false;
      this.isSorted = false;
      this.raw = null;
      return;
    }
    boolean isC = true;
    boolean isSeq = true;
    boolean isSort = true;
    this.start = raw[0];
    for (int i=0; i<raw.length; i++) {
      if (raw[i] != start+i) isSeq = false;
      if (raw[i] != start) isC = false;
      if (i>0 && raw[i] < raw[i-1]) isSort = false;
    }

    this.raw = (!isSeq && !isC) ? raw : null;
    this.isSequential = isSeq;
    this.isConstant = isC;
    this.isSorted = isSort;
  }

  public int get(int idx) {
    if (isConstant) return start;
    if (isSequential) return start+idx;
    return raw[idx];
  }

  public int getN() {
    return n;
  }

  public void show(Formatter f) {
    if (isConstant) f.format("isConstant=%d",start);
    else if (isSequential) f.format("isSequential start=%d",start);
    else {
      f.format("isSorted=%s ", isSorted);
      Misc.showInts(raw, f);
    }
  }

  /**
   * Find which index holds the value want
   * @param want  value wanted
   * @return < 0 if not found, else the index. If duplicates, then return any match
   */
  public int findIdx(int want) {
    if (isConstant) return (want == start)  ? 0 : -1;
    if (isSequential) return want - start;
    if (isSorted) {
      return Arrays.binarySearch(raw, want);
    }
    // linear search
    for (int i=0; i<raw.length; i++)
      if (raw[i] == want) return i;
    return -1;
  }



}
