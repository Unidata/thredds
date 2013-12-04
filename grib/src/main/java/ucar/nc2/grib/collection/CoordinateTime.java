package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilderImpl;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Time coordinates that are not intervals.
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateTime implements Coordinate {
  private final List<Integer> offsetSorted;
  private final int code;

  public CoordinateTime(List<Integer> offsetSorted, int code) {
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
    this.code = code;
  }

  static public Integer extractOffset(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    return pds.getForecastTime();
  }

  public Integer extract(Grib2Record gr) {
    return extractOffset(gr);
  }

  public List<Integer> getOffsetSorted() {
    return offsetSorted;
  }

  @Override
  public List<? extends Object> getValues() {
    return offsetSorted;
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  public Type getType() {
    return Type.time;
  }

  @Override
  public String getUnit() {
    return null;
  }

  public int getCode() {
    return code;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s offsets:", indent, getType());
     for (Integer cd : offsetSorted)
       info.format(" %3d,", cd);
    info.format(" (%d) %n", offsetSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time offsets:%n");
     for (Integer cd : offsetSorted)
       info.format("   %3d%n", cd);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTime that = (CoordinateTime) o;

    if (code != that.code) return false;
    if (!offsetSorted.equals(that.offsetSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = offsetSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  ////////////////////////////////////////////

  static public class Builder extends CoordinateBuilderImpl  {
    int code;

    public Builder(int code) {
      this.code = code;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return extractOffset(gr);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(offsetSorted, code);
    }
  }

}
