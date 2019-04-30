/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.collection.GribCollectionMutable;
import ucar.nc2.grib.collection.PartitionCollectionMutable;
import ucar.nc2.time.CalendarDate;

import java.util.*;

/**
 * Create the overall coordinate across the same variable in different partitions
 * The CoordinateBuilders create unique sets of Coordinates.
 * The CoordinateND result is then the cross-product of those Coordinates.
 *
 * Used by GribPartitionBuilder
 * This is a builder helper class, the result is obtained from List<Coordinate> finish().
 *
 * So if theres a lot of missing records in that cross-product, we may have the variable wrong (?),
 *  or our assumption that the data comprises a multidim array may be wrong
 *
 * @author John
 * @since 12/10/13
 */
public class CoordinatePartitionUnionizer {
  private static final boolean debugPartitionErrors = true;

  private final FeatureCollectionConfig.GribIntvFilter intvFilter;
  private final org.slf4j.Logger logger;
  private final GribCollectionMutable.VariableIndex vi;
  private final Map<Long, PartitionCollectionMutable.Partition> timeMap;

  public CoordinatePartitionUnionizer(GribCollectionMutable.VariableIndex vi, FeatureCollectionConfig.GribIntvFilter intvFilter, org.slf4j.Logger logger) {
    this.vi = vi;
    this.intvFilter = intvFilter;
    this.logger = logger;
    timeMap = (debugPartitionErrors) ? new HashMap<>() : null;
  }

  List<Coordinate> unionCoords = new ArrayList<>();

  CoordinateBuilder runtimeBuilder ;
  CoordinateBuilder timeBuilder;
  CoordinateBuilder timeIntvBuilder;
  CoordinateBuilder vertBuilder;
  CoordinateBuilder ensBuilder;
  CoordinateTime2DUnionizer time2DBuilder;

  boolean duplicateRuntimeMessage; // only one message per CoordinatePartitionUnionizer instance

  public void addCoords(List<Coordinate> coords, PartitionCollectionMutable.Partition part) {
    Coordinate runtime = null;
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          CoordinateRuntime rtime = (CoordinateRuntime) coord;
          if (runtimeBuilder == null) runtimeBuilder = new CoordinateRuntime.Builder2(rtime.getTimeUnits());
          runtimeBuilder.addAll(coord);
          runtime = coord;
          if (debugPartitionErrors && !duplicateRuntimeMessage && part != null)
            testDuplicateRuntime(rtime, part);
          break;

        case time:
          CoordinateTime time = (CoordinateTime) coord;
          if (timeBuilder == null) timeBuilder = new CoordinateTime.Builder2(coord.getCode(), time.getTimeUnit(), time.getRefDate());
          timeBuilder.addAll(coord);
          break;

        case timeIntv:
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) coord;
          if (timeIntvBuilder == null) timeIntvBuilder = new CoordinateTimeIntv.Builder2(null, coord.getCode(), timeIntv.getTimeUnit(), timeIntv.getRefDate());
          timeIntvBuilder.addAll(intervalFilter((CoordinateTimeIntv)coord));
          break;

        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          if (time2DBuilder == null) time2DBuilder = new CoordinateTime2DUnionizer(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode(), false, logger);
          time2DBuilder.addAll(time2D);
          // debug
          CoordinateRuntime runtimeFrom2D = time2D.getRuntimeCoordinate();
          if (!runtimeFrom2D.equals(runtime))
            logger.warn("HEY CoordinateUnionizer runtimes not equal");
          break;

        case ens:
          if (ensBuilder == null) ensBuilder = new CoordinateEns.Builder2(coord.getCode());
          ensBuilder.addAll(coord);
          break;

        case vert:
          CoordinateVert vertCoord = (CoordinateVert) coord;
          if (vertBuilder == null) vertBuilder = new CoordinateVert.Builder2(coord.getCode(), vertCoord.getVertUnit());
          vertBuilder.addAll(coord);
          break;
      }
    }
  }

  private void testDuplicateRuntime(CoordinateRuntime runtime, PartitionCollectionMutable.Partition part) {
    PartitionCollectionMutable.Partition shownPrevPart = null;
    for (int idx=0; idx<runtime.getNCoords(); idx++) {    // possible duplicate runtimes from different partitions
      long time = runtime.getRuntime(idx);
      PartitionCollectionMutable.Partition prevPart = timeMap.get(time);
      if (prevPart != null && prevPart != part && prevPart != shownPrevPart) {
        logger.warn("Variable {} Runtime {} in part {} and partition {}", vi.id(), CalendarDate.of(time), prevPart.getName(), part.getName());
        shownPrevPart = prevPart; // eliminate extra messages
        duplicateRuntimeMessage = true;
      }
      timeMap.put(time, part);
    }
  }

  private List<TimeCoordIntvValue> intervalFilter(CoordinateTimeIntv coord) {
    if (intvFilter == null) return coord.getTimeIntervals();
    List<TimeCoordIntvValue> result = new ArrayList<>();
    for (TimeCoordIntvValue tinv : coord.getTimeIntervals()) {
      if (!intvFilter.filter(vi.getVarid(), tinv.getBounds1(), tinv.getBounds2(), Integer.MIN_VALUE))
        result.add(tinv);
    }
    return result;
  }

   public List<Coordinate> finish() {
    if (runtimeBuilder != null)
      unionCoords.add(runtimeBuilder.finish());
    else
      logger.warn("HEY CoordinateUnionizer missing runtime");

    if (timeBuilder != null)
      unionCoords.add(timeBuilder.finish());
    else if (timeIntvBuilder != null)
      unionCoords.add(timeIntvBuilder.finish());
    else if (time2DBuilder != null)
      unionCoords.add(time2DBuilder.finish());
    else
      logger.warn("HEY CoordinateUnionizer missing time");

    if (ensBuilder != null) // ens must come before vert to preserve order
       unionCoords.add(ensBuilder.finish());
    if (vertBuilder != null)
      unionCoords.add(vertBuilder.finish());

    // result = new CoordinateND<>(unionCoords);
    return unionCoords;
  }

}
