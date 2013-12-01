package ucar.arr;

import java.util.*;

/**
 * Store objects of type T in a sparse array.
 *
 * @author caron
 * @since 11/24/13
 */
public class SparseArray<T> {
  int[] size;    // multidim sizes
  int[] stride;  // for index calculation
  int totalSize; // product of sizes

  int[] track; // index into content, size totalSize. LOOK use byte, short for memory ??
  List<T> content; // keep the things in an ArrayList.

  int ndups = 0; // number of duplicates

  public SparseArray( int... size) {
    this.size = size;
    totalSize = 1;
    for (int aSize : size) totalSize *= aSize;
    this.content = new ArrayList<>(totalSize);  // LOOK could only allocate part of this.

    // strides
    stride = new int[size.length];
    int product = 1;
    for (int ii = size.length - 1; ii >= 0; ii--) {
      int thisDim = size[ii];
      stride[ii] = product;
      product *= thisDim;
    }
    track = new int[totalSize];
  }

  public void add(T thing, int... index) {
    content.add(thing);
    int where = calcIndex(index);
    if (track[where] > 0) ndups++;  // LOOK here is where we need to decide how to handle duplicates
    track[where] = content.size(); // 1-based so that 0 = missing
  }

  public T fetch(int[] index) {
    int where = calcIndex(index);
    if (track[where] == 0) return null; // missing
    int idx = track[where-1];
    return content.get(idx);
  }

  public int countNotMissing() {
    int result=0;
    for (int idx : track)
      if (idx > 0) result++;
    return result;
  }

  public int countMissing() {
    int result=0;
    for (int idx : track)
      if (idx == 0) result++;
    return result;
  }

  public int calcIndex(int... index) {
    if (index.length != size.length)
      System.out.println("HEY");
    assert index.length == size.length;
    int result = 0;
    for (int ii = 0; ii < index.length; ii++)
      result += index[ii] * stride[ii];
    return result;
  }

  public int getNduplicates() {
    return ndups;
  }

  public double getDensity() {
    return (double) countNotMissing() / totalSize;
  }

  public void showInfo(Formatter info, Counter all) {
    info.format(" ndups=%d total=%d/%d density= %f%n", ndups, countNotMissing(), totalSize, getDensity());

    if (all != null) {
      all.dups += ndups;
      all.recordsUnique += countNotMissing();
      all.recordsTotal += totalSize;
      all.vars++;
    }
  }

}
