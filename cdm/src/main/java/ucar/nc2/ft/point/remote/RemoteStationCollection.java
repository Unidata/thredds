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
      PointStreamProto.StationList pfc = PointStreamProto.StationList.parseFrom(b);
      stationHelper.addStation(new StationImpl());

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

  private class RemoteStationFeatureImpl extends StationFeatureImpl {

    RemoteStationFeatureImpl(Station s) {
      super(s, dateUnit, -1);

            HttpMethod method = null;

      try {
        method = ncremote.sendQuery(makeRequest());
        InputStream in = method.getResponseBodyAsStream();

        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(b);
        PointFeatureIterator iter = new RemotePointFeatureIterator(pfc, method, in);
        iter.setCalculateBounds(this);
        return iter;

      } finally {
        if (method != null) method.releaseConnection();
      }
    }

    // an iterator over the observations for this station
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.recnum[1] = recnum;
      cursor.tableData[1] = stationData;
      cursor.parentIndex = 1; // LOOK ?
      StructureDataIterator obsIter = ft.getStationObsDataIterator(cursor, bufferSize);
      return new RemoteStationPointIterator((size() < 0) ? this : null, obsIter, cursor);
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
