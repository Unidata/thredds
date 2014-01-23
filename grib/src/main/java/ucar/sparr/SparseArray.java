package ucar.sparr;

import java.util.*;

/**
 * Store objects of type T in a sparse array.
 *
 * @author caron
 * @since 11/24/13
 */
public class SparseArray<T> {
  private int[] size;    // multidim sizes
  private int[] stride;  // for index calculation
  private int totalSize; // product of sizes

  private int[] track; // index into content, size totalSize. LOOK use byte, short for memory ??
  private List<T> content; // keep the things in an ArrayList.

  private int ndups = 0; // number of duplicates

  // create from Rectilyser or reindexer
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
    content.add(thing);            // add the thing at end of list, idx = size-1
    int where = calcIndex(index);
    if (track[where] > 0) {
      ndups++;  // LOOK here is where we need to decide how to handle duplicates
      if (info != null) info.format(" duplicate %s%n     with %s%n%n", thing, content.get(track[where]-1));
    }
    track[where] = content.size();  // 1-based so that 0 = missing, so content at where = content.get(track[where]-1)
  }

  public int calcIndex(int... index) {
    assert index.length == size.length;
    int result = 0;
    for (int ii = 0; ii < index.length; ii++)
      result += index[ii] * stride[ii];
    return result;
  }

  public T getContent(int idx) {
    if (idx > track.length)
      System.out.println("HEY");
    int contentIdx = track[idx]-1;
    if (contentIdx < 0) return null; // missing
    return content.get(contentIdx);
  }

  public T getContent(int[] index) {
    int where = calcIndex(index);
    return getContent(where);
  }

  public int[] getShape() {
    return size;
  }

  public int getRank() {
    return size.length;
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

  public int getTrack(int idx) {
    return track[idx];
  }

  public void setTrack(int[] track) {
    this.track = track;
  }

  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

  public int getNduplicates() {
    return ndups;
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

  public float getDensity() {
    return (float) countNotMissing() / totalSize;
  }

  public void showInfo(Formatter info, Counter all) {
    info.format("ndups=%d total=%d/%d density= %f%n", ndups, countNotMissing(), totalSize, getDensity());

    if (all != null) {
      all.dups += ndups;
      all.recordsUnique += countNotMissing();
      all.recordsTotal += totalSize;
      all.vars++;
    }

    info.format("sizes=");
    List<Integer> sizes = new ArrayList<>();
    for (int s : size) {
      info.format("%d,", s);
      if (s == 1) continue; // skip dimension len 1
      sizes.add(s);
    }
    info.format("%n%n");
    showRecurse(0, sizes, info);
  }

  int showRecurse(int offset, List<Integer> sizes, Formatter f) {
    if (sizes.size() == 0) return 0;
    if (sizes.size() == 1) {
      int len = sizes.get(0);
      for (int i=0; i<len; i++) {
        boolean hasRecord = track[offset+i] > 0;
        if (hasRecord) f.format("X"); else f.format("-");
      }
      f.format("%n");
      return len;

    } else {
      int total = 0;
      int len = sizes.get(0);
      for (int i=0; i<len; i++) {
        int count = showRecurse(offset, sizes.subList(1,sizes.size()), f);
        offset += count;
        total += count;
      }
      f.format("%n");
      return total;
    }

  }


}
