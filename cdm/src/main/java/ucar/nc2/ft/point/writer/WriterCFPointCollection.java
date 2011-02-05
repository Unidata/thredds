/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.point.writer;

import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.*;
import ucar.nc2.ft.*;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * Write a CF "Discrete Sample" point file.
 *
 * @author caron
 * @since Nov 23, 2010
 */
public class WriterCFPointCollection {
  private static final String recordDimName = "record";
  private static final String latName = "latitude";
  private static final String lonName = "longitude";
  private static final String altName = "altitude";
  private static final String timeName = "time";
  private static final boolean debug = false;

  private DateFormatter dateFormatter = new DateFormatter();

  private NetcdfFileWriteable ncfile;
  private String title;
  private String altUnits;

  private Set<Dimension> dimSet = new HashSet<Dimension>(20);
  private Date minDate = null;
  private Date maxDate = null;

  public WriterCFPointCollection(String fileOut, String title) throws IOException {
    ncfile = NetcdfFileWriteable.createNew(fileOut, false);
    ncfile.setFill( false);
    this.title = title;
  }

  public void setLength(long size) {
    ncfile.setLength( size);
  }

  public void writeHeader(List<VariableSimpleIF> vars, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;
    ncfile.addGlobalAttribute("Conventions", "CF-1.6");
    ncfile.addGlobalAttribute("featureType", "point");
    ncfile.addGlobalAttribute("title", title);
    ncfile.addGlobalAttribute("desc", "Written by TDS/CDM Remote Feature subset service");

    // dummys, update in finish()
    ncfile.addGlobalAttribute("time_coverage_start", dateFormatter.toDateTimeStringISO(new Date()));
    ncfile.addGlobalAttribute("time_coverage_end", dateFormatter.toDateTimeStringISO(new Date()));
    ncfile.addGlobalAttribute("geospatial_lat_min", 0.0);
    ncfile.addGlobalAttribute("geospatial_lat_max", 0.0);
    ncfile.addGlobalAttribute("geospatial_lon_min", 0.0);
    ncfile.addGlobalAttribute("geospatial_lon_max", 0.0);

    createCoordinates(timeUnit);
    createDataVariables(vars);

    ncfile.create(); // done with define mode

    // now write the observations
    if (! (Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
      throw new IllegalStateException("can't add record variable");
  }

  private void createCoordinates(DateUnit timeUnit) throws IOException {
    ncfile.addUnlimitedDimension(recordDimName);

    // time variable
    Variable timeVar = ncfile.addVariable(timeName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(timeVar, new Attribute("units", timeUnit.getUnitsString()));
    ncfile.addVariableAttribute(timeVar, new Attribute("long_name", "time of measurement"));

    // add the station Variables using the station dimension
    Variable v = ncfile.addVariable(latName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_north"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station latitude"));

    v = ncfile.addVariable(lonName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute("units", "degrees_east"));
    ncfile.addVariableAttribute(v, new Attribute("long_name", "station longitude"));

    if (altUnits != null) {
      v = ncfile.addVariable(altName, DataType.DOUBLE, recordDimName);
      ncfile.addVariableAttribute(v, new Attribute("units", altUnits));
      // ncfile.addVariableAttribute(v, new Attribute("positive", "up"));
      ncfile.addVariableAttribute(v, new Attribute("long_name", "altitude"));
    }
  }

  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
    String coordNames = timeName + " " + latName +" "+ lonName;
    if (altUnits != null)
      coordNames = coordNames +" " + altName;

    // find all dimensions needed by the data variables
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (!d.isUnlimited())
        ncfile.addDimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : dataVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        newVar.addAttribute( att);
      }

      newVar.addAttribute( new Attribute("coordinates", coordNames));
    }

  }

  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 latArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 lonArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 altArray = new ArrayDouble.D1(1);
  private int[] origin = new int[1];

  public void writeRecord(PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(sobs.getObservationTime(), sobs.getObservationTimeAsDate(), sobs.getLocation(), sdata);
  }

  public void writeRecord(double timeCoordValue, Date obsDate, EarthLocation loc, StructureData sdata) throws IOException {

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // date is handled specially
    if ((minDate == null) || minDate.after(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.before(obsDate)) maxDate = obsDate;

    timeArray.set(0, timeCoordValue);
    latArray.set(0, loc.getLatitude());
    lonArray.set(0, loc.getLongitude());
    if (altUnits != null)
      altArray.set(0, loc.getAltitude());
    trackBB(loc);

    // write the recno record
    origin[0] = recno;
    try {
      ncfile.write("record", origin, sArray);
      ncfile.write(timeName, origin, timeArray);
      ncfile.write(latName, origin, latArray);
      ncfile.write(lonName, origin, lonArray);
      if (altUnits != null)
        ncfile.write(altName, origin, altArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

  public void finish() throws IOException {
    ncfile.updateAttribute(null, new Attribute("geospatial_lat_min", llbb.getLowerLeftPoint().getLatitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lat_max", llbb.getUpperRightPoint().getLatitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lon_min", llbb.getLowerLeftPoint().getLongitude()));
    ncfile.updateAttribute(null, new Attribute("geospatial_lon_max", llbb.getUpperRightPoint().getLongitude()));

    // if there is no data
    if (minDate == null) minDate = new Date();
    if (maxDate == null) maxDate = new Date();

    ncfile.updateAttribute(null, new Attribute("time_coverage_start", dateFormatter.toDateTimeStringISO(minDate)));
    ncfile.updateAttribute(null, new Attribute("time_coverage_end", dateFormatter.toDateTimeStringISO(maxDate)));

    ncfile.close();
  }

  private LatLonRect llbb = null;
  private void trackBB(EarthLocation loc) {
    if (llbb == null) {
      llbb = new LatLonRect(loc.getLatLon(), .001, .001);
      return;
    }
    llbb.extend(loc.getLatLon());
  }

}