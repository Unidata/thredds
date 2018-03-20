/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.remote;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import ucar.ma2.ArrayStructureBB;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataDeep;
import ucar.ma2.StructureMembers;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointFeatureImpl;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.Station;

/**
 * Defines the point stream format, along with pointStream.proto.
 *
 cd c:/dev/github/thredds/cdm/src/main/java
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
      case Start:
        return NcStream.writeBytes(out, NcStream.MAGIC_START);
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

  static public PointStreamProto.PointFeatureCollection encodePointFeatureCollection(
          String name, String timeUnitString, String altUnits, PointFeature pf) throws IOException {
    PointStreamProto.PointFeatureCollection.Builder builder = PointStreamProto.PointFeatureCollection.newBuilder();
    if (name == null)
      System.out.printf("HEY null pointstream name%n");
    builder.setName(name);
    builder.setTimeUnit(timeUnitString);

    if (altUnits != null) {
      builder.setAltUnit(altUnits);
    }

    StructureData sdata = pf.getDataAll();
    StructureMembers sm = sdata.getStructureMembers();
    for (StructureMembers.Member m : sm.getMembers()) {
      PointStreamProto.Member.Builder mbuilder = PointStreamProto.Member.newBuilder();
      mbuilder.setName(m.getName());
      if (null != m.getDescription())
        mbuilder.setDesc(m.getDescription());
      if (null != m.getUnitsString())
        mbuilder.setUnits(m.getUnitsString());
      mbuilder.setDataType(NcStream.convertDataType(m.getDataType()));
      mbuilder.setSection(NcStream.encodeSection(new ucar.ma2.Section(m.getShape())));
      builder.addMembers(mbuilder);
    }

    return builder.build();
  }

  static public PointStreamProto.PointFeature encodePointFeature(PointFeature pf) throws IOException {
    PointStreamProto.Location.Builder locBuilder = PointStreamProto.Location.newBuilder();
    locBuilder.setTime(pf.getObservationTime());
    locBuilder.setNomTime(pf.getNominalTime());

    EarthLocation loc = pf.getLocation();
    locBuilder.setLat(loc.getLatitude());
    locBuilder.setLon(loc.getLongitude());
    locBuilder.setAlt(loc.getAltitude());

    PointStreamProto.PointFeature.Builder builder = PointStreamProto.PointFeature.newBuilder();
    builder.setLoc(locBuilder);

    StructureData sdata = pf.getDataAll();
    ArrayStructureBB abb = StructureDataDeep.copyToArrayBB(sdata);
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
          builder.addAllSdata(Arrays.asList((String[]) ho));
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
    private CalendarDateUnit dateUnit;
    private StructureMembers sm;

    ProtobufPointFeatureMaker(PointStreamProto.PointFeatureCollection pfc) throws IOException {
      try {
        // LOOK No calendar
        dateUnit = CalendarDateUnit.of(null, pfc.getTimeUnit());
      } catch (Exception e) {
        e.printStackTrace();
        dateUnit = CalendarDateUnit.unixDateUnit;
      }

      sm = new StructureMembers(pfc.getName());
      for (PointStreamProto.Member m : pfc.getMembersList()) {
        String name = m.getName();
        String desc = m.getDesc().length() > 0 ? m.getDesc() : null;
        String units = m.getUnits().length() > 0 ? m.getUnits() : null;
        DataType dtype = NcStream.convertDataType(m.getDataType());
        int[] shape = NcStream.decodeSection(m.getSection()).getShape();

        sm.addMember(name, desc, units, dtype, shape);
      }
      ArrayStructureBB.setOffsets(sm);
    }

    @Override
    public PointFeature make(DsgFeatureCollection dsg, byte[] rawBytes) throws InvalidProtocolBufferException {
      PointStreamProto.PointFeature pfp = PointStreamProto.PointFeature.parseFrom(rawBytes);
      PointStreamProto.Location locp = pfp.getLoc();
      EarthLocationImpl location = new EarthLocationImpl(locp.getLat(), locp.getLon(), locp.getAlt());
      return new MyPointFeature(dsg, location, locp.getTime(), locp.getNomTime(), dateUnit, pfp);
    }

    private class MyPointFeature extends PointFeatureImpl {
      PointStreamProto.PointFeature pfp;

      MyPointFeature(DsgFeatureCollection dsg, EarthLocation location, double obsTime, double nomTime, CalendarDateUnit timeUnit, PointStreamProto.PointFeature pfp) {
        super(dsg, location, obsTime, nomTime, timeUnit);
        this.pfp = pfp;
      }

      @Nonnull
      @Override
      public StructureData getFeatureData() throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(pfp.getData().toByteArray());
        ArrayStructureBB asbb = new ArrayStructureBB(sm, new int[]{1}, bb, 0);
        for (String s : pfp.getSdataList()) {
          asbb.addObjectToHeap(s);
        }
        return asbb.getStructureData(0);
      }

      @Nonnull
      @Override
      public StructureData getDataAll() throws IOException {
        return getFeatureData();
      }

      public String toString() {
        return location + " obs=" + obsTime + " nom=" + nomTime;
      }
    }
  }

  public static int write(PointFeatureCollection pointFeatCol, File outFile) throws IOException {
    String name = outFile.getCanonicalPath();
    String timeUnitString = pointFeatCol.getTimeUnit().getUdUnit();
    String altUnits = pointFeatCol.getAltUnits();
    PointFeatureIterator pointFeatIter = pointFeatCol.getPointFeatureIterator();

    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
      return write(out, pointFeatIter, name, timeUnitString, altUnits);
    } finally {
      pointFeatIter.close();
    }
  }

  // Adapted from thredds.server.cdmremote.PointWriter.WriterNcstream
  // Caller must iter.finish() and out.close().
  public static int write(OutputStream out, PointFeatureIterator pointFeatIter, String name, String timeUnitString,
          String altUnits) throws IOException {
    int numWritten = 0;

    while (pointFeatIter.hasNext()) {
      try {
        PointFeature pointFeat = pointFeatIter.next();

        if (numWritten == 0) {
          PointStreamProto.PointFeatureCollection protoPfc =
                  PointStream.encodePointFeatureCollection(name, timeUnitString, altUnits, pointFeat);
          byte[] data = protoPfc.toByteArray();

          PointStream.writeMagic(out, MessageType.PointFeatureCollection);
          NcStream.writeVInt(out, data.length);
          out.write(data);
        }

        PointStreamProto.PointFeature protoPointFeat = PointStream.encodePointFeature(pointFeat);
        byte[] data = protoPointFeat.toByteArray();

        PointStream.writeMagic(out, MessageType.PointFeature);
        NcStream.writeVInt(out, data.length);
        out.write(data);

        ++numWritten;
      } catch (Throwable t) {
        NcStreamProto.Error protoError =
                NcStream.encodeErrorMessage(t.getMessage() != null ? t.getMessage() : t.getClass().getName());
        byte[] data = protoError.toByteArray();

        PointStream.writeMagic(out, PointStream.MessageType.Error);
        NcStream.writeVInt(out, data.length);
        out.write(data);

        throw new IOException(t);
      }
    }

    PointStream.writeMagic(out, PointStream.MessageType.End);

    return numWritten;
  }
}
