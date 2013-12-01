package ucar.nc2.grib.collection;

import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilder;
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
  //private final List<Coordinate> subdivide;

  public CoordinateVert(List<VertCoord.Level> levelSorted, List<Coordinate> subdivide) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    //this.subdivide = (subdivide == null) ? null :  Collections.unmodifiableList(subdivide);
  }

  static public VertCoord.Level extractLevel(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    boolean hasLevel2 = pds.getLevelType2() != GribNumbers.MISSING;
    double level2val =  hasLevel2 ?  pds.getLevelValue2() :  GribNumbers.UNDEFINEDD;
    boolean isLayer = Grib2Utils.isLayer(gr);

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

  @Override
  public String getName() {
    return "vert";
  }

  /* public List<Grib2Record> getRecordList(int timeIdx) {
    return recordList.get(timeIdx);
  } */

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s Levels: ", indent);
     for (VertCoord.Level level : levelSorted)
       info.format("%s, ", level);
    info.format("%n");
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Levels:%n");
    for (VertCoord.Level level : levelSorted)
      info.format("   %s%n", level);
  }

  static public class Builder extends CoordinateBuilderImpl {

    public Builder(Object runtime) {
      super(runtime);
    }

    @Override
    public CoordinateBuilder makeBuilder(Object val) {
      CoordinateBuilder result = new Builder(val);
      result.chainTo(nestedBuilder);
      return result;
    }

    @Override
    protected Object extract(Grib2Record gr) {
      return extractLevel(gr);
    }

    @Override
   protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide) {
      List<VertCoord.Level> levelSorted = new ArrayList<>(values.size());
      for (Object val : values) levelSorted.add( (VertCoord.Level) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(levelSorted, subdivide);
    }
  }

}
