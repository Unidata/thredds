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

package ucar.nc2.dt.point;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;

import java.io.*;
import java.util.*;

/**
 * This reads MADIS station data formatted files. It might actually be ok for any AWIPS station file ??
 *
 * <p> We construct the list of StationObsDatatype records, but without the data cached.
 * <p>
 * There are a few problems with this format from an efficiency POV:
 * <ol><li> the station lat, lon, and desc are in the record variable, forcing us to read one obd data for
 *  each station at open.
 * <li> time units are 'seconds since 1-1-1970' instead of 'seconds since 1970-01-01' (not udunit compatible)
 *  this is being corrected.
 * </ol>
 *
 * @author caron
 */

public class MadisStationObsDataset extends StationObsDatasetImpl  implements TypedDatasetFactoryIF {
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
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new MadisStationObsDataset( ncd);
  }
  
  public MadisStationObsDataset() {}

  public MadisStationObsDataset(NetcdfDataset ds) throws IOException {
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
    recordHelper.setStationInfo(stnIdVName, stnDescVName);
    removeDataVariable(obsTimeVName);
    removeDataVariable(nomTimeVName);
    removeDataVariable("latitude");
    removeDataVariable("longitude");
    removeDataVariable(altVName);
    removeDataVariable("prevRecord");
    removeDataVariable(stnIdVName);
    removeDataVariable(stnDescVName);

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
      float lat = sdata.convertScalarFloat("latitude");
      float lon = sdata.convertScalarFloat("longitude");
      float alt = sdata.convertScalarFloat(altVName);

      MadisStationImpl stn = (MadisStationImpl) recordHelper.stnHash.get(stationId);
      if (stn != null) {
        if (debugLinks) {
          parseInfo.append("Already have="+stationId+" for station at index "+lastValue+" lastRecord= "+stn.lastRecord+"\n");
          parseInfo.append("  old lat="+stn.getLatitude()+" lon= "+stn.getLongitude()+" alt= "+stn.getAltitude()+"\n");
          parseInfo.append("  new lat="+lat+" lon= "+lon+" alt= "+alt+"\n");
        }
        stn.addLinkedList( lastValue, inventory);
        continue;
      } 

      MadisStationImpl station = new MadisStationImpl(stationId, stationDesc, lat, lon, alt, lastValue, inventory);
      recordHelper.stnHash.put(stationId, station);
      stations.add( station);
    }
    if (debug) System.out.println("total stations " + stations.size()+" should be = "+n);

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

  protected StationObsDatatype makeObs(int recno) throws IOException {
    try {
      StructureData sdata = recordVar.readStructure(recno);

      String stationId = sdata.getScalarString(stnIdVName);
      ucar.unidata.geoloc.Station s = recordHelper.stnHash.get(stationId);

      float obsTime = sdata.convertScalarFloat(obsTimeVName);
      float nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarFloat(nomTimeVName);

      return recordHelper.new RecordStationObs( s, obsTime, nomTime, recno);

    } catch (ucar.ma2.InvalidRangeException e) {
      e.printStackTrace();
      throw new IOException( e.getMessage());
    }
  }

  public List getData( ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return ((MadisStationImpl)s).getObservations();
  }

  public int getDataCount() {
    Dimension unlimitedDim = ncfile.getUnlimitedDimension();
    return unlimitedDim.getLength();
  }

  private class MadisStationImpl extends StationImpl {
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
          float obsTime = sdata.convertScalarFloat(obsTimeVName);
          float nomTime = (nomTimeVName == null) ? obsTime : sdata.convertScalarFloat(nomTimeVName);
          result.add( 0, recordHelper.new RecordStationObs(s, obsTime, nomTime, recnum));
          recnum = prevRecord;
        } catch (ucar.ma2.InvalidRangeException e) {
          e.printStackTrace();
          throw new IOException(e.getMessage());
        }
      }
    }
  }

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