/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.VariableSimpleImpl;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Write DSG CF-1.6 file from a Coverage Dataset
 *
 * @author caron
 * @since 7/8/2015
 */
public class CoverageAsPoint {
  private static final boolean debug = true;

  private CoverageDataset gcd;
  private List<VarData> varData;
  private SubsetParams subset;
  private LatLonPointImpl latLonPoint;
  private DateUnit dateUnit;

  private class VarData {
    Coverage cov;
    GeoReferencedArray array;

    public VarData(Coverage cov) throws IOException {
      this.cov = cov;
      try {
        this.array = cov.readData(subset);
        if (debug) System.out.printf(" Coverage %s data shape = %s%n", cov.getName(), Misc.showInts(array.getData().getShape()));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }
  }

  public CoverageAsPoint(CoverageDataset gcd, List<String> varNames, SubsetParams subset) throws IOException {
    this.gcd = gcd;
    this.subset = subset;

    latLonPoint = (LatLonPointImpl) subset.get(SubsetParams.latlonPoint);
    if (latLonPoint == null)
      throw new IllegalArgumentException("No latlon point");

    varData = new ArrayList<>(varNames.size());
    for (String varName : varNames) {
      Coverage cov = gcd.findCoverage(varName); // LOOK we should read all at once if possible
      if (cov != null) {
        varData.add(new VarData(cov));

        if (dateUnit == null) { // assume all have the same time unit, just use the first one
          CoverageCoordSys csys = cov.getCoordSys();
          CoverageCoordAxis timeAxis = csys.getTimeAxis();
          CalendarDateUnit cdUnit = timeAxis.getCalendarDateUnit();
          try {
            dateUnit = new DateUnit( cdUnit.getUdUnit());
          } catch (Exception e) {
            throw new IllegalArgumentException("Could not make DateUnit from "+cdUnit.getUdUnit());
          }
        }
      }
    }

  }

  public FeatureDatasetPoint asFeatureDatasetPoint() {
    // for the moment, assume a single station, no vert coord, single lat/lon
    return new CoverageAsFeatureDatasetPoint(FeatureType.STATION);
  }

  private class CoverageAsFeatureDatasetPoint extends ucar.nc2.ft.point.PointDatasetImpl {
    protected CoverageAsFeatureDatasetPoint(FeatureType featureType) {
      super(featureType);
      CoverageAsStationFeatureCollection fc = new CoverageAsStationFeatureCollection(gcd.getName()+" AsStationFeatureCollection", dateUnit, null);
      setPointFeatureCollection(fc);

      List<VariableSimpleIF> dataVars = new ArrayList<>();
      for (VarData vd : varData) {                    // String name, String desc, String units, DataType dt
        VariableSimpleIF simple = VariableSimpleImpl.makeScalar(vd.cov.getName(), vd.cov.getDescription(), vd.cov.getUnits(), vd.cov.getDataType());
        dataVars.add(simple);
      }
      this.dataVariables = dataVars;
    }
  }

  private class CoverageAsStationFeatureCollection extends StationTimeSeriesCollectionImpl {

    public CoverageAsStationFeatureCollection(String name, DateUnit dateUnit, String altUnits) {
      super(name, dateUnit, altUnits);
    }

    @Override
    protected StationHelper createStationHelper() throws IOException {
      StationHelper helper = new StationHelper();
      String name = String.format("GridPointAt[%s]", latLonPoint.toString(3));
      name = StringUtil2.replace(name.trim(), ' ', "_");
      helper.addStation(new MyStationFeature(name, name, null, latLonPoint.getLatitude(), latLonPoint.getLongitude(), 0.0, dateUnit, null, -1));
      return helper;
    }
  }

  private class MyStationFeature extends StationTimeSeriesFeatureImpl {

    public MyStationFeature(String name, String desc, String wmoId, double lat, double lon, double alt, DateUnit timeUnit, String altUnits, int npts) {
      // String name, String desc, String wmoId, double lat, double lon, double alt, DateUnit timeUnit, String altUnits, int npts
      super(name, desc, wmoId, lat, lon, alt, timeUnit, altUnits, npts);
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return StructureData.EMPTY;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      return new TimeseriesIterator();
    }

    private class VarIter {
      Coverage cov;
      GeoReferencedArray geoA;
      IndexIterator dataIter;

      public VarIter(Coverage cov, GeoReferencedArray array, IndexIterator dataIter) {
        this.cov = cov;
        this.geoA = array;
        this.dataIter = dataIter;
      }
    }

    private class TimeseriesIterator extends PointIteratorAbstract {
      int curr = 0;
      int nvalues;
      List<VarIter> varIters;
      CoverageCoordAxis1D timeAxis;

      TimeseriesIterator() {
        varIters = new ArrayList<>();
        for (VarData vd : varData) {
          Array data = vd.array.getData();
          if (debug) System.out.printf("%s shape=%s%n", vd.cov.getName(), Misc.showInts(data.getShape()));
          varIters.add( new VarIter( vd.cov, vd.array,  data.getIndexIterator()));
          nvalues = (int) data.getSize();

          if (timeAxis == null) { // assume they are all the same (!)
            CoverageCoordSys csys = vd.array.getCoordSysForData();
            timeAxis = (CoverageCoordAxis1D) csys.getTimeAxis();   // LOOK may not be right
          }
        }
      }

      @Override
      public boolean hasNext() {
        return curr < nvalues;
      }

      @Override
      public PointFeature next() {
        double obsTime = timeAxis.getCoord(curr);

        StructureDataScalar coords = new StructureDataScalar("Coords");
        for (VarIter vi : varIters) {
          coords.addMember(vi.cov.getName(), null, null, vi.cov.getDataType(), (Number) vi.dataIter.getObjectNext());
        }
        curr++;
        return new MyPointFeature(MyStationFeature.this, obsTime, 0.0, timeUnit, coords);
      }

      @Override
      public void close() {
        // ignore
      }

      @Override
      public void setBufferSize(int bytes) {
        // ignore
      }
    }
  }

  private class MyPointFeature extends PointFeatureImpl implements StationPointFeature {
    StationFeature stn;
    StructureData sdata;

    public MyPointFeature(StationFeature stn, double obsTime, double nomTime, DateUnit timeUnit, StructureData sdata) {
      super(stn, obsTime, nomTime, timeUnit);
      this.stn = stn;
      this.sdata = sdata;
    }

    @Override
    public StationFeature getStation() {
      return stn;
    }

    @Override
    public StructureData getFeatureData() throws IOException {
      return sdata;
    }

    @Override
    public StructureData getDataAll() throws IOException {
      return sdata;
    }
  }

}
