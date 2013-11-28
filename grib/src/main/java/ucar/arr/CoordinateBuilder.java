package ucar.arr;

import ucar.nc2.grib.grib2.Grib2Record;

import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 11/27/13
 */
public interface CoordinateBuilder {

  public CoordinateBuilder chainTo(CoordinateBuilder builder);

  public CoordinateBuilder makeBuilder(Object val);

  public void add(Grib2Record r);

  public Coordinate finish();

  public List<Grib2Record> getRecords();


  // call only after finish();
  public Coordinate getCoordinate();

  public boolean hasChildBuilders();

  public CoordinateBuilder getChildBuilder(Object key);
}
