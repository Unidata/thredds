package ucar.nc2.ft.point.remote;

import ucar.nc2.ft.point.StationTimeSeriesCollectionImpl;
import ucar.nc2.ft.point.StationHelper;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;
import ucar.nc2.ft.point.standard.Cursor;
import ucar.nc2.ft.point.standard.StandardPointFeatureIterator;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.stream.NcStreamRemote;
import ucar.nc2.stream.NcStream;
import ucar.nc2.units.DateUnit;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.StationImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpMethod;

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
      method = ncremote.sendQuery("getStations");
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

  DateUnit dateUnit;

  private class RemoteStationFeatureImpl extends StationFeatureImpl {

    RemoteStationFeatureImpl(Station s) {
      super(s, dateUnit, -1);
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {

      HttpMethod method = null;

      try {
        method = ncremote.sendQuery("stn="+s.getName());
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
        PointFeatureIterator iter = new RemoteStationPointIterator(null);
        iter.setCalculateBounds(this);
        return iter;

      } finally {
        if (method != null) method.releaseConnection();
      }

    }

  }

  // the iterator over the observations
  private class RemoteStationPointIterator extends PointIteratorAbstract {
    StationFeatureImpl station;

    RemoteStationPointIterator(StationFeatureImpl station) throws IOException {
      this.station = station;
    }

    public boolean hasNext() throws IOException {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PointFeature next() throws IOException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void finish() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setBufferSize(int bytes) {
    }
  }




}
