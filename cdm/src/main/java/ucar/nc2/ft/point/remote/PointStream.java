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
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.units.DateUnit;
import ucar.nc2.stream.NcStream;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.Station;
import ucar.ma2.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Defines the point stream format, along with pointStream.proto.
 *
 cd c:/dev/tds4.2/thredds/cdm/src/main/java
 protoc --proto_path=. --java_out=. ucar/nc2/ft/point/remote/pointStream.proto
 *
 * @author caron
 * @since Feb 16, 2009
 */
public class PointStream {
  public enum MessageType {
    Start, Header, Data, End, Error, Eos,
    StationList, PointFeatureCollection, PointFeature
  }

  static private final byte[] MAGIC_StationList = new byte[]{(byte) 0xfe, (byte) 0xfe, (byte) 0xef, (byte) 0xef};
  static private final byte[] MAGIC_PointFeatureCollection = new byte[]{(byte) 0xfa, (byte) 0xfa, (byte) 0xaf, (byte) 0xaf};
  static private final byte[] MAGIC_PointFeature = new byte[]{(byte) 0xf0, (byte) 0xf0, (byte) 0x0f, (byte) 0x0f};

  static private final boolean debug = false;

  static public MessageType readMagic(InputStream is) throws IOException {
    byte[] b = new byte[4];
    int done = NcStream.readFully(is, b);
    if (done != 4) return MessageType.Eos;

    if (test(b, MAGIC_PointFeature)) return MessageType.PointFeature;
    if (test(b, MAGIC_PointFeatureCollection)) return MessageType.PointFeatureCollection;
    if (test(b, MAGIC_StationList)) return MessageType.StationList;
    if (test(b, NcStream.MAGIC_START)) return MessageType.Start;
    if (test(b, NcStream.MAGIC_HEADER)) return MessageType.Header;
    if (test(b, NcStream.MAGIC_DATA)) return MessageType.Data;
    if (test(b, NcStream.MAGIC_END)) return MessageType.End;
    if (test(b, NcStream.MAGIC_ERR)) return MessageType.Error;
    return null;
  }

  static public int writeMagic(OutputStream out, MessageType type) throws IOException {
    switch (type) {
      case PointFeature:
        return NcStream.writeBytes(out, PointStream.MAGIC_PointFeature);
      case PointFeatureCollection:
        return NcStream.writeBytes(out, PointStream.MAGIC_PointFeatureCollection);
      case StationList:
        return NcStream.writeBytes(out, PointStream.MAGIC_StationList);
      case End:
        return NcStream.writeBytes(out, NcStream.MAGIC_END);
      case Error:
        return NcStream.writeBytes(out, NcStream.MAGIC_ERR);
    }
    return 0;
  }

  private static boolean test(byte[] b, byte[] m) {
    if (b.length != m.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != m[i]) return false;
    return true;
  }

  static public PointStreamProto.PointFeatureCollection encodePointFeatureCollection(String name, PointFeature pf) throws IOException {
    PointStreamProto.PointFeatureCollection.Builder builder = PointStreamProto.PointFeatureCollection.newBuilder();
    if (name == null)
      System.out.printf("HEY null pointstream name%n");
    builder.setName(name);
    builder.setTimeUnit(pf.getTimeUnit().getUnitsString());

    StructureData sdata = pf.getData();
    StructureMembers sm = sdata.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      PointStreamProto.Member.Builder mbuilder = PointStreamProto.Member.newBuilder();
      mbuilder.setName(m.getName());
      if (null != m.getDescription())
        mbuilder.setDesc(m.getDescription());
      if (null != m.getUnitsString())
        mbuilder.setUnits(m.getUnitsString());
      mbuilder.setDataType(NcStream.encodeDataType(m.getDataType()));
      mbuilder.setSection(NcStream.encodeSection(new ucar.ma2.Section(m.getShape())));
      builder.addMembers(mbuilder);
    }

