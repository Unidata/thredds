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

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.nc2.*;
import ucar.nc2.ft.*;
import ucar.unidata.geoloc.EarthLocation;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;

/**
 * Write a CF 1.6 "Discrete Sample" point file.
 *
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8294224"
 * @author caron
 * @since Nov 23, 2010
 */
public class WriterCFPointCollection extends CFPointWriter {
  private Variable time, lat, lon, alt, record;

  public WriterCFPointCollection(String fileOut, String title) throws IOException {
    this(null, fileOut, Arrays.asList(new Attribute[]{new Attribute(CDM.TITLE, title)}));
  }

  public WriterCFPointCollection(NetcdfFileWriter.Version version, String fileOut, List<Attribute> atts) throws IOException {
    super(fileOut, atts, version);

    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.point.name()));
  }

  public void writeHeader(List<VariableSimpleIF> vars, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;

    createCoordinates(timeUnit);
    createDataVariables(vars);

    writer.create(); // done with define mode
    record = writer.addRecordStructure();
  }

  private void createCoordinates(DateUnit timeUnit) throws IOException {
    writer.addUnlimitedDimension(recordDimName);

    // time variable
    time = writer.addVariable(null, timeName, DataType.DOUBLE, recordDimName);
    writer.addVariableAttribute(time, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
    writer.addVariableAttribute(time, new Attribute(CDM.LONG_NAME, "time of measurement"));

    // add the station Variables using the station dimension
    lat = writer.addVariable(null, latName, DataType.DOUBLE, recordDimName);
    writer.addVariableAttribute(lat, new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    writer.addVariableAttribute(lat, new Attribute(CDM.LONG_NAME, "station latitude"));

    lon = writer.addVariable(null, lonName, DataType.DOUBLE, recordDimName);
    writer.addVariableAttribute(lon, new Attribute(CDM.UNITS, CDM.LON_UNITS));
    writer.addVariableAttribute(lon, new Attribute(CDM.LONG_NAME, "station longitude"));

    if (altUnits != null) {
      alt = writer.addVariable(null, altName, DataType.DOUBLE, recordDimName);
      writer.addVariableAttribute(alt, new Attribute(CDM.UNITS, altUnits));
      // ncfile.addVariableAttribute(v, new Attribute("positive", "up"));
      writer.addVariableAttribute(alt, new Attribute(CDM.LONG_NAME, "altitude"));
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
        writer.addDimension(null, d.getShortName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : dataVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getShortName());
      }
      Variable newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        newVar.addAttribute( att);
      }

      newVar.addAttribute( new Attribute(CF.COORDINATES, coordNames));
    }

  }

  /////////////////////////////////////////////////////////
  // writing data

  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 latArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 lonArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 altArray = new ArrayDouble.D1(1);
  private int[] origin = new int[1];

  public void writeRecord(PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sobs.getLocation(), sdata);
  }

  public void writeRecord(double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata) throws IOException {
    trackBB(loc, obsDate);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    timeArray.set(0, timeCoordValue);
    latArray.set(0, loc.getLatitude());
    lonArray.set(0, loc.getLongitude());
    if (altUnits != null)
      altArray.set(0, loc.getAltitude());

    // write the recno record
    origin[0] = recno;
    try {
      writer.write(record, origin, sArray);
      writer.write(time, origin, timeArray);
      writer.write(lat, origin, latArray);
      writer.write(lon, origin, lonArray);
      if (altUnits != null)
        writer.write(alt, origin, altArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

}