package ucar.nc2.grib.collection;

import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilderImpl;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Vertical GRIB coordinates
 *
 * @author caron
 * @since 11/27/13
 */
public class CoordinateVert implements Coordinate {
  private final List<VertCoord.Level> levelSorted;
  private final int code;

  public CoordinateVert(List<VertCoord.Level> levelSorted, int code) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    this.code = code;
  }

  static public VertCoord.Level extractLevel(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    boolean hasLevel2 = pds.getLevelType2() != GribNumbers.MISSING;
    double level2val =  hasLevel2 ?  pds.getLevelValue2() :  GribNumbers.UNDEFINEDD;
    boolean isLayer = Grib2Utils.isLayer(pds);

    return new VertCoord.Level(pds.getLevelValue1(), level2val, isLayer);
  }

  public Object extract(Grib2Record gr) {
    return extractLevel(gr);
  }


  public List<VertCoord.Level> getLevelSorted() {
    return levelSorted;
  }

  public List<? extends Object> getValues() {
    return levelSorted;
  }

  public int getSize() {
    return levelSorted.size();
  }

  public Type getType() {
    return Type.vert;
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
    info.format("%s%s levels: ", indent, getType());
     for (VertCoord.Level level : levelSorted)
       info.format(" %s", level);
    info.format(" (%d)%n", levelSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Levels:%n");
    for (VertCoord.Level level : levelSorted)
      info.format("   %s%n", level);
  }

  public boolean equals2(CoordinateVert other) {
    if (getCode() != other.getCode())
      return false;

    if (getSize() != other.getSize())
      return false;

    for (int i = 0; i < levelSorted.size(); i++) {
      if (!levelSorted.get(i).equals(other.levelSorted.get(i)))
        return false;
    }

    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateVert that = (CoordinateVert) o;

    if (code != that.code) return false;
    if (!levelSorted.equals(that.levelSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = levelSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////////////

  static public class Builder extends CoordinateBuilderImpl {
    int code;

    public Builder(int code) {
      this.code = code;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return extractLevel(gr);
    }

    @Override
   public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(levelSorted, code);
    }
  }

}
