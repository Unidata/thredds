// $Id: OldUnidataPointObsDataset.java,v 1.9 2006/06/06 16:07:14 caron Exp $
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

import ucar.nc2.*;
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
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */

public class OldUnidataPointObsDataset extends PointObsDatasetImpl {
  static private String latName = "lat";
  static private String lonName = "lon";
  static private String altName = "Depth";
  static private String timeName = "timeObs";

  static public boolean isValidFile(NetcdfFile ds) {
    Structure recordVar = (Structure) ds.findVariable("record");
    if (null == recordVar) return false;

    if (recordVar.findVariable(latName) == null) return false;
    if (recordVar.findVariable(lonName) == null) return false;
    if (recordVar.findVariable(altName) == null) return false;
    if (recordVar.findVariable(timeName) == null) return false;

    return true;
  }

  private RecordDatasetHelper recordHelper;
  private ArrayList records;

  public OldUnidataPointObsDataset(NetcdfFile ds) throws IOException {
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