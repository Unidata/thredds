/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.units.DateUnit;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.Station;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.ma2.Section;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.google.protobuf.ByteString;

/**
 * Defines the point stream format, along with pointStream.proto.
 * cd /dev/tds/thredds/cdm/src/main/java
 * protoc --proto_path=. --java_out=. ucar/nc2/ft/point/remote/pointStream.proto
 *
 * @author caron
 * @since Feb 16, 2009
 */
public class PointStream {

  static public PointStreamProto.PointFeatureCollection encodePointFeatureCollection(String name, PointFeature pf) throws IOException {
    PointStreamProto.PointFeatureCollection.Builder builder = PointStreamProto.PointFeatureCollection.newBuilder();
    if (name == null)
      System.out.printf("HEY%n");
    builder.setName(name);
    builder.setTimeUnit( pf.getTimeUnit().getUnitsString());

    StructureData sdata = pf.getData();
    StructureMembers sm = sdata.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      PointStreamProto.Member.Builder mbuilder = PointStreamProto.Member.newBuilder();
      mbuilder.setName( m.getName());
      if (null != m.getDescription())
        mbuilder.setDesc( m.getDescription());
      if (null != m.getUnitsString())
        mbuilder.setUnits( m.getUnitsString());
      mbuilder.setDataType( NcStream.encodeDataType(m.getDataType()));
      mbuilder.setSection( NcStream.encodeSection( new ucar.ma2.Section( m.getShape())));
      builder.addMembers(mbuilder);
    }

    return builder.build();
  }

  static public PointStreamProto.PointFeature encodePointFeature(PointFeature pf) throws IOException {
    PointStreamProto.Location.Builder locBuilder = PointStreamProto.Location.newBuilder();
    locBuilder.setTime(pf.getObservationTime());
    if (!Double.isNaN( pf.getNominalTime()) && (pf.getNominalTime() != pf.getObservationTime()))
      locBuilder.setNomTime(pf.getNominalTime());
    
    EarthLocation loc = pf.getLocation();
    locBuilder.setLat(loc.getLatitude());
    locBuilder.setLon(loc.getLongitude());
    if (!Double.isNaN( loc.getAltitude()))
      locBuilder.setAlt(loc.getAltitude());

    PointStreamProto.PointFeature.Builder builder = PointStreamProto.PointFeature.newBuilder();
    builder.setLoc(locBuilder);

    StructureData sdata = pf.getData();
    StructureMembers sm = sdata.getStructureMembers();
    int size = sm.getStructureSize();
    ByteBuffer bb = ByteBuffer.allocate(size);
    int stringno = 0;
    for (StructureMembers.Member m : sm.getMembers()) {
      //System.out.printf("%s offset=%d%n", m.getName(), bb.position());      
      if (m.getDataType() == DataType.STRING) {
        builder.addSdata(sdata.getScalarString(m));
        bb.putInt(stringno++); // 4 bytes for the string num on the object heap. LOOK could optimize this
      } else if (m.getDataType() == DataType.DOUBLE)
        bb.putDouble(sdata.getScalarDouble(m));
      else if (m.getDataType() == DataType.FLOAT)
        bb.putFloat(sdata.getScalarFloat(m));
      else if (m.getDataType() == DataType.INT)
        bb.putInt(sdata.getScalarInt(m));
      else if (m.getDataType() == DataType.SHORT)
        bb.putShort(sdata.getScalarShort(m));
      else if (m.getDataType() == DataType.BYTE)
        bb.put(sdata.getScalarByte(m));
      else if (m.getDataType() == DataType.CHAR) {
        for (char c : sdata.getJavaArrayChar(m))
          bb.put((byte) c);
      } else
        System.out.println(" unimplemented type = "+m.getDataType());

    }
    //System.out.println(" size= "+size+" bb="+bb.limit());
    builder.setData( ByteString.copyFrom( bb.array()));
    return builder.build();
  }

  static public PointStreamProto.StationList encodeStations(List<Station> stnList) throws IOException {
    PointStreamProto.StationList.Builder stnBuilder = PointStreamProto.StationList.newBuilder();
    for (Station loc : stnList) {
      PointStreamProto.Station.Builder locBuilder = PointStreamProto.Station.newBuilder();

      locBuilder.setId(loc.getName());
      locBuilder.setLat(loc.getLatitude());
      locBuilder.setLon(loc.getLongitude());
      if (!Double.isNaN( loc.getAltitude()))
        locBuilder.setAlt(loc.getAltitude());
      if (loc.getDescription() != null)
        locBuilder.setDesc(loc.getDescription());
      if (loc.getWmoId() != null)
        locBuilder.setWmoId(loc.getWmoId());

      stnBuilder.addStations(locBuilder);
    }

    return stnBuilder.build();
  }

}
