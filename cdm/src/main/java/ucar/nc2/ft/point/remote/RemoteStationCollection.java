package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.stream.NcStreamRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.units.DateUnit;
import ucar.ma2.*;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;
import ucar.unidata.geoloc.EarthLocationImpl;
import ucar.unidata.geoloc.EarthLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.nio.ByteBuffer;

import org.apache.commons.httpclient.HttpMethod;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Connect to remote Station Collection
 *
 * @author caron
 */
public class RemoteStationCollection extends StationTimeSeriesCollectionImpl {
  NcStreamRemote ncremote;

  public RemoteStationCollection(String name, NcStreamRemote ncremote) throws IOException {
    super(name);
    this.ncremote = ncremote;

    stationHelper = new StationHelper();

    HttpMethod method = null;
    try {
      String query = "stations";
      method = ncremote.sendQuery(query);
      InputStream in = method.getResponseBodyAsStream();

      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      PointStreamProto.StationList stationsp = PointStreamProto.StationList.parseFrom(b);
      for (ucar.nc2.ft.point.remote.PointStreamProto.Station sp : stationsp.getStationsList()) {
        Station s = new StationImpl(sp.getId(), sp.getDesc(), sp.getWmoId(), sp.getLat(), sp.getLon(), sp.getAlt());
        stationHelper.addStation(s);
      }

    } finally {
      if (method != null) method.releaseConnection();
    }

  }

  @Override
  public StationTimeSeriesFeature getStationFeature(Station s) throws IOException {
    return new RemoteStationFeatureImpl(s);
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {

    // an anonymous class iterating over the stations
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter = stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return new RemoteStationFeatureImpl(stationIter.next());
      }

      public void setBufferSize(int bytes) {
      }

      public void finish() {
      }
    };
  }

  private class RemoteStationFeatureImpl extends StationFeatureImpl {
    RemotePointFeatureIterator riter;

    RemoteStationFeatureImpl(Station s) {
      super(s, null, -1);
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {

      HttpMethod method = null;

      try {
        method = ncremote.sendQuery("stn=" + s.getName());
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);

        // dont know timeUnit unitil now
        try {
          timeUnit = new DateUnit(pfc.getTimeUnit());
        } catch (Exception e) {
          e.printStackTrace();
        }

        riter = new RemotePointFeatureIterator(method, in, new ProtobufPointFeatureMaker(pfc, timeUnit));
        riter.setCalculateBounds(this);
        return riter;

      } catch (Throwable t) {
        if (method != null) method.releaseConnection();
        // log.error( "", t);
        throw new IOException( t.getMessage());
      }
    }
  }

  // makes a PointFeature from the raw bytes of the protobuf message
  private class ProtobufPointFeatureMaker implements FeatureMaker {
    private DateUnit dateUnit;
    private StructureMembers sm;

    ProtobufPointFeatureMaker(PointStreamProto.PointFeatureCollection pfc, DateUnit dateUnit) throws IOException {
      this.dateUnit = dateUnit;

      int offset = 0;
      sm = new StructureMembers(pfc.getName());
      for (PointStreamProto.Member m : pfc.getMembersList()) {
        StructureMembers.Member member = sm.addMember(m.getName(), m.getDesc(), m.getUnits(),
                NcStream.decodeDataType(m.getDataType()),
                NcStream.decodeSection(m.getSection()).getShape());
        member.setDataParam(offset);
        //System.out.printf("%s offset=%d%n", member.getName(), offset);
        offset += member.getSizeBytes();
      }
      sm.setStructureSize(offset);
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
