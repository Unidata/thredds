package ucar.nc2.grib.collection;

import ucar.arr.Coordinate;
import ucar.arr.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public class RecordMap implements Iterable<Grib2Rectilyser.Record> {

  int nruntime, ntime;
  int[] other;
  int totalSize;
  List<Grib2Rectilyser.Record> records;
  SparseArray<Grib2Rectilyser.Record> sparse;

  RecordMap(int nruntime, int ntime, int... other) {
    this.nruntime = nruntime;
    this.ntime = ntime;
    this.other = other;

    totalSize = nruntime * ntime;
    for (int n : other)
      totalSize *= n;

    records = new ArrayList<>(totalSize);
    // sparse = new SparseArray<>();
  }

  double density() {
    return 0.0;
  }

  void add(Grib2Rectilyser.Record r, Coordinate runCE, Coordinate timeCR, Coordinate... otherCE) {
    records.add(r);



    //sparse.add(r, runCE, timeCR, otherCE);
  }

  @Override
  public Iterator<Grib2Rectilyser.Record> iterator() {  // LOOK not sorted
    return records.iterator();
  }
}
