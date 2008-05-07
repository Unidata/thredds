// $Id: UnidataStationObsDataset2.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * This handles datasets in the original (now outdated) "Unidata Point Format".
 * @deprecated  use UnidataStationObsDataset instead of this.
 *
 * @author John Caron
 * @version $Id: UnidataStationObsDataset2.java 51 2006-07-12 17:13:13Z caron $
 */

public class UnidataStationObsDataset2 extends StationObsDatasetImpl implements TypedDatasetFactoryIF {

  static public boolean isValidFile(NetcdfFile ds) {
    return ds.findAttValueIgnoreCase(null, "Conventions", "").equalsIgnoreCase("Unidata Station Format v1.0");
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new UnidataStationObsDataset2( ncd);
  }

  public UnidataStationObsDataset2() {}

  private Structure recordVar;
  private RecordDatasetHelper recordHelper;
  private boolean debugRead = false;

  public UnidataStationObsDataset2(NetcdfDataset ds) throws IOException {
    super(ds);

    title = "Station Data from NWS";
    desc = "Station Data from NWS distributed through the Unidata IDD realtime datastream. " +
      "Decoded into netCDF files by metar2nc (new). Usually 1 hour of data";

    recordHelper = new RecordDatasetHelper(ds, "time_observation","time_nominal", dataVariables, parseInfo);
    recordHelper.setStationInfo( "station_index", "location");

    removeDataVariable("time_observation");
    removeDataVariable("time_nominal");
    removeDataVariable("previousReport");
    removeDataVariable("station_index");

    recordVar = recordHelper.recordVar;
    timeUnit = recordHelper.timeUnit;

    // get the station info
    ArrayChar stationIdArray = (ArrayChar) ds.findVariable("id").read();
    ArrayChar stationDescArray = (ArrayChar) ds.findVariable("location").read();
    Array latArray = ds.findVariable("latitude").read();
    Array lonArray = ds.findVariable("longitude").read();
    Array elevArray = ds.findVariable("elevation").read();
    Array lastRecordArray = ds.findVariable("lastReport").read();
    Array numReportsArray = ds.findVariable("numberReports").read();
    Index ima = lastRecordArray.getIndex();

    int n = ds.findVariable("number_stations").readScalarInt();
    recordHelper.stnHash = new HashMap( 2*n);
    for (int i = 0; i<n; i++) {
      ima.set(i);

      String stationName = stationIdArray.getString(i).trim();
      String stationDesc = stationDescArray.getString(i).trim();

      UnidataStationImpl bean = new UnidataStationImpl(stationName, stationDesc,
          latArray.getFloat(ima),
          lonArray.getFloat(ima),
          elevArray.getFloat(ima),
          lastRecordArray.getInt(ima),
          numReportsArray.getInt(ima)
      );

      stations.add(bean);
      recordHelper.stnHash.put(new Integer(i), bean);
    }
    setBoundingBox();

    // get min, max date
    Variable minTimeVar = ds.findVariable("minimum_time_observation");
    int minTimeValue = minTimeVar.readScalarInt();
    startDate = timeUnit.makeDate( minTimeValue);

    Variable maxTimeVar = ds.findVariable("maximum_time_observation");
    int maxTimeValue = maxTimeVar.readScalarInt();
    endDate = timeUnit.makeDate( maxTimeValue);
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}
  protected void setBoundingBox() {
    boundingBox = stationHelper.getBoundingBox();
  }

  public List getData(CancelTask cancel) throws IOException {
    ArrayList allData = new ArrayList();
    for (int i=0; i<getDataCount(); i++) {
      allData.add( makeObs(i));
      if ((cancel != null) && cancel.isCancel())
        return null;
    }
    return allData;
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData( Station s, CancelTask cancel) throws IOException {
    return ((UnidataStationImpl)s).getObservations();
  }

  private class UnidataStationImpl extends StationImpl {
    private int lastRecord;

    private UnidataStationImpl( String name, String desc, double lat, double lon, double elev, int lastRecord, int count) {
      super( name, desc, lat, lon, elev, count);
      this.lastRecord = lastRecord;
    }

    protected ArrayList readObservations() throws IOException {
      ArrayList obs = new ArrayList();
      int recno = lastRecord;

      while (recno >= 0) {
        try {
          if (debugRead) System.out.println(name + " try to read at record "+recno);
          StructureData sdata = recordVar.readStructure(recno);
          int prevRecord = sdata.getScalarInt("previousReport");
          float obsTime = sdata.convertScalarFloat("time_observation");
          float nomTime = sdata.convertScalarFloat("time_nominal");

          obs.add( 0, recordHelper.new RecordStationObs( this, obsTime, nomTime, recno));
          recno = prevRecord;
        }
        catch (ucar.ma2.InvalidRangeException e) {
          e.printStackTrace();
          throw new IOException( e.getMessage());
        }
      }

      return obs;
    }
  }

  protected StationObsDatatype makeObs(int recno) throws IOException {
    try {
      StructureData sdata = recordVar.readStructure(recno);

      int stationIndex = sdata.getScalarInt("station_index");
      Station station = (Station) stations.get(stationIndex);
      if (station == null)
        parseInfo.append("cant find station at index = "+stationIndex+"\n");

      float obsTime = sdata.convertScalarFloat("time_observation");
      float nomTime = sdata.convertScalarFloat("time_nominal");

      return recordHelper.new RecordStationObs( station, obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException( e.getMessage());
    }
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new StationDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class StationDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordStationObs( recnum, sdata);
    }

    StationDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }

}
