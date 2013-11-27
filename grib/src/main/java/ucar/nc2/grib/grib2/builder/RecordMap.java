package ucar.nc2.grib.grib2.builder;

import ucar.arr.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/24/13
 */
public class RecordMap {

  int nruntime, ntime;
  int[] other;
  int totalSize;
  List<Grib2Rectilyser2.Record> records;
  SparseArray<Grib2Rectilyser2.Record> sparse;

  RecordMap(int nruntime, int ntime, int... other) {
    this.nruntime = nruntime;
    this.ntime = ntime;
    this.other = other;

    totalSize = nruntime * ntime;
    for (int n : other)
      totalSize *= n;

    records = new ArrayList<>(totalSize);
    sparse = new SparseArray<>();
  }

  double density() {
    return 0.0;
  }

  void add(Grib2Rectilyser2.Record r, Coordinate runCE, Coordinate timeCR, Coordinate... otherCE) {
    records.add(r);



    sparse.add(r, runCE, timeCR, otherCE);
  }

}
