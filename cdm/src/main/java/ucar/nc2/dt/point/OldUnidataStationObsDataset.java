// $Id: OldUnidataStationObsDataset.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.ncml.NcMLReader;

import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.StructureData;

import java.io.*;
import java.util.*;

/**
 * This handles datasets in the old metar2nc format. It identifies them by looking at the title,
 *  expecting "METAR definition", "SYNOPTIC definition", or "BUOY definition". It uses an NcML
 *  file to identify the names of the lat, lon, etc variables.
 *
 * <p>
 * Since there is no other way to find what the stations are, or to find what data belongs to what
 *  station, we read through the entire dataset at open.
 * We construct the list of StationObsDatatype records, but without the data cached.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */

public class OldUnidataStationObsDataset extends StationObsDatasetImpl  implements TypedDatasetFactoryIF {

  static public boolean isValidFile(NetcdfFile ds) {
    String kind = ds.findAttValueIgnoreCase(null, "title", null);
    if (kind == null) return false;
    if ("METAR definition".equals( kind)) return true;
    if ("SYNOPTIC definition".equals( kind)) return true;
    if ("BUOY definition".equals( kind)) return true;

    return false;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new OldUnidataStationObsDataset( ncd);
  }
  
  public OldUnidataStationObsDataset() {}

  private NetcdfDataset dataset;
  private RecordDatasetHelper recordHelper;
  private ArrayList records;
  private String latName, lonName, elevName, descName, timeName, timeNominalName, stationIdName;

  public OldUnidataStationObsDataset(NetcdfDataset ds) throws IOException {
    super( ds);
    this.dataset = ds;

    String ncmlURL = null;

    String kind = ds.findAttValueIgnoreCase(null, "title", null);
    if ("METAR definition".equals(kind)) {
      Variable v = ds.findVariable("station");
      if (v != null)
        ncmlURL = CoordSysBuilder.resourcesDir+"metar2ncMetar.ncml";
      else  // older form
        ncmlURL = CoordSysBuilder.resourcesDir+"metar2ncMetar2.ncml";

    } else if ("SYNOPTIC definition".equals(kind)) {
      ncmlURL = CoordSysBuilder.resourcesDir+"metar2ncSynoptic.ncml";

    } else if ("BUOY definition".equals(kind)) {
      ncmlURL = CoordSysBuilder.resourcesDir+"metar2ncBuoy.ncml";
    }

    if (ncmlURL == null)
      throw new IOException("unknown StationObsDataset type "+ds.getLocation());

    init(ncmlURL);
  }

  public OldUnidataStationObsDataset(NetcdfDataset ds, String ncmlURL) throws IOException {
    super( ds);
    this.dataset = ds;

    init(ncmlURL);
  }

  protected void init(String ncmlURL) throws IOException {
    NcMLReader.wrapNcMLresource(dataset, ncmlURL, null);

    stationIdName = dataset.findAttValueIgnoreCase(null, "_StationIdVar", null);
    descName = dataset.findAttValueIgnoreCase(null, "_StationDescVar", null); // ok if null
    latName = dataset.findAttValueIgnoreCase(null, "_StationLatVar", null);
    lonName = dataset.findAttValueIgnoreCase(null, "_StationLonVar", null);
    elevName = dataset.findAttValueIgnoreCase(null, "_StationElevVar", null);
    timeName = dataset.findAttValueIgnoreCase(null, "_StationTimeVar", null);
    timeNominalName = dataset.findAttValueIgnoreCase(null, "_StationTimeNominalVar", null);

    recordHelper = new RecordDatasetHelper(dataset, timeName, timeNominalName, dataVariables);
    recordHelper.setStationInfo( stationIdName, descName);
    recordHelper.setLocationInfo( latName, lonName, elevName);

    removeDataVariable(latName);
    removeDataVariable(lonName);
    removeDataVariable(elevName);
    removeDataVariable(timeName);
    removeDataVariable(timeNominalName);

    records = recordHelper.readAllCreateObs( null);
    stations = new ArrayList(recordHelper.stnHash.values());

    setTimeUnits();
    setStartDate();
    setEndDate();
    setBoundingBox();
  }

  protected void setTimeUnits() { timeUnit = recordHelper.timeUnit;}
  protected void setStartDate() { startDate = timeUnit.makeDate( recordHelper.minDate);}
  protected void setEndDate() { endDate = timeUnit.makeDate( recordHelper.maxDate);}
  protected void setBoundingBox() { boundingBox = recordHelper.boundingBox;}

  public List getData(CancelTask cancel) throws IOException {
    return records;
  }

  public int getDataCount() {
    return records.size();
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return recordHelper.getData( records, boundingBox, cancel);
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return recordHelper.getData( records, boundingBox, startTime, endTime, cancel);
  }

  public int getStationDataCount(ucar.unidata.geoloc.Station s) {
    StationImpl si = (StationImpl) s;
    return si.getNumObservations();
  }

  public List getData( ucar.unidata.geoloc.Station s, CancelTask cancel) throws IOException {
    return ((StationImpl)s).getObservations();
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new StationDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class StationDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recno, StructureData sdata) {
      return recordHelper.new RecordStationObs( recno, sdata);
    }

    StationDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }
}