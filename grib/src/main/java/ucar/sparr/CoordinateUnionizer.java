package ucar.sparr;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Create the overall coordinate across the same variable in different partitions
 *
 * @author John
 * @since 12/10/13
 */
public class CoordinateUnionizer {
  FeatureCollectionConfig.GribIntvFilter intvFilter;
  int varId;

  public CoordinateUnionizer(int varId, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    this.intvFilter = intvFilter;
    this.varId = varId;
  }

  List<Coordinate> unionCoords = new ArrayList<>();
  CoordinateND<GribCollection.Record> result;

  CoordinateBuilder runtimeBuilder ;
  CoordinateBuilder timeBuilder;
  CoordinateBuilder timeIntvBuilder;
  CoordinateBuilder vertBuilder;
  Time2DUnionBuilder time2DBuilder;

  public void addCoords(List<Coordinate> coords) {
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          if (runtimeBuilder == null) runtimeBuilder = new CoordinateRuntime.Builder();
          runtimeBuilder.addAll(coord);
          break;
        case time:
          CoordinateTime time = (CoordinateTime) coord;
          if (timeBuilder == null) timeBuilder = new CoordinateTime.Builder(coord.getCode(), time.getTimeUnit());
          timeBuilder.addAll(coord);
          break;
        case timeIntv:
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) coord;
          if (timeIntvBuilder == null) timeIntvBuilder = new CoordinateTimeIntv.Builder(null, timeIntv.getTimeUnit(), coord.getCode());
          timeIntvBuilder.addAll(intervalFilter((CoordinateTimeIntv)coord));
          break;
        case vert:
          if (vertBuilder == null) vertBuilder = new CoordinateVert.Builder(coord.getCode());
          vertBuilder.addAll(coord);
          break;
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          if (time2DBuilder == null) time2DBuilder = new Time2DUnionBuilder(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode());
          time2DBuilder.addAll(time2D);
          break;
      }
    }
  }

  private List<TimeCoord.Tinv> intervalFilter(CoordinateTimeIntv coord) {
    if (intvFilter == null) return coord.getTimeIntervals();
    List<TimeCoord.Tinv> result = new ArrayList<>();
    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      if (intvFilter.filterOk(varId, tinv.getIntervalSize(), 0))
        result.add(tinv);
    }
    return result;
  }

      /* true means remove
  private boolean filterOut(Grib2Record gr, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    int[] intv = tables.getForecastTimeIntervalOffset(gr);
    if (intv == null) return false;
    int haveLength = intv[1] - intv[0];

    // HACK
    if (haveLength == 0 && intvFilter.isZeroExcluded()) {  // discard 0,0
      if ((intv[0] == 0) && (intv[1] == 0)) {
        //f.format(" FILTER INTV [0, 0] %s%n", gr);
        return true;
      }
      return false;

    } else if (intvFilter.hasFilter()) {
      int discipline = gr.getIs().getDiscipline();
      Grib2Pds pds = gr.getPDS();
      int category = pds.getParameterCategory();
      int number = pds.getParameterNumber();
      int id = (discipline << 16) + (category << 8) + number;

      int prob = Integer.MIN_VALUE;
      if (pds.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
        prob = (int) (1000 * pdsProb.getProbabilityUpperLimit());
      }
      return intvFilter.filterOut(id, haveLength, prob);
    }
    return false;
  }  */


  public List<Coordinate> finish() {
    if (runtimeBuilder != null)
      unionCoords.add(runtimeBuilder.finish());
    else
      System.out.println("HEY missing runtime");

    if (timeBuilder != null)
      unionCoords.add(timeBuilder.finish());
    else if (timeIntvBuilder != null)
      unionCoords.add(timeIntvBuilder.finish());
    else if (time2DBuilder != null)
      unionCoords.add(time2DBuilder.finish());
    else
      System.out.println("HEY missing time");

    if (vertBuilder != null)
      unionCoords.add(vertBuilder.finish());

    result = new CoordinateND<>(unionCoords);
    return unionCoords;
  }

  /*
   * Reindex with shared coordinates and return new CoordinateND
   * @param prev  previous
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   *
  public void addIndex(CoordinateND<GribCollection.Record> prev) {
    result.reindex(prev);
  }

  public CoordinateND<GribCollection.Record> getCoordinateND() {
    return result;
  } */

  private class Time2DUnionBuilder extends CoordinateBuilderImpl<Grib2Record> {
    boolean isTimeInterval;
    CalendarPeriod timeUnit;
    int code;

    CoordinateRuntime.Builder runBuilder;
    List<CalendarDate> runtimes = new ArrayList<>();
    List<Coordinate> times = new ArrayList<>();

    public Time2DUnionBuilder(boolean isTimeInterval, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder();
    }

    @Override
    public void addAll(Coordinate coord) {
      super.addAll(coord);
      CoordinateTime2D coordT2D = (CoordinateTime2D) coord;

      CoordinateRuntime runtime = coordT2D.getRuntimeCoordinate();
      runtimes.addAll(runtime.getRuntimesSorted());
      times.addAll( coordT2D.getTimes());
    }

    @Override
    public Object extract(Grib2Record gr) {
      throw new RuntimeException();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      CoordinateRuntime runCoord = new CoordinateRuntime(runtimes);

      List<CoordinateTime2D.Time2D> vals = new ArrayList<>(values.size());
      for (Object val : values) vals.add( (CoordinateTime2D.Time2D) val);
      Collections.sort(vals);

      return new CoordinateTime2D(code, timeUnit, vals, runCoord, times);
    }

  }

}
