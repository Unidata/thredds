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

  // create from Rectilyser
  public SparseArray( int... size) {
    this.size = size;
    calcStrides();

    track = new int[totalSize];
    this.content = new ArrayList<>(totalSize);  // LOOK could only allocate part of this.
  }

  // read back in from index
  public SparseArray( int[] size, int[] track, List<T> content) {
    this.size = size;
    calcStrides();

    this.track = track;
    this.content = content;

    if (track.length != totalSize)
      throw new IllegalStateException("track len "+track.length+" != totalSize "+totalSize);
  }

  private void calcStrides() {
    totalSize = 1;
    for (int aSize : size) totalSize *= aSize;

    // strides
    stride = new int[size.length];
    int product = 1;
    for (int ii = size.length - 1; ii >= 0; ii--) {
      int thisDim = size[ii];
      stride[ii] = product;
      product *= thisDim;
    }
  }

  public void add(T thing, Formatter info, int... index) {
    content.add(thing);
    int where = calcIndex(index);
    if (track[where] > 0) {
      ndups++;  // LOOK here is where we need to decide how to handle duplicates
      if (info != null) info.format(" duplicate %s%n     with %s%n%n", thing, content.get(track[where]-1));
    }
    track[where] = content.size();  // 1-based so that 0 = missing
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

  public int[] getShape() {
    return size;
  }

  public int getTotalSize() {
    return totalSize;
  }

  public void setSize(int[] size) {
    this.size = size;
  }

  public int[] getTrack() {
    return track;
  }

  public void setTrack(int[] track) {
    this.track = track;
  }

  public List<T> getContent() {
    return content;
  }

  public T getContent(int idx) {
    return content.get(idx);
  }

  public void setContent(List<T> content) {
    this.content = content;
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
