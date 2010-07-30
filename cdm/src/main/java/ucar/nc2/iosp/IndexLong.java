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
