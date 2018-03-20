/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import com.google.protobuf.ByteString;
import thredds.inventory.MCollection;
import ucar.coord.*;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import java.io.IOException;

/**
 * Common superclass for writing Grib ncx files
 *
 * @author caron
 * @since 2/20/14
 */
class GribCollectionWriter {
  static public final int currentVersion = 1;

  protected final MCollection dcm; // may be null, when read in from index
  protected final org.slf4j.Logger logger;

  protected GribCollectionWriter(MCollection dcm, org.slf4j.Logger logger) {
    this.dcm = dcm;
    this.logger = logger;
  }

  protected GribCollectionProto.Gds writeGdsProto(GribHorizCoordSystem hcs) throws IOException {
    return writeGdsProto(hcs.getRawGds(), hcs.getPredefinedGridDefinition());
  }

  /*
    message Gds {
      bytes gds = 1;                        // raw gds: Grib1SectionGridDefinition or Grib2SectionGridDefinition
      uint32 predefinedGridDefinition = 2;  // only grib1; instead of gds raw bytes; need center, subcenter to interpret
    }
   */
  static GribCollectionProto.Gds writeGdsProto(byte[] rawGds, int predefinedGridDefinition) throws IOException {
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
    GribAxisType axisType = 1;
    int32 code = 2;   // time unit; level type
    string unit = 3;
    repeated float values = 4;
    repeated float bound = 5; // only used if interval, then = (value, bound)
    repeated int64 msecs = 6; // calendar date
   */
  protected GribCollectionProto.Coord writeCoordProto(CoordinateRuntime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType( convertAxisType (coord.getType()));
    b.setCode(coord.getCode());
    if (coord.getUnit() != null)
      b.setUnit(coord.getUnit());

    for (int idx=0; idx<coord.getSize(); idx++) {
      long runtime = coord.getRuntime(idx);
      b.addMsecs(runtime);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
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
    b.setAxisType( convertAxisType (coord.getType()));
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
    b.setAxisType(convertAxisType(coord.getType()));
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
    b.setAxisType(convertAxisType(coord.getType()));
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
    b.setAxisType(convertAxisType(coord.getType()));
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

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null)
      for (int val : time2runtime)
        b.addTime2Runtime(val);

    return b.build();
  }

  ///////////////////////////////////////////////////////
  /* not currently used

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
      for (FeatureCollectionConfig.GribIntvFilterParam intvFilter : gribConfig.intvFilter.filterList) {
        GribCollectionProto.IntvFilter.Builder bIntv = GribCollectionProto.IntvFilter.newBuilder();
        bIntv.setVariableId(intvFilter.id);
        bIntv.setIntvLength(intvFilter.intvLength);
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
    if (pconfig.getDateFormatMark().length() > 0)
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
      config.gribConfig.intvFilter.filterList = new ArrayList<>();
      for (GribCollectionProto.IntvFilter pi :  pconfig.getIntvFilterList()) {
        int prob =  pi.getIntvProb();
        config.gribConfig.intvFilter.filterList.add(new FeatureCollectionConfig.GribIntvFilterParam(pi.getVariableId(), pi.getIntvLength(), prob));
      }
    }

    if ( pconfig.getTimeUnitConvertCount() > 0) {
      config.gribConfig.tuc =  new FeatureCollectionConfig.TimeUnitConverterHash();
      for (GribCollectionProto.IntMap pIntMap :  pconfig.getTimeUnitConvertList()) {
        config.gribConfig.tuc.map.put(pIntMap.getFrom(), pIntMap.getTo());
      }
    }

    if (pconfig.getDateFormatMark().length() > 0)
      config.gribConfig.setUserTimeUnit(pconfig.getDateFormatMark());

    return config;
  } */

  static public GribCollectionProto.GribAxisType convertAxisType(Coordinate.Type type) {
    switch (type) {
      case runtime:
        return GribCollectionProto.GribAxisType.runtime;
      case time:
        return GribCollectionProto.GribAxisType.time;
      case time2D:
        return GribCollectionProto.GribAxisType.time2D;
      case timeIntv:
        return GribCollectionProto.GribAxisType.timeIntv;
      case ens:
        return GribCollectionProto.GribAxisType.ens;
      case vert:
        return GribCollectionProto.GribAxisType.vert;
    }
    throw new IllegalStateException("illegal axis type " + type);
  }


}
