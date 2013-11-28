package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.builder.CoordinateTime;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Describe
 *
 * @author caron
 * @since 11/27/13
 */
public class CoordinateND {

  CoordinateBuilder root;
  List<Coordinate> coordinates;
  SparseArray<Grib2Record> sa;

  public CoordinateND(CoordinateBuilder root) {
    this.root = root;
  }

  public void add(Grib2Record gr) {
    root.add(gr);
  }

  public void finish() {
    root.finish();
    buildOrthogonalCoordinates();
    buildSparseArray();

    /* sa = new SparseArray<>(nruns, maxTimes);
    int runIdx = 0;
    for (CoordinateTime time : timesSorted) { // one for each runtime
      for (int timeIdx = 0; timeIdx < time.getSize(); timeIdx++) {
        for (Grib2Record r : time.getRecordList(timeIdx))
          sa.add(r, runIdx, timeIdx);
      }
      runIdx++;
    } */

  }

  private void buildOrthogonalCoordinates() {
    coordinates = new ArrayList<>();
    coordinates.add(root.getCoordinate());
    int wantLevel = 1;
    while (true) {
      Coordinate result = mergeNestedCoordinates(root, wantLevel, 1);
      if (result == null) break;
      coordinates.add(result);
      wantLevel++;
    }
  }

  private Coordinate mergeNestedCoordinates(CoordinateBuilder builder, int wantLevel, int isLevel) {

    if (builder.getNestedBuilder() == null)
      return null;

    Set<Object> allKeys = new HashSet<>();
    Coordinate coord = builder.getCoordinate();
    for (Object key : coord.getValues()) {
      CoordinateBuilder nestedBuilder = builder.getChildBuilder(key);

      if (wantLevel == isLevel) {
        Coordinate nestedCoord = nestedBuilder.getCoordinate();
        for (Object nestedVal : nestedCoord.getValues())
          allKeys.add(nestedVal);

      } else {
        Coordinate nestedCoord2 = mergeNestedCoordinates(nestedBuilder, wantLevel, isLevel + 1);
        if (nestedCoord2 == null) return null;
        allKeys.addAll(nestedCoord2.getValues());
      }
    }
    if (allKeys.size() == 0) return null;
    return builder.getNestedBuilder().makeCoordinate(allKeys);
  }

  void buildSparseArray() {
    int[] sizeArray = new int[coordinates.size()];
    for (int i = 0; i < coordinates.size(); i++)
      sizeArray[i] = coordinates.get(i).getSize();
    sa = new SparseArray<>(sizeArray);

    List<Integer> indexList = new ArrayList<>();
    makeArray(root, indexList, 0);
  }

  void makeArray(CoordinateBuilder builder, List<Integer> indexList, int level) {

    Coordinate coord = builder.getCoordinate();
    int index = 0;
    indexList.add(level);
    for (Object key : coord.getValues()) {
      indexList.set(level, index);

      if (builder.getNestedBuilder() != null) {
        CoordinateBuilder nestedBuilder = builder.getChildBuilder(key);
        makeArray(nestedBuilder, indexList, level + 1);

      } else {
        int[] indexArray = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) indexArray[i] = indexList.get(i);

        for (Grib2Record r : builder.getRecords(key))  {
          sa.add(r, indexArray);
        }
      }
      index++;
    }
  }


  /////////////////////////////////////////////////////////////////////////

  public void showInfo(Formatter info) {
    showInfo(coordinates, info, new Indent(2));
    if (sa != null) sa.showInfo(info);
  }

  // recurse
  private void showInfo(List<Coordinate> coords, Formatter info, Indent indent) {
    Coordinate coord = coords.get(0);
    coord.showInfo(info, indent);

    if (coords.size() > 1) {
      showInfo(coords.subList(1, coords.size()), info, indent.incr());
      indent.decr();
    }
  }


}
