/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.coord;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.TimeCoord;

import java.util.*;

/**
 * Create the overall coordinate across the same variable in different partitions
 * The CoordinateBuilders create unique sets of Coordinates.
 * The CoordinateND result is then the cross-product of those Coordinates.
 *
 * This is a builder helper class, the result is obtained from List<Coordinate> finish().
 *
 * So if theres a lot of missing records in that cross-product, we may have the variable wrong (?),
 *  or our assumption that the data comprises a multidim array may be wrong
 *
 * @author John
 * @since 12/10/13
 */
public class CoordinateUnionizer {
  // static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateUnionizer.class);

  FeatureCollectionConfig.GribIntvFilter intvFilter;
  org.slf4j.Logger logger;
  int varId;

  public CoordinateUnionizer(int varId, FeatureCollectionConfig.GribIntvFilter intvFilter, org.slf4j.Logger logger) {
    this.varId = varId;
    this.intvFilter = intvFilter;
    this.logger = logger;
  }

  List<Coordinate> unionCoords = new ArrayList<>();

  CoordinateBuilder runtimeBuilder ;
  CoordinateBuilder timeBuilder;
  CoordinateBuilder timeIntvBuilder;
  CoordinateBuilder vertBuilder;
  CoordinateBuilder ensBuilder;
  CoordinateTime2DUnionizer time2DBuilder;

  public void addCoords(List<Coordinate> coords) {
    Coordinate runtime = null;
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          CoordinateRuntime rtime = (CoordinateRuntime) coord;
          if (runtimeBuilder == null) runtimeBuilder = new CoordinateRuntime.Builder2(rtime.getTimeUnits());
          runtimeBuilder.addAll(coord);
          runtime = coord;
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

  private List<TimeCoord.Tinv> intervalFilter(CoordinateTimeIntv coord) {
    if (intvFilter == null) return coord.getTimeIntervals();
    List<TimeCoord.Tinv> result = new ArrayList<>();
    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      if (intvFilter.filterOk(varId, tinv.getIntervalSize(), 0))
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
