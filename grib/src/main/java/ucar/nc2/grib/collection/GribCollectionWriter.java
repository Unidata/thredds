/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import ucar.coord.*;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.time.CalendarDate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Common superclass for writing Grib ncx3 files
 *
 * @author caron
 * @since 2/20/14
 */
class GribCollectionWriter {
  static public final int currentVersion = 1;
  protected GribCollectionProto.Gds writeGdsProto(GribHorizCoordSystem hcs) throws IOException {
    return writeGdsProto(hcs.getRawGds(), hcs.getPredefinedGridDefinition());
  }

    /*
  message Gds {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;  // only when user overrides default name
  }
   */
  protected GribCollectionProto.Gds writeGdsProto(byte[] rawGds, int predefinedGridDefinition) throws IOException {
    GribCollectionProto.Gds.Builder b = GribCollectionProto.Gds.newBuilder();

    if (predefinedGridDefinition >= 0)
      b.setPredefinedGridDefinition(predefinedGridDefinition);
    else {
      b.setGds(ByteString.copyFrom(rawGds));
    }

    return b.build();
  }

    /*
  message Coord {
    required int32 type = 1;   // Coordinate.Type.oridinal
    required int32 code = 2;   // time unit; level type
    required string unit = 3;
    repeated float values = 4;
    repeated float bound = 5; // only used if interval, then = (value, bound)
    repeated int64 msecs = 6; // calendar date
   */
  protected GribCollectionProto.Coord writeCoordProto(CoordinateRuntime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    if (coord.getUnit() != null) b.setUnit(coord.getUnit());

    for (int idx=0; idx<coord.getSize(); idx++) {
      long runtime = coord.getRuntime(idx);
      b.addMsecs(runtime);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillis());
    for (Integer offset : coord.getOffsetSorted())
      b.addValues(offset);

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null)
      for (int val : time2runtime)
        b.addTime2Runtime(val);

    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTimeIntv coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillis());

    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      b.addValues(tinv.getBounds1());
      b.addBound(tinv.getBounds2());
    }

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null)
      for (int val : time2runtime)
        b.addTime2Runtime(val);

    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateVert coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());

    if (coord.getUnit() != null) b.setUnit(coord.getUnit());
    for (VertCoord.Level level : coord.getLevelSorted()) {
      if (coord.isLayer()) {
        b.addValues((float) level.getValue1());
        b.addBound((float) level.getValue2());
      } else {
        b.addValues((float) level.getValue1());
      }
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateEns coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());

    if (coord.getUnit() != null) b.setUnit(coord.getUnit());
    for (EnsCoord.Coord level : coord.getEnsSorted()) {
      b.addValues((float) level.getCode());       // lame
      b.addBound((float) level.getEnsMember());
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTime2D coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    CoordinateRuntime runtimeCoord = coord.getRuntimeCoordinate();
    for (int idx=0; idx<runtimeCoord.getSize(); idx++) {
      long runtime = runtimeCoord.getRuntime(idx);
      b.addMsecs(runtime);
    }

    b.setIsOrthogonal(coord.isOrthogonal());
    b.setIsRegular(coord.isRegular());
    for (Coordinate time : coord.getTimesForSerialization()) {
      if (time.getType() == Coordinate.Type.time)
        b.addTimes(writeCoordProto((CoordinateTime)time));
      else
        b.addTimes(writeCoordProto((CoordinateTimeIntv)time));
    }

    return b.build();
  }

  protected GribCollectionProto.FcConfig writeConfig(FeatureCollectionConfig config) throws IOException {
    GribCollectionProto.FcConfig.Builder b = GribCollectionProto.FcConfig.newBuilder();
    b.setName(config.collectionName);
    b.setCollectionSpec(config.spec);
    b.setPartitionType(config.ptype.toString());
    if (config.dateFormatMark != null)
      b.setDateFormatMark(config.dateFormatMark);

    FeatureCollectionConfig.GribConfig gribConfig = config.gribConfig;
    if (gribConfig.gdsHash != null) {
      for (Map.Entry<Integer, Integer> entry : gribConfig.gdsHash.entrySet()) {
        GribCollectionProto.IntMap.Builder bIntMap = GribCollectionProto.IntMap.newBuilder();
        bIntMap.setFrom(entry.getKey());
        bIntMap.setTo(entry.getValue());
        b.addGdsConvert(bIntMap);
      }
    }

    b.setPdsUseGenType(gribConfig.useGenType);
    b.setPdsUseTableVersion(gribConfig.useTableVersion);
    b.setPdsIntvMerge(gribConfig.intvMerge);
    b.setPdsUseCenter(gribConfig.useCenter);

    if (gribConfig.intvFilter != null) {
      b.setIntvExcludeZero(gribConfig.intvFilter.isZeroExcluded());
      for (FeatureCollectionConfig.GribIntvFilterParam intvFilter : gribConfig.intvFilter.filter) {
        GribCollectionProto.IntvFilter.Builder bIntv = GribCollectionProto.IntvFilter.newBuilder();
        bIntv.setVariableId(intvFilter.id);
        bIntv.setIntvLength(intvFilter.intvLength);
        if (intvFilter.prob != Integer.MIN_VALUE)
          bIntv.setIntvProb(intvFilter.prob);
        b.addIntvFilter(bIntv);
      }
    }

    // time unit convert
    if (gribConfig.tuc != null) {
      for (Map.Entry<Integer, Integer> entry : gribConfig.tuc.map.entrySet()) {
        GribCollectionProto.IntMap.Builder bIntMap = GribCollectionProto.IntMap.newBuilder();
        bIntMap.setFrom(entry.getKey());
        bIntMap.setTo(entry.getValue());
        b.addTimeUnitConvert(bIntMap);
      }
    }

    if (gribConfig.userTimeUnit != null)
      b.setUserTimeUnit(gribConfig.userTimeUnit.toString());

    return b.build();
  }

  protected FeatureCollectionConfig readConfig(boolean isGrib1, GribCollectionProto.FcConfig pconfig) throws IOException {

    FeatureCollectionConfig config = new FeatureCollectionConfig();
    config.name = pconfig.getName();
    config.collectionName = pconfig.getName();
    config.type = isGrib1 ? FeatureCollectionType.GRIB1 : FeatureCollectionType.GRIB2;
    config.spec = pconfig.getCollectionSpec();
    config.ptype = FeatureCollectionConfig.PartitionType.valueOf(pconfig.getPartitionType());
    if (pconfig.hasDateFormatMark())
      config.dateFormatMark = pconfig.getDateFormatMark();

    if ( pconfig.getGdsConvertCount() > 0) {
      config.gribConfig.gdsHash =  new HashMap<>();
      for (GribCollectionProto.IntMap pIntMap : pconfig.getGdsConvertList()) {
        config.gribConfig.gdsHash.put(pIntMap.getFrom(), pIntMap.getTo());
      }
    }

    config.gribConfig.useGenType = pconfig.getPdsUseGenType();
    config.gribConfig.useTableVersion = pconfig.getPdsUseTableVersion();
    config.gribConfig.intvMerge = pconfig.getPdsIntvMerge();
    config.gribConfig.useCenter = pconfig.getPdsUseCenter();

    boolean isZeroExcluded = pconfig.getIntvExcludeZero();
    if ( isZeroExcluded || pconfig.getIntvFilterCount() > 0) {
      config.gribConfig.intvFilter =  new FeatureCollectionConfig.GribIntvFilter();
      config.gribConfig.intvFilter.isZeroExcluded = isZeroExcluded;
      config.gribConfig.intvFilter.filter = new ArrayList<>();
      for (GribCollectionProto.IntvFilter pi :  pconfig.getIntvFilterList()) {
        int prob =  pi.hasIntvProb() ? pi.getIntvProb() : Integer.MIN_VALUE;
        config.gribConfig.intvFilter.filter.add(new FeatureCollectionConfig.GribIntvFilterParam(pi.getVariableId(), pi.getIntvLength(), prob));
      }
    }

    if ( pconfig.getTimeUnitConvertCount() > 0) {
      config.gribConfig.tuc =  new FeatureCollectionConfig.TimeUnitConverterHash();
      for (GribCollectionProto.IntMap pIntMap :  pconfig.getTimeUnitConvertList()) {
        config.gribConfig.tuc.map.put(pIntMap.getFrom(), pIntMap.getTo());
      }
    }

    if (pconfig.hasUserTimeUnit())
      config.gribConfig.setUserTimeUnit(pconfig.getUserTimeUnit());

    return config;
  }


}
