package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Vertical GRIB coordinates
 * Effectively immutable; setName() can only be called once.
 *
 * @author caron
 * @since 11/27/13
 */
@Immutable
public class CoordinateVert implements Coordinate {

  private final List<VertCoord.Level> levelSorted;
  private final int code; // Grib1 - code table 3; Grib2 - Code table 4.5
  private String name;
  private final VertCoord.VertUnit vunit;
  private final boolean isLayer;

  public CoordinateVert(int code, VertCoord.VertUnit vunit, List<VertCoord.Level> levelSorted) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    this.code = code;
    this.vunit = vunit;
    this.isLayer = levelSorted.get(0).isLayer();
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
    if (idx >= levelSorted.size())
      return null;
    return levelSorted.get(idx);
  }

  @Override
  public int getSize() {
    return levelSorted.size();
  }

  @Override
  public Type getType() {
    return Type.vert;
  }

  @Override
  public int estMemorySize() {
    return 160 + getSize() * ( 40 + Misc.referenceSize);
  }

  @Override
  public String getUnit() {
    return vunit == null ? null : vunit.getUnits();
  }

  public VertCoord.VertUnit getVertUnit() {
    return vunit;
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
    if (this.name != null) throw new IllegalStateException("Cant modify");
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

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    int code;
    VertCoord.VertUnit vunit;

    public Builder2(int code, VertCoord.VertUnit vunit) {
      this.code = code;
      this.vunit = vunit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      boolean hasLevel2 = pds.getLevelType2() != GribNumbers.MISSING;
      double level2val =  hasLevel2 ?  pds.getLevelValue2() :  GribNumbers.UNDEFINEDD;
      boolean isLayer = Grib2Utils.isLayer(pds);
      return new VertCoord.Level(pds.getLevelValue1(), level2val, isLayer);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, vunit, levelSorted);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    int code;
    Grib1Customizer cust;

    public Builder1(Grib1Customizer cust, int code) {
      this.cust = cust;
      this.code = code;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      boolean isLayer = cust.isLayer(pds.getLevelType());
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      double level2val =  isLayer ?  plevel.getValue2() :  GribNumbers.UNDEFINEDD;
      return new VertCoord.Level(plevel.getValue1(), level2val, isLayer);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, cust.getVertUnit(code), levelSorted);
    }
  }

}
