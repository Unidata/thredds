package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Store objects of type T in a sparse array.
 *
 * @author caron
 * @since 11/24/13
 */
public class SparseArray<T> {
  int[] size;    // multidim sizes
  int[] stride;  // optimize index calculation
  int totalSize; // product of sizes

  int[] track; // index into content  size totalSize. LOOK use byte, short for memory ??
  List<T> content; // keep the things in an ArrayList. LOOK could only allocated part of this.

  int ndups = 0; // number of duplicates - last one is kept (could make this changeable)

  public SparseArray( int... size) {
    this.size = size;
    totalSize = 1;
    for (int aSize : size) totalSize *= aSize;
    this.content = new ArrayList<>(totalSize);

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
    if (track[where] > 0) ndups++;
    track[where] = content.size(); // 1-based so that 0 = missing
  }

  public T fetch(int[] index) {
    int where = calcIndex(index);
    int idx = track[where-1];
    return content.get(idx);
  }

  public int countNotMissing() {
    int result=0;
    for (int idx : track)
      if (idx > 0) result++;
    return result;
  }

  public int calcIndex(int... index) {
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

  public void showInfo(Formatter info) {
    info.format(" ndups=%d total=%d/%d density= %f%n", ndups, countNotMissing(), totalSize, getDensity());
  }

  public static class Builder {
    SparseArray<Grib2Record> sa;

    Builder(CoordinateBuilder root) {
      List<Integer> sizeList = new ArrayList<>();
      getSizes(sizeList, root);

      int[] sizeArray = new int[sizeList.size()];
      for (int i=0; i<sizeList.size(); i++) sizeArray[i] = sizeList.get(i);
      sa = new SparseArray(sizeArray);

    }


    void getSizes(List<Integer> sizeList, CoordinateBuilder coordBuilder) {
      Coordinate coord = coordBuilder.getCoordinate();
      sizeList.add(coord.getSize());

      if (coordBuilder.hasChildBuilders()) {
        int max = 0;
        for (Object key : coord.getValues()) {
          CoordinateBuilder next = coordBuilder.getChildBuilder(key);
          Coordinate coordNested = next.getCoordinate();
          max = Math.max(max, coordNested.getSize());
        }
        sizeList.add(max);
      }
    }

    void makeArray() {
      int[] sizeArray = new int[sizeList.size()];
      for (int i=0; i<sizeList.size(); i++) sizeArray[i] = sizeList.get(i);
      SparseArray<Grib2Record> sa = new SparseArray(sizeArray);
    }

    void makeArray(SparseArray<Grib2Record> sa, CoordinateBuilder coordBuilder, List<Integer> indexList) {
       if (coordBuilder.getRecords() == null) return;

      int[] index = new int[indexList.size()];
      for (int i=0; i<indexList.size(); i++) index[i] = indexList.get(i);

       for (Grib2Record r : coordBuilder.getRecords())
         sa.add(r, index);
     }

   }
}
