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

package ucar.nc2.dt.point;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;

import java.io.*;
import java.util.*;

/**
 * National Data Buoy Center data. This is a time series at a single buoy.
 *
 * <p> This is a single station of data. Assumed to be time ordered.
 * We construct the list of StationObsDatatype records, but without the data cached.
 *
 * @see <a href="http://www.ndbc.noaa.gov/index.shtml">http://www.ndbc.noaa.gov/index.shtml</a>
 *
 * @author caron
 */

//  LOOK when from a dods server, record structure not there. Havent dealt with that yet.
public class NdbcDataset extends StationObsDatasetImpl  implements TypedDatasetFactoryIF {
  private ArrayInt.D1 dates;
  private RecordDatasetHelper recordHelper;
  private StationImpl station;

  static public boolean isValidFile(NetcdfFile ds) {
    if (!ds.findAttValueIgnoreCase(null, "Conventions", "").equalsIgnoreCase("COARDS")) return false;
    if (!ds.findAttValueIgnoreCase(null, "data_provider", "").equalsIgnoreCase("National Data Buoy Center")) return false;

    if (null == ds.findAttValueIgnoreCase(null, "station", null)) return false;
    if (null == ds.findAttValueIgnoreCase(null, "location", null)) return false;

    if (ds.findVariable("lat") == null) return false;
    if (ds.findVariable("lon") == null) return false;

    // DODS wont have record !!
    if (!ds.hasUnlimitedDimension()) return false;

    return true;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new NdbcDataset( ncd);
  }
  
  public NdbcDataset() {}

  public NdbcDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    recordHelper = new RecordDatasetHelper(ds, "time", null, dataVariables);
    removeDataVariable("time");
    timeUnit = recordHelper.timeUnit;

    Variable latVar = ds.findVariable("lat");
    double lat = latVar.readScalarDouble();
    Variable lonVar = ds.findVariable("lon");
    double lon = lonVar.readScalarDouble();

    // LOOK assume its time ordered
    Variable dateVar = ds.findVariable("time");
    dates = (ArrayInt.D1) dateVar.read();
    int count = (int) dates.getSize();
    int firstDate = dates.get(0);
    int lastDate = dates.get( count-1);

    startDate = timeUnit.makeDate( (double) firstDate);
    endDate = timeUnit.makeDate( (double) lastDate);

    String name = ds.findAttValueIgnoreCase(null, "station", null);
    String stationDesc = ds.findAttValueIgnoreCase(null, "description", null);

    // only one station in the file
    station = new StationImpl( name, stationDesc, lat, lon,  Double.NaN, count);
    stations.add( station);

    // typed dataset fields
    title = ds.findAttValueIgnoreCase(null, "data_provider", null) +" Station "+name;
    desc = title +"\n" + ds.findAttValueIgnoreCase(null, "data_quality", null);

    setBoundingBox();
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}
  protected void setBoundingBox() { boundingBox = stationHelper.getBoundingBox(); }

  public List getData(CancelTask cancel) throws IOException {
    return getData( station, cancel);
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData( Station s, CancelTask cancel) throws IOException {
    StationImpl si = (StationImpl) s;
    int count = getDataCount();

    if (null == si.getObservations()) {
      for (int recno = 0; recno < count; recno++) {
        double time = dates.get(recno);
        si.addObs( recordHelper.new RecordStationObs( s, time, time, recno));
        if ((cancel != null) && cancel.isCancel()) return null;
      }
    }

    return si.getObservations();
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new NdbcDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class NdbcDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordStationObs( station, sdata);
    }

    NdbcDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }

  /* public void initNonRecordCase(NetcdfFile ds) throws IOException {
    this.ds = ds;

    Variable timeVar = ds.findVariable("record.timeObs");
    String timeUnitString = ds.findAttValueIgnoreCase(timeVar, "units", "seconds since 1970-01-01");
    try {
      dateUnit = (DateUnit) SimpleUnit.factoryWithExceptions(timeUnitString);
    } catch (Exception e) {
      System.out.println("Error on string = "+timeUnitString+" == "+e.getMessage());
      dateUnit = (DateUnit) SimpleUnit.factory("seconds since 1970-01-01");
    }

    // get the station info
    ArrayChar stationIdArray = (ArrayChar) ds.findVariable("id").read();
    ArrayChar stationDescArray = (ArrayChar) ds.findVariable("location").read();
    Array latArray = ds.findVariable("latitude").read();
    Array lonArray = ds.findVariable("longitude").read();
    Array elevArray = ds.findVariable("elevation").read();
    Array lastRecordArray = ds.findVariable("lastReport").read();
    Index ima = lastRecordArray.getIndex();

    int n = ds.findVariable("num_stations").readScalarInt();
    for (int i = 0; i<n; i++) {
      ima.set(i);

      String stationName = stationIdArray.getString(i).trim();
      String stationDesc = stationDescArray.getString(i).trim();

      StationImpl bean = new StationImpl(stationName, stationDesc,
          latArray.getFloat(ima),
          lonArray.getFloat(ima),
          elevArray.getFloat(ima),
          lastRecordArray.getInt(ima));

      stationHash.put(stationName, bean);
      stations.add(bean);
      // System.out.println(" read " + stationName);
    }
    // System.out.println("total stations " + n);

    // get the record info
    recordVar = (Structure) ds.findVariable("record");
    ucar.nc2.Dimension nrecs = ds.findDimension("recNum");
    int nobs = nrecs.getLength();

    try {
      StructureData data = recordVar.readStructure(0);
      startDate = data.getScalarInt("time_observation");
      data = data = recordVar.readStructure(nobs-1);
      endDate = data.getScalarInt("time_observation");
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    }

    stationHelper = new StationDatasetHelper( this);
  } */
}