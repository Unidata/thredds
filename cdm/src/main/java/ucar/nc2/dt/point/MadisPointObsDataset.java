// $Id: $
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

import ucar.nc2.dt.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class MadisPointObsDataset extends PointObsDatasetImpl  implements TypedDatasetFactoryIF {
  private Structure recordVar;
  private RecordDatasetHelper recordHelper;
  private String obsTimeVName, nomTimeVName, stnIdVName, stnDescVName, altVName;

  private boolean debug = false, debugLinks = false;

  static public boolean isValidFile(NetcdfFile ds) {
    if (ds.findVariable("staticIds") == null) return false;
    if (ds.findVariable("nStaticIds") == null) return false;
    if (ds.findVariable("lastRecord") == null) return false;
    if (ds.findVariable("prevRecord") == null) return false;
    if (ds.findVariable("latitude") == null) return false;
    if (ds.findVariable("longitude") == null) return false;

    if (!ds.hasUnlimitedDimension()) return false;
    if (ds.findGlobalAttribute("timeVariables") == null) return false;
    if (ds.findGlobalAttribute("idVariables") == null) return false;

    Attribute att = ds.findGlobalAttribute("title");
    if ((att != null) && att.getStringValue().equals("MADIS ACARS data")) return false;

    return true;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new MadisStationObsDataset( ncd);
  }
  public MadisPointObsDataset() {}

  public MadisPointObsDataset(NetcdfFile ds) throws IOException {
    super(ds);

    // dork around with the variable names
    altVName = "elevation";

    String timeNames = ds.findAttValueIgnoreCase(null, "timeVariables", null);
    StringTokenizer stoker = new StringTokenizer( timeNames,", ");
    obsTimeVName = stoker.nextToken();

    if (ds.findVariable("timeNominal") != null)
      nomTimeVName = "timeNominal";

    String idNames = ds.findAttValueIgnoreCase(null, "idVariables", null);
    stoker = new StringTokenizer( idNames,", ");
    stnIdVName = stoker.nextToken();

    if (stnIdVName.equals("stationName")) { // metars, sao, maritime
      if (ds.findVariable("locationName") != null)
        stnDescVName = "locationName";
      if (debug) System.out.println("filetype 1 (metars)");

    } else if (stnIdVName.equals("latitude")) { // acars LOOK this is not station - move to trajectory !!
      if (ds.findVariable("en_tailNumber") != null)
        stnIdVName = "en_tailNumber";
      altVName = "altitude";
      if (debug) System.out.println("filetype 3 (acars)");

    } else {  // coop, hydro, mesonet, radiometer
      if (ds.findVariable("stationId") != null)
        stnIdVName = "stationId";
      if (ds.findVariable("stationName") != null)
        stnDescVName = "stationName";
      if (debug) System.out.println("filetype 2 (mesonet)");
    }
    if (debug) System.out.println("title= "+ncfile.findAttValueIgnoreCase(null, "title", null));

    recordHelper = new RecordDatasetHelper(ds, obsTimeVName, nomTimeVName, dataVariables, parseInfo);
    removeDataVariable("prevRecord");

    timeUnit = recordHelper.timeUnit;

    // construct the stations
    Variable lastRecordVar = ds.findVariable("lastRecord");
    ArrayInt.D1 lastRecord = (ArrayInt.D1) lastRecordVar.read();

    Variable inventoryVar = ds.findVariable("inventory");
    ArrayInt.D1 inventoryData = (ArrayInt.D1) inventoryVar.read();

    Variable v = ds.findVariable("nStaticIds");
    int n = v.readScalarInt();

    recordHelper.stnHash = new HashMap(2*n);
    recordVar = (Structure) ds.findVariable("record");
    for (int stnIndex=0; stnIndex < n; stnIndex++) {
      int lastValue = lastRecord.get(stnIndex);
      int inventory = inventoryData.get(stnIndex);

      if (lastValue < 0)
        continue;

      // get an obs record for this, and extract the station info
      StructureData sdata = null;
      try {
        sdata = recordVar.readStructure( lastValue);
      } catch (InvalidRangeException e) {
        parseInfo.append("Invalid lastValue="+lastValue+" for station at index "+stnIndex+"\n");
        continue;
      }

      String stationId = sdata.getScalarString( stnIdVName).trim();
      String stationDesc = (stnDescVName == null) ? null : sdata.getScalarString( stnDescVName);
    }
    //if (debug) System.out.println("total stations " + stations.size()+" should be = "+n);

    // get min, max date
    Variable timeVar = ds.findVariable(obsTimeVName);
    Array timeData = timeVar.read();
    MAMath.MinMax minmax = MAMath.getMinMax( timeData);

    startDate = timeUnit.makeDate( minmax.min);
    endDate = timeUnit.makeDate( minmax.max);

    setBoundingBox();
  }

  protected void setTimeUnits() {}
  protected void setStartDate() {}
  protected void setEndDate() {}
  protected void setBoundingBox() {
    // boundingBox = stationHelper.getBoundingBox();
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

  protected PointObsDatatype makeObs(int recno) throws IOException {
    try {
      StructureData sdata = recordVar.readStructure(recno);
      float obsTime = sdata.getScalarFloat(obsTimeVName);
      float nomTime = (nomTimeVName == null) ? obsTime : sdata.getScalarFloat(nomTimeVName);

      float lat = sdata.getScalarFloat("latitude");
      float lon = sdata.getScalarFloat("longitude");
      float alt = sdata.getScalarFloat(altVName);

      return recordHelper.new RecordPointObs( new EarthLocationImpl(lat, lon, alt), obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException( e.getMessage());
    }
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /* private class MadisStationImpl extends StationImpl {
    private int lastRecord;
    private ArrayList lastRecords;

    private MadisStationImpl( String name, String desc, double lat, double lon, double alt,
                              int lastRecord, int inventory) {
      super( name, desc, lat, lon, alt);
      this.lastRecord = lastRecord;
      this.count = count( inventory);
    }

    // extra linked lists
    private void addLinkedList(int lastRecord, int inventory ) {
      if (lastRecords == null)
        lastRecords = new ArrayList();
      lastRecords.add( new Integer(lastRecord));
      this.count += count( inventory);
    }

    private int count( int inventory) {
      int cnt = 0;
      for (int bitno=0; bitno < 24; bitno++) {
        cnt += (inventory & 1);
        inventory = inventory >> 1;
      }
      return cnt;
    }

    protected ArrayList readObservations() throws IOException {
      ArrayList result = new ArrayList();
      readLinkedList( this, result, lastRecord);

      if (lastRecords == null)
        return result;

      // extra linked lists
      for (int i = 0; i < lastRecords.size(); i++) {
        Integer lastLink = (Integer) lastRecords.get(i);
        readLinkedList( this, result, lastLink.intValue());
      }

      Collections.sort( result);
      return result;
  }

    protected void readLinkedList(StationImpl s, ArrayList result, int lastLink) throws IOException {
      int recnum = lastLink;
      while (recnum >= 0) {
        try {
          StructureData sdata = recordVar.readStructure(recnum);
          int prevRecord = sdata.getScalarInt("prevRecord");
          float obsTime = sdata.getScalarFloat(obsTimeVName);
          float nomTime = (nomTimeVName == null) ? obsTime : sdata.getScalarFloat(nomTimeVName);
          result.add( 0, recordHelper.new RecordStationObs(s, obsTime, nomTime, recnum));
          recnum = prevRecord;
        } catch (ucar.ma2.InvalidRangeException e) {
          e.printStackTrace();
          throw new IOException(e.getMessage());
        }
      }
    }
  } */

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new MadisDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class MadisDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordStationObs( recnum, sdata);
    }

    MadisDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }
}
