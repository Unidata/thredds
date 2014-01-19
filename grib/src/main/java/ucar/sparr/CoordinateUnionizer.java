package ucar.sparr;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.collection.*;

import java.util.*;

/**
 * Create shared coordinates across the same variable in different partitions
 * Create the union of all coordinates.
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

  public void addCoords(List<Coordinate> coords) {
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          if (runtimeBuilder == null) runtimeBuilder = new CoordinateRuntime.Builder();
          runtimeBuilder.addAll(coord);
          break;
        case time:
          if (timeBuilder == null) timeBuilder = new CoordinateTime.Builder(coord.getCode());
          timeBuilder.addAll(coord);
          break;
        case timeIntv:
          if (timeIntvBuilder == null) timeIntvBuilder = new CoordinateTimeIntv.Builder(null, null, coord.getCode());
          timeIntvBuilder.addAll(intervalFilter((CoordinateTimeIntv)coord));
          break;
        case vert:
          if (vertBuilder == null) vertBuilder = new CoordinateVert.Builder(coord.getCode());
          vertBuilder.addAll(coord);
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
    else
      System.out.println("HEY missing time");

    if (vertBuilder != null)
      unionCoords.add(vertBuilder.finish());

    result = new CoordinateND<>(unionCoords);
    return unionCoords;
  }

  /**
   * Reindex with shared coordinates and return new CoordinateND
   * @param prev  previous
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   */
  public void addIndex(CoordinateND<GribCollection.Record> prev) {
    result.reindex(prev);
  }

  public CoordinateND<GribCollection.Record> getCoordinateND() {
    return result;
  }

}
