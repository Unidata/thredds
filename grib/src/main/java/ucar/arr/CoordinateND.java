package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * N dimensional coordinates.
 *
 * @author caron
 * @since 11/27/13
 */
public class CoordinateND {

  List<CoordinateBuilder> builders;
  List<Coordinate> coordinates; // result is orthogonal coordinates
  SparseArray<Grib2Record> sa;  // indexes refer to coordinates

  public CoordinateND() {
    builders = new ArrayList<>();
  }

  public void addBuilder(CoordinateBuilder builder) {
    builders.add(builder);
  }

  public void addRecord( Grib2Record gr) {
    for (CoordinateBuilder builder : builders)
      builder.addRecord(gr);
  }

  public void finish(List<Grib2Record> records, Formatter info) {
    coordinates = new ArrayList<>();
    for (CoordinateBuilder builder : builders)
      coordinates.add(builder.finish());

    buildSparseArray(records, info);
  }

  public void buildSparseArray(List<Grib2Record> records, Formatter info) {
    int[] sizeArray = new int[coordinates.size()];
    for (int i = 0; i < coordinates.size(); i++)
      sizeArray[i] = coordinates.get(i).getSize();
    sa = new SparseArray<>(sizeArray);

    int[] index = new int[coordinates.size()];
    for (Grib2Record gr : records) {
      int count = 0;
      for (CoordinateBuilder builder : builders)
        index[count++] = builder.getIndex(gr);

      sa.add(gr, info, index);
    }
  }


  /////////////////////////////////////////////////////////////////////////
  // apres finis

  public List<Coordinate> getCoordinates() {
    return coordinates;
  }

  public int getNCoordinates() {
    return coordinates.size();
  }

  public SparseArray<Grib2Record> getSparseArray() {
    return sa;
  }

  public void showInfo(List<Grib2Record> records, Formatter info) {
    buildSparseArray(records, info);
    info.format("%n%n");
    sa.showInfo(info, null);
    info.format("%n");
    for (Coordinate coord : coordinates)
      coord.showInfo(info, new Indent(2));
  }

  public void showInfo(Formatter info, Counter all) {
    for (Coordinate coord : coordinates)
       coord.showInfo(info, new Indent(2));

    if (sa != null) sa.showInfo(info, all);
  }

}
