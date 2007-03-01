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
import ucar.nc2.units.DateFormatter;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.IOException;

import thredds.catalog.*;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class StationObsDatasetWriter {
  private static final String recordDimName = "record";
  private static final String stationDimName = "station";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";
  private static final String idName = "name";
  private static final String descName = "description";
  private static final String wmoName = "wmo_id";
  private static final String timeStrLenDim = "time_strlen";

  DateFormatter dateFormatter = new DateFormatter();

  public void write(StationObsDataset sobsDataset, String fileOut) throws IOException {
    HashSet dimSet = new HashSet();
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(fileOut);
           
    List stnList = sobsDataset.getStations();
    int nstns = stnList.size();

    // global attributes
    List gatts = sobsDataset.getGlobalAttributes();
    for (int i = 0; i < gatts.size(); i++) {
      Attribute att = (Attribute) gatts.get(i);
      ncfile.addGlobalAttribute(att);
    }

    ncfile.addGlobalAttribute("Conventions", "Unidata Observation Dataset v1.0");
    ncfile.addGlobalAttribute("cdm_datatype", "Station");
    ncfile.addGlobalAttribute("observationDimension", recordDimName);
    ncfile.addGlobalAttribute("stationDimension", stationDimName);
    ncfile.addGlobalAttribute("latitude_coordinate", latName);
    ncfile.addGlobalAttribute("longitude_coordinate", lonName);
    ncfile.addGlobalAttribute("time_coordinate", timeName);

    LatLonRect llbb = sobsDataset.getBoundingBox();
    ncfile.addGlobalAttribute("geospatial_lat_min", Double.toString(llbb.getLowerLeftPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lat_max", Double.toString(llbb.getUpperRightPoint().getLatitude()));
    ncfile.addGlobalAttribute("geospatial_lon_min", Double.toString(llbb.getLowerLeftPoint().getLongitude()));
    ncfile.addGlobalAttribute("geospatial_lon_max", Double.toString(llbb.getUpperRightPoint().getLongitude()));

    ncfile.addGlobalAttribute("time_coverage_start", dateFormatter.toDateTimeStringISO( sobsDataset.getStartDate()));
    ncfile.addGlobalAttribute("time_coverage_end", dateFormatter.toDateTimeStringISO( sobsDataset.getEndDate()));

    // see if theres a altitude, wmoId
    boolean useAlt = false;
    boolean useWmoId = false;
    for (int i = 0; i < stnList.size(); i++) {
      Station stn = (Station) stnList.get(i);
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0)) {
        useWmoId = true;
        break;
      }
    }
    if (useAlt)
      ncfile.addGlobalAttribute("altitude_coordinate", altName);

    // add the dimensions
    ncfile.addUnlimitedDimension(recordDimName);
    ncfile.addDimension(stationDimName, nstns);
    ncfile.addDimension(timeStrLenDim, 20);

    List dataVars = sobsDataset.getDataVariables();
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF var = (VariableSimpleIF) dataVars.get(i);
      List dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    Iterator iter = dimSet.iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (!d.isUnlimited())
        ncfile.addDimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }

    // add the station Variables using the station dimension
    Variable v = ncfile.addVariable(latName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_north"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station latitude"));

    v = ncfile.addVariable(lonName, DataType.DOUBLE, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_east"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station longitude"));

    if (useAlt) {
      v = ncfile.addVariable(altName, DataType.DOUBLE, stationDimName);
      ncfile.addVariableAttribute(v, new Attribute("units", "meters"));
      ncfile.addVariableAttribute(v, new Attribute("long_name", "station altitude"));
    }

    v = ncfile.addVariable(idName, DataType.STRING, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station identifier"));

    v = ncfile.addVariable(descName, DataType.STRING, stationDimName);
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station description"));

    if (useWmoId) {
      v = ncfile.addVariable(wmoName, DataType.STRING, stationDimName);
      ncfile.addVariableAttribute(v, new Attribute("long_name", "station WMO id"));
    }

    // add the data variables all using the record dimension
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF oldVar = (VariableSimpleIF) dataVars.get(i);
      List dims = oldVar.getDimensions();
      StringBuffer dimNames = new StringBuffer(recordDimName);
      for (int j = 0; j < dims.size(); j++) {
        Dimension d = (Dimension) dims.get(j);
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getName(), oldVar.getDataType(), dimNames.toString());

      List atts = oldVar.getAttributes();
      for (int j = 0; j < atts.size(); j++) {
        Attribute att = (Attribute) atts.get(j);
        ncfile.addVariableAttribute(newVar, att);
      }
    }

    // time variable
    Variable timeVar = ncfile.addVariable(timeName, DataType.CHAR, recordDimName+" "+timeStrLenDim);
    ncfile.addVariableAttribute(timeVar, new Attribute("long_name", "ISO-8601 Date"));    

    // done with define mode
    ncfile.create();

    // now write the station data
    ArrayDouble.D1 latArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 lonArray = new ArrayDouble.D1(nstns);
    ArrayDouble.D1 altArray = new ArrayDouble.D1(nstns);
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 descArray = new ArrayObject.D1(String.class, nstns);
    ArrayObject.D1 wmoArray = new ArrayObject.D1(String.class, nstns);

    for (int i = 0; i < stnList.size(); i++) {
      Station stn = (Station) stnList.get(i);

      latArray.set(i, stn.getLatitude());
      lonArray.set(i, stn.getLongitude());
      if (useAlt) altArray.set(i, stn.getAltitude());

      idArray.set(i, stn.getName());
      descArray.set(i, stn.getDescription());
      if (useWmoId) wmoArray.set(i, stn.getWmoId());
    }

    try {
      ncfile.write(latName, latArray);
      ncfile.write(lonName, lonArray);
      if (useAlt) ncfile.write(altName, altArray);
      ncfile.write(idName, idArray);
      ncfile.write(descName, descArray);
      if (useWmoId) ncfile.write(wmoName, wmoArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    // now write the observations
    if (!ncfile.addRecordStructure())
      throw new IllegalStateException("can't add record variable");

    int[] origin = new int[1];
    int[] originTime = new int[2];
    int recno = 0;
    ArrayStructureW sArray = null;
    ArrayObject.D1 timeArray = new ArrayObject.D1( String.class, 1);

    DataIterator diter = sobsDataset.getDataIterator(1000 * 1000);
    while (diter.hasNext()) {
      StationObsDatatype sobs = (StationObsDatatype) diter.nextData();
      StructureData recordData = sobs.getData();

      // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
      if (sArray == null)
        sArray = new ArrayStructureW(recordData.getStructureMembers(), new int[]{1});
      sArray.setStructureData(recordData, 0);

      // date is handled specially
      timeArray.set(0, dateFormatter.toDateTimeStringISO( sobs.getObservationTimeAsDate()));

      // write the recno record
      origin[0] = recno;
      originTime[0] = recno;
      try {
        ncfile.write("record", origin, sArray);
        ncfile.write(timeName, originTime, timeArray);

      } catch (InvalidRangeException e) {
        e.printStackTrace();
        throw new IllegalStateException(e);
      }
      recno++;
    }

    ncfile.close();
  }

  public static void main(String args[]) throws IOException {

    String location = "C:/data/test/finalMonth2003.nc";
    StringBuffer errlog = new StringBuffer();
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.STATION, location, null, errlog);

    StationObsDatasetWriter writer = new StationObsDatasetWriter();
    String fileOut = "C:/temp/sobs.nc";
    writer.write(sobs, fileOut);
  }

}
