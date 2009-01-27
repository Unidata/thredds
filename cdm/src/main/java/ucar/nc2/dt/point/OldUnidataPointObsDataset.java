// $Id: OldUnidataPointObsDataset.java 51 2006-07-12 17:13:13Z caron $
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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;
import java.io.IOException;

/**
 * This handles datasets in an old format. It needs a record dimension, and record variables "lat", "lon", "Depth"
 *  and "timeObs".
 * <ul>
 * <li> The timeObs variable must have a valid udunit date unit.
 * <li> Lat, Lon are in decimal degreees north and east.
 * <li> Depth is optional and must be in meters above msl.
 * </ul>
 *
 * <p>
 * Since there is no other way to find what the stations are, or to find what data belongs to what
 *  station, we read through the entire dataset when starting.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */

public class OldUnidataPointObsDataset extends PointObsDatasetImpl implements TypedDatasetFactoryIF {
  static private String latName = "lat";
  static private String lonName = "lon";
  static private String altName = "Depth";
  static private String timeName = "timeObs";

  static public boolean isValidFile(NetcdfFile ds) {
    if (!ds.hasUnlimitedDimension()) return false;
    if (ds.findVariable(latName) == null) return false;
    if (ds.findVariable(lonName) == null) return false;
    if (ds.findVariable(altName) == null) return false;
    if (ds.findVariable(timeName) == null) return false;

    return true;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new OldUnidataPointObsDataset( ncd);
  }

  public OldUnidataPointObsDataset() {}

  private RecordDatasetHelper recordHelper;
  private ArrayList records;

  public OldUnidataPointObsDataset(NetcdfDataset ds) throws IOException {
    super( ds);

    recordHelper = new RecordDatasetHelper(ds, timeName, timeName, dataVariables);
    recordHelper.setLocationInfo(latName, lonName, altName);
    records = recordHelper.readAllCreateObs( null);
    removeDataVariable(timeName);
    removeDataVariable(latName);
    removeDataVariable(lonName);
    removeDataVariable(altName);

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


  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new PointDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

  private class PointDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordPointObs( recnum, sdata);
    }

    PointDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }
}