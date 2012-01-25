/*
 * Copyright 1998-2012 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.iosp;

/**
 * Uses longs for indexing, otherwise similar to ucar.ma2.Index
 *
 * @author caron
 * @since Jul 30, 2010
 */
public class IndexLong {
  private int[] shape;
  private long[] stride;
  private int rank;

  private int offset; // element = offset + stride[0]*current[0] + ...
  private int[] current; // current element's index

  // shape = int[] {1}
  public IndexLong() {
    shape = new int[] { 1};
    stride = new long[] { 1};

    rank = shape.length;
    current = new int[rank];

    stride[0] = 1;
    offset = 0;
  }

  public IndexLong(int[] _shape, long[] _stride) {
    this.shape = new int[_shape.length];  // optimization over clone
    System.arraycopy(_shape, 0, this.shape, 0, _shape.length);

    this.stride = new long[_stride.length];  // optimization over clone
    System.arraycopy(_stride, 0, this.stride, 0, _stride.length);

    rank = shape.length;
    current = new int[rank];
    offset = 0;
  }

  static public long computeSize(int[] shape) {
    long product = 1;
    for (int ii = shape.length - 1; ii >= 0; ii--)
      product *= shape[ii];
    return product;
  }

  public long incr() {
    int digit = rank - 1;
    while (digit >= 0) {
      current[digit]++;
      if (current[digit] < shape[digit])
        break;
      current[digit] = 0;
      digit--;
    }
    return currentElement();
  }

  public long currentElement() {
    long value = offset;
    for (int ii = 0; ii < rank; ii++)
      value += current[ii] * stride[ii];
    return value;
  }
}
