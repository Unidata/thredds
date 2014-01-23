package ucar.nc2.grib.collection;

import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilder;
import ucar.sparr.CoordinateBuilderImpl;
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

  static public VertCoord.Level extractLevel(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    boolean hasLevel2 = pds.getLevelType2() != GribNumbers.MISSING;
    double level2val =  hasLevel2 ?  pds.getLevelValue2() :  GribNumbers.UNDEFINEDD;
    boolean isLayer = Grib2Utils.isLayer(pds);
    return new VertCoord.Level(pds.getLevelValue1(), level2val, isLayer);
  }

  private final List<VertCoord.Level> levelSorted;
  private final int code;
  private String name;
  private final VertCoord.VertUnit vunit;
  private final boolean isLayer;

  public CoordinateVert(int code, List<VertCoord.Level> levelSorted) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    this.code = code;
    this.vunit = Grib2Utils.getLevelUnit(code);
    this.isLayer = levelSorted.get(0).isLayer();
  }

  public Object extract(Grib2Record gr) {
    return extractLevel(gr);
  }

  public List<VertCoord.Level> getLevelSorted() {
    return levelSorted;
  }

  @Override
  public List<? extends Object> getValues() {
    return levelSorted;
  }

  @Override
  public int getIndex(Object val) {
    return levelSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    return levelSorted.get(idx);
  }

  public int getSize() {
    return levelSorted.size();
  }

  public Type getType() {
    return Type.vert;
  }

  @Override
  public String getUnit() {
    return vunit.getUnits();
  }

  public boolean isLayer() {
    return isLayer;
  }

  public boolean isPositiveUp() {
    return vunit.isPositiveUp();
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s: ", indent, getType());
     for (VertCoord.Level level : levelSorted)
       info.format(" %s", level);
    info.format(" (%d)%n", levelSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Levels: (%s)%n", getUnit());
    for (VertCoord.Level level : levelSorted)
      info.format("   %s%n", level);
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

  /* public CoordinateBuilder makeBuilder() {
    return new Builder(code);
  } */

  static public class Builder extends CoordinateBuilderImpl<Grib2Record> {
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
      return new CoordinateVert(code, levelSorted);
    }
  }

}
