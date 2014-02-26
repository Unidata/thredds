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
import ucar.coord.*;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;

/**
 * Common superclass for writing Grib ncx2 files
 *
 * @author caron
 * @since 2/20/14
 */
public class GribCollectionWriter {

  protected GribCollectionProto.Gds writeGdsProto(GribCollection.HorizCoordSys hcs) throws IOException {
    return writeGdsProto(hcs.getGdsHash(), hcs.getRawGds(), hcs.getNameOverride(), hcs.getPredefinedGridDefinition());
  }

    /*
  message Gds {
    optional bytes gds = 1;             // all variables in the group use the same GDS
    optional sint32 gdsHash = 2 [default = 0];
    optional string nameOverride = 3;  // only when user overrides default name
  }
   */
  protected GribCollectionProto.Gds writeGdsProto(int gdsHash, byte[] rawGds, String nameOverride, int predefinedGridDefinition) throws IOException {
    GribCollectionProto.Gds.Builder b = GribCollectionProto.Gds.newBuilder();

    if (predefinedGridDefinition >= 0)
      b.setPredefinedGridDefinition(predefinedGridDefinition);
    else {
      b.setGds(ByteString.copyFrom(rawGds));
      b.setGdsHash(gdsHash);
    }
    if (nameOverride != null)
      b.setNameOverride(nameOverride);

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
    for (CalendarDate cd : coord.getRuntimesSorted()) {
      b.addMsecs(cd.getMillis());
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTime coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillis());
    for (Integer offset : coord.getOffsetSorted()) {
      b.addValues(offset);
    }
    return b.build();
  }

  protected GribCollectionProto.Coord writeCoordProto(CoordinateTimeIntv coord) throws IOException {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setType(coord.getType().ordinal());
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillis());

    // LOOK old way - do we need ?
    /*     float scale = (float) tc.getTimeUnitScale(); // deal with, eg, "6 hours" by multiplying values by 6
        if (tc.isInterval()) {
          for (TimeCoord.Tinv tinv : tc.getIntervals()) {
            b.addValues(tinv.getBounds1() * scale);
            b.addBound(tinv.getBounds2() * scale);
          } */
    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      b.addValues(tinv.getBounds1());
      b.addBound(tinv.getBounds2());
    }
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
    CoordinateRuntime runtime = coord.getRuntimeCoordinate();
    for (CalendarDate cd : runtime.getRuntimesSorted()) {
      b.addMsecs(cd.getMillis());
    }

    for (Coordinate time : coord.getTimes()) {
      if (time.getType() == Coordinate.Type.time)
        b.addTimes(writeCoordProto((CoordinateTime)time));
      else
        b.addTimes(writeCoordProto((CoordinateTimeIntv)time));
    }

    return b.build();
  }


}
