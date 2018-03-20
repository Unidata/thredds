/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.PointFeature;
import javax.annotation.Nonnull;

/**
 * A factory for making deep copies of StationPointFeature, so all data is self contained.
 * A factory will use the first StationPointFeature to get the StructureMembers object, and the DateUnits, and uses that for all copies.
 * So all StationPointFeature must have the same StructureMembers and DateUnit.
 * It will keep a hashmap of Stations, and reuse the Station object.
 *
 * @author caron
 * @since 6/20/2014
 */
public class StationFeatureCopyFactory {

  static private final int POINTER_SIZE = 8; // assume 64 bit pointers could do better with -XX:+UseCompressedOops
  static private final int OBJECT_SIZE = 40; // overhead per object estimate
  static private final int ARRAY_SIZE = 8;   // assume 64 bit pointers

  private final Map<String, StationFeatureImpl> stationMap;
  private final StructureMembers sm;
  private final int sizeInBytes;

  public StationFeatureCopyFactory(StationPointFeature proto) throws IOException {
    stationMap = new HashMap<>();
    StructureData sdata = proto.getFeatureData();
    sm = new StructureMembers(sdata.getStructureMembers());
    sizeInBytes =  OBJECT_SIZE + POINTER_SIZE +       // PointFeatureCopy - 1 pointer                                             48
            2 * 8 + 2 * POINTER_SIZE +                // PointFeatureImpl - 2 doubles and 2 pointers                              32
            OBJECT_SIZE + 3 * 8 +                     // Earth Location - 3 doubles                                               64
            OBJECT_SIZE +                             // StructureDataDeep
            4 + POINTER_SIZE +                        // StructureDataA  - 1 int and 1 pointer
            OBJECT_SIZE + 4 + 2 * POINTER_SIZE +      // ArrayStructureBB - 1 int and 2 pointers (heap is optional)
            2 * POINTER_SIZE + 4 +                    // ArrayStructure - 2 pointers and an int
            OBJECT_SIZE + 8 * 4 + 8 + POINTER_SIZE +  // ByteBuffer - 8 ints, 1 long, 1 pointer
            sm.getStructureSize();                    // LOOK vlens, Strings  (Heap Size)
  }

  /**
   * approx size of each copy
   * @return approx size of each copy
   */
  public int getSizeInBytes() {
    return sizeInBytes;
  }

  public StationPointFeature deepCopy(StationPointFeature from) throws IOException {
    StationFeature s = from.getStation();
    StationFeatureImpl sUse = stationMap.get(s.getName());
    if (sUse == null) {
      sUse = new StationFeatureImpl(s);
      stationMap.put(s.getName(), sUse);
    }
    sUse.incrNobs();
    StationPointFeatureCopy deep = new StationPointFeatureCopy(sUse, from);
    deep.data = StructureDataDeep.copy(from.getFeatureData(), sm);
    return deep;
  }

  private class StationPointFeatureCopy extends PointFeatureImpl implements StationPointFeature {

    final StationFeature station;
    StructureData data;

    StationPointFeatureCopy(StationFeature station, PointFeature pf) {
      super(pf.getFeatureCollection(), station, pf.getObservationTime(), pf.getNominalTime(),
              pf.getFeatureCollection().getTimeUnit());
      this.station = station;
    }

    @Nonnull
    @Override
    public StructureData getDataAll() throws IOException {
      return data;  // ??
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
      return data;
    }

    @Override
    public StationFeature getStation() {
      return station;
    }
  }
}
