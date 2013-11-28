package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.List;
import java.util.Set;

/**
 * Build Coordinates heirarchically, keeping records at the leaves
 * Finds distinct values
 *
 * @author caron
 * @since 11/27/13
 */
public interface CoordinateBuilder {

  // make a Builder that will be associated with the val
  public CoordinateBuilder makeBuilder(Object val);

  public void add(Grib2Record r);

  public Coordinate finish();

  public Coordinate getCoordinate(); // call only after finish

  //////////////////////////////////////

   // add a nested CoordinateBuilder, return it
  public CoordinateBuilder chainTo(CoordinateBuilder builderBuilder);

  // if a nested CoordinateBuilder exists
  public CoordinateBuilder getNestedBuilder();

  // get nested CoordinateBuilder associated with the key
  public CoordinateBuilder getChildBuilder(Object key);

  // make a Coordinate
  public Coordinate makeCoordinate(Set<Object> allCoords);


  ////////////////////////////////////////////

  public List<Grib2Record> getRecords(Object key);  // only at the leaves

}