    return builder.build();
  }

  static public PointStreamProto.PointFeature encodePointFeature(PointFeature pf) throws IOException {
    PointStreamProto.Location.Builder locBuilder = PointStreamProto.Location.newBuilder();
    locBuilder.setTime(pf.getObservationTime());
    if (!Double.isNaN(pf.getNominalTime()) && (pf.getNominalTime() != pf.getObservationTime()))
      locBuilder.setNomTime(pf.getNominalTime());

    EarthLocation loc = pf.getLocation();
    locBuilder.setLat(loc.getLatitude());
    locBuilder.setLon(loc.getLongitude());
    if (!Double.isNaN(loc.getAltitude()))
      locBuilder.setAlt(loc.getAltitude());

    PointStreamProto.PointFeature.Builder builder = PointStreamProto.PointFeature.newBuilder();
    builder.setLoc(locBuilder);

    StructureData sdata = pf.getData();
    ArrayStructureBB abb = IospHelper.copyToArrayBB(sdata);
    ByteBuffer bb = abb.getByteBuffer();
    if (debug) {
      StructureMembers sm = sdata.getStructureMembers();
      int size = sm.getStructureSize();
      System.out.printf("encodePointFeature size= %d bb=%d%n", size, bb.position());
    }
    builder.setData(ByteString.copyFrom(bb.array()));
    List<Object> heap = abb.getHeap();
    if (heap != null) {
      for (Object ho : heap) {
        if (ho instanceof String)
          builder.addSdata((String) ho);
        else if (ho instanceof String[])
          builder.addSdata((String) ho);  // LOOK cast != instance
        else
          throw new IllegalStateException("illegal object on heap = "+ho);
      }
    }
    return builder.build();
  }

  static public PointStreamProto.StationList encodeStations(List<Station> stnList) throws IOException {
    PointStreamProto.StationList.Builder stnBuilder = PointStreamProto.StationList.newBuilder();
    for (Station loc : stnList) {
      PointStreamProto.Station.Builder locBuilder = PointStreamProto.Station.newBuilder();

      locBuilder.setId(loc.getName());
      locBuilder.setLat(loc.getLatitude());
      locBuilder.setLon(loc.getLongitude());
      if (!Double.isNaN(loc.getAltitude()))
        locBuilder.setAlt(loc.getAltitude());
      if (loc.getDescription() != null)
        locBuilder.setDesc(loc.getDescription());
      if (loc.getWmoId() != null)
        locBuilder.setWmoId(loc.getWmoId());

      stnBuilder.addStations(locBuilder);
    }

    return stnBuilder.build();
  }

  //////////////////////////////////////////////////////////////////
  // decoding
  // makes a PointFeature from the raw bytes of the protobuf message

  static class ProtobufPointFeatureMaker implements FeatureMaker {
    private DateUnit dateUnit;
    private StructureMembers sm;

    ProtobufPointFeatureMaker(PointStreamProto.PointFeatureCollection pfc) throws IOException {
      try {
        dateUnit = new DateUnit(pfc.getTimeUnit());
      } catch (Exception e) {
        e.printStackTrace();
        dateUnit = DateUnit.getUnixDateUnit();
      }

      sm = new StructureMembers(pfc.getName());
      for (PointStreamProto.Member m : pfc.getMembersList()) {
        sm.addMember(m.getName(), m.getDesc(), m.getUnits(),
                NcStream.decodeDataType(m.getDataType()),
                NcStream.decodeSection(m.getSection()).getShape());
      }
      ArrayStructureBB.setOffsets(sm);
    }

    public PointFeature make(byte[] rawBytes) throws InvalidProtocolBufferException {
      PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(rawBytes);
      PointStreamProto.Location locp = pfp.getLoc();
      EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());
      return new MyPointFeature(location, locp.getTime(), locp.getNomTime(), dateUnit, pfp);
    }

    private class MyPointFeature extends PointFeatureImpl {
      PointStreamProto.PointFeature pfp;

      MyPointFeature(EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit, PointStreamProto.PointFeature pfp) {
        super(location, obsTime, nomTime, timeUnit);
        this.pfp = pfp;
      }

      public StructureData getData() throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(pfp.getData().toByteArray());
        ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[]{1}, bb, 0);
        for (String s : pfp.getSdataList())
          asbb.addObjectToHeap(s);
        return asbb.getStructureData(0);
      }

      public String toString() {
        return location + " obs=" + obsTime + " nom=" + nomTime;
      }
    }
  }



}
