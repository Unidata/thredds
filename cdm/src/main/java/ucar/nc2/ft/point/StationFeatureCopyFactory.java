/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.Station;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 6/20/2014
 */
public class StationFeatureCopyFactory {

  static private final int POINTER_SIZE = 8; // assume 64 bit pointers could do better with -XX:+UseCompressedOops
  static private final int OBJECT_SIZE = 40; // overhead per object estimate
  static private final int ARRAY_SIZE = 8;   // assume 64 bit pointers

  private final StructureMembers sm;
  private final DateUnit du;
  private final int sizeInBytes;

  public StationFeatureCopyFactory(StationPointFeature proto) throws IOException {
    StructureData sdata = proto.getData();
    sm = new StructureMembers(sdata.getStructureMembers());
    du = proto.getTimeUnit();
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
    StationPointFeatureCopy deep = new StationPointFeatureCopy(from);
    deep.data = StructureDataDeep.copy(from.getData(), sm);
    return deep;
  }

  private class StationPointFeatureCopy extends PointFeatureImpl implements StationPointFeature {

    StructureData data;

    StationPointFeatureCopy(PointFeature pf) {
      super(pf.getLocation(), pf.getObservationTime(), pf.getNominalTime(), du);
    }

    @Override
    public StructureData getData() throws IOException {
      return data;
    }

    @Override
    public Station getStation() {
      return null;
    }
  }
}
