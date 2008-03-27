/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dt2.point;

import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dt2.*;
import ucar.nc2.dt2.coordsys.CoordSysAnalyzer;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.ma2.*;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.io.IOException;
import java.text.ParseException;

/**
 * Default handler for PointFeatureDataset
 *
 * @author caron
 */
public class PointDatasetDefaultFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetDefaultFactory.class);

  // FeatureDatasetFactory
  public boolean isMine(NetcdfDataset ds) {

    boolean hasTime = false;
    boolean hasLat = false;
    boolean hasLon = false;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if ((axis.getAxisType() == AxisType.Time) && (axis.getRank() == 1))
        hasTime = true;
      if ((axis.getAxisType() == AxisType.Lat) && (axis.getRank() == 1))
        hasLat = true;
      if ((axis.getAxisType() == AxisType.Lon) && (axis.getRank() == 1))
        hasLon = true;
    }

    return hasTime && hasLon && hasLat;
  }

  public FeatureDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    CoordSysAnalyzer analyser = new CoordSysAnalyzer(ncd);
    return new PointDatasetDefault(analyser, ncd, errlog);
  }

  public FeatureType getFeatureDataType() {
    return FeatureType.POINT;
  }

  /////////////////////////////////////////////////////////////////////
  private static class PointDatasetDefault extends PointDatasetImpl {

    private DateFormatter formatter;
    private DateUnit timeUnit;
    private boolean needBB = true;

    PointDatasetDefault(CoordSysAnalyzer analyser, NetcdfDataset ds, StringBuffer errlog) throws IOException {
      super(ds, PointFeature.class);
      parseInfo.append(" PointFeatureDatasetImpl=").append(getClass().getName()).append("\n");

      List<FeatureCollection> featureCollections = new ArrayList<FeatureCollection>();
      for (CoordSysAnalyzer.FlatTable flatTable : analyser.getTables()) {

        if (timeUnit == null) {
          try {
            timeUnit = flatTable.getTimeUnit();
          } catch (Exception e) {
            if (null != errlog) errlog.append(e.getMessage()).append("\n");
            timeUnit = (DateUnit) SimpleUnit.factory("seconds since 1970-01-01");
          }
        }

        Structure obsStruct;
        Dimension obsDim = flatTable.getObsDimension();
        if (obsDim.isUnlimited()) {
          ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
          obsStruct = (Structure) ds.getRootGroup().findVariable("record");
        } else {
          obsStruct = new StructurePseudo(ds, null, "obs", obsDim);
        }

        // create member variables
        for (Variable v : flatTable.getDataVariables()) {
          dataVariables.add(v);
        }

        if (flatTable.getFeatureType() == FeatureType.STATION)
          featureCollections.add(new DefaultStationCollectionImpl(ds, flatTable, obsStruct));
        else
          featureCollections.add(new DefaultPointCollectionImpl(flatTable, obsStruct));
      }

      setPointFeatureCollection(featureCollections);
    }

    // calculate bounding box, date range when all the data is iterated
    private LatLonRect calcBB = null;
    private double minTime = Double.MAX_VALUE;
    private double maxTime = -Double.MAX_VALUE;

    private void startCalcBB() {
      calcBB = null;
    }

    private void calcBB(PointFeature pf) {
      if (calcBB == null)
        calcBB = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
      else
        calcBB.extend(pf.getLocation().getLatLon());

      double obsTime = pf.getObservationTime();
      minTime = Math.min(minTime, obsTime);
      maxTime = Math.max(maxTime, obsTime);
    }

    private void finishCalcBB() {
      if (calcBB.crossDateline() && calcBB.getWidth() > 350.0) { // call it global - less confusing
        double lat_min = calcBB.getLowerLeftPoint().getLatitude();
        double deltaLat = calcBB.getUpperLeftPoint().getLatitude() - lat_min;
        boundingBox = new LatLonRect(new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
      } else {
        boundingBox = calcBB;
      }

      dateRange = new DateRange(timeUnit.makeDate(minTime), timeUnit.makeDate(maxTime));
      needBB = false;
    }

    //////////////////////////////////////////////////////////////////////////////
    // various utilities

    private StructureMembers.Member timeMember, latMember, lonMember, altMember;
    private StructureMembers.Member stnNameMember, stnDescMember;

    public double getTime(CoordSysAnalyzer.FlatTable ft, StructureData obsData) {
      if (timeMember == null) {
        StructureMembers members = obsData.getStructureMembers();
        timeMember = members.findMember(ft.timeAxis.getShortName());
      }

      if ((timeMember.getDataType() == ucar.ma2.DataType.CHAR) || (timeMember.getDataType() == ucar.ma2.DataType.STRING)) {
        String time = obsData.getScalarString(timeMember);
        if (null == formatter) formatter = new DateFormatter();
        Date date;
        try {
          date = formatter.isoDateTimeFormat(time);
        } catch (ParseException e) {
          log.error("Cant parse date - not ISO formatted, = " + time);
          return 0.0;
        }
        return date.getTime() / 1000.0;

      } else {
        return obsData.convertScalarDouble(timeMember);
      }
    }

    public EarthLocation getEarthLocation(CoordSysAnalyzer.FlatTable ft, StructureData obsData) {
      if (latMember == null) {
        StructureMembers members = obsData.getStructureMembers();
        latMember = members.findMember(ft.latAxis.getShortName());
        lonMember = members.findMember(ft.lonAxis.getShortName());
        altMember = (ft.heightAxis == null) ? null : members.findMember(ft.heightAxis.getShortName());
      }

      double lat = obsData.convertScalarDouble(latMember);
      double lon = obsData.convertScalarDouble(lonMember);
      double alt = (altMember == null) ? 0.0 : obsData.convertScalarDouble(altMember);

      return new EarthLocationImpl(lat, lon, alt);
    }

    //////////////////////////////////////////////////////////////////////////////
    // PointFeatureCollection

    private class DefaultPointCollectionImpl extends PointCollectionImpl {
      private CoordSysAnalyzer.FlatTable flatTable;
      private Structure obsStruct;

      DefaultPointCollectionImpl(CoordSysAnalyzer.FlatTable flatTable, Structure obsStruct) {
        super("DefaultPointCollectionImpl-" + obsStruct.getName());
        this.flatTable = flatTable;
        this.obsStruct = obsStruct;
      }

      public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        return new DefaultPointFeatureIterator();
      }

      // the iterator over the observations
      private class DefaultPointFeatureIterator extends StructureDataIterator {
        DefaultPointFeatureIterator() throws IOException {
          super(obsStruct, -1, null);
          if (needBB) startCalcBB();
        }

        protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
          PointFeature result = new MyPointFeature(recnum, flatTable, sdata);

          // try to calculate bounding box, date range when data is fully iterated
          if (needBB) calcBB(result);

          return result;
        }

        // decorate hasNext to know when the iteraton is complete
        public boolean hasNext() throws IOException {
          boolean r = super.hasNext();
          if (needBB && !r) finishCalcBB();
          return r;
        }
      }

    }

    // a PointFeature that can be constructed from the observation StructureData
    private class MyPointFeature extends PointFeatureImpl {
      protected int recno;
      protected StructureData sdata;

      public MyPointFeature(int recnum, CoordSysAnalyzer.FlatTable flatTable, StructureData obsData) {
        super(PointDatasetDefault.this.timeUnit);
        this.sdata = obsData;
        this.recno = recnum;

        obsTime = getTime(flatTable, obsData);
        nomTime = obsTime;
        location = getEarthLocation(flatTable, obsData);
      }

      public String getId() {
        return Integer.toString(recno);
      }

      public StructureData getData() {
        return sdata;
      }
    }

    //////////////////////////////////////////////////////////////////////////////
    // StationFeatureCollection

    private class DefaultStationCollectionImpl extends StationCollectionImpl {
      private CoordSysAnalyzer.FlatTable flatTable;
      private Structure obsStruct, stationStruct;

      DefaultStationCollectionImpl(NetcdfDataset ds, CoordSysAnalyzer.FlatTable flatTable, Structure obsStruct) throws IOException {
        super("DefaultStationCollectionImpl-" + obsStruct.getName());
        this.flatTable = flatTable;
        this.obsStruct = obsStruct;

        Dimension stationDim = flatTable.getStationDimension();
        if (stationDim.isUnlimited()) {
          ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
          stationStruct = (Structure) ds.getRootGroup().findVariable("record");  // ??
        } else {
          stationStruct = new StructurePseudo(ds, null, "station", stationDim);
        }

        // LOOK can we defer StationHelper ?
        ucar.ma2.StructureDataIterator siter = stationStruct.getStructureIterator();
        while (siter.hasNext()) {
          StructureData stationData = siter.next();
          stationHelper.addStation(makeStation(flatTable, stationData));
        }
      }

      public Station makeStation(CoordSysAnalyzer.FlatTable ft, StructureData stationData) {
        if (latMember == null) {
          StructureMembers members = stationData.getStructureMembers();
          latMember = members.findMember(ft.latAxis.getShortName());
          lonMember = members.findMember(ft.lonAxis.getShortName());
          altMember = (ft.heightAxis == null) ? null : members.findMember(ft.heightAxis.getShortName());

          // ha ha
          stnNameMember = members.findMember("station_id");
          stnDescMember = members.findMember("station_desc");
        }

        String stationName = stationData.getScalarString(stnNameMember);
        String stationDesc = (stnDescMember == null) ? stationName : stationData.getScalarString(stnDescMember);
        double lat = stationData.convertScalarDouble(latMember);
        double lon = stationData.convertScalarDouble(lonMember);
        double alt = (altMember == null) ? 0.0 : stationData.convertScalarDouble(altMember);

        return new LinkedStationFeatureImpl(stationName, stationDesc, lat, lon, alt, timeUnit, ft, stationData);
      }

      public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
        return new StationListIterator();
      }

      private class StationListIterator implements PointFeatureCollectionIterator {
        Iterator<Station> stationIter;

        StationListIterator() {
          stationIter = stationHelper.getStations().iterator();
        }

        public boolean hasNext() throws IOException {
          return stationIter.hasNext();
        }

        public PointFeatureCollection nextFeature() throws IOException {
          return (StationFeatureImpl) stationIter.next();
        }

        public void setBufferSize(int bytes) {
          // no op
        }
      }

      private class LinkedStationFeatureImpl extends StationFeatureImpl {
        int firstRecno;
        String linkVarName;

        LinkedStationFeatureImpl(String name, String desc, double lat, double lon, double alt, DateUnit dateUnit,
                                 CoordSysAnalyzer.FlatTable ft, StructureData stationData) {
          super(name, desc, lat, lon, alt, dateUnit, -1);

          firstRecno = stationData.getScalarInt( ft.getLinkedFirstRecVarName());
          linkVarName = ft.getLinkedNextRecVarName();
        }

        // an iterator over Features of type PointFeature
        public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
          return new LinkedStationFeatureIterator((getNumberPoints() < 0) ? this : null, firstRecno, linkVarName);
        }
      }

      // the iterator over the observations
      private class LinkedStationFeatureIterator extends StructureDataLinkedIterator {
        StationFeatureImpl station;
        int npts = 0;

        LinkedStationFeatureIterator(StationFeatureImpl station, int firstRecord, String linkVarName) throws IOException {
          super(obsStruct, firstRecord, -1, linkVarName, null);
          this.station = station;
        }

        protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
          return new MyPointFeature(recnum, flatTable, sdata);
        }

        // decorate to capture npts
        public boolean hasNext() throws IOException {
          boolean result = super.hasNext();

          if (!result && (station != null))
            station.setNumberPoints( npts);

          npts++;
          return result;
        }
      }

    }

  }
}
