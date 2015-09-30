/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.writer;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
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
  //private Map<String, Variable> varMap  = new HashMap<>();

  public WriterCFPointCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
                                 CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.point.name()));
    writer.addGroupAttribute(null, new Attribute(CF.DSG_REPRESENTATION, "Point Data, H.1"));
  }

  public void writeHeader(PointFeature pf) throws IOException {
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleImpl.makeScalar(timeName, "time of measurement", timeUnit.getUdUnit(), DataType.DOUBLE)
            .add(new Attribute(CF.CALENDAR, timeUnit.getCalendar().toString())));

    coords.add(VariableSimpleImpl.makeScalar(latName,  "latitude of measurement", CDM.LAT_UNITS, DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeScalar(lonName,  "longitude of measurement", CDM.LON_UNITS, DataType.DOUBLE));
    Formatter coordNames = new Formatter().format("%s %s %s", timeName, latName, lonName);
    if (altUnits != null) {
      coords.add( VariableSimpleImpl.makeScalar(altName, "altitude of measurement", altUnits, DataType.DOUBLE)
                      .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits))));
      coordNames.format(" %s", altName);
    }

    super.writeHeader(coords, null, pf.getDataAll(), coordNames.toString());
  }

  protected void makeFeatureVariables(StructureData featureData, boolean isExtended) throws IOException {
    // NOOP
  }

  /////////////////////////////////////////////////////////
  // writing data

  public void writeRecord(PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sobs.getLocation(), sdata);
  }

  private int obsRecno = 0;
  public void writeRecord(double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata) throws IOException {
    trackBB(loc.getLatLon(), obsDate);

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(timeName, null, null, DataType.DOUBLE, timeCoordValue);
    coords.addMember(latName,  null, null, DataType.DOUBLE, loc.getLatitude());
    coords.addMember(lonName,  null, null, DataType.DOUBLE, loc.getLongitude());
    if (altUnits != null) coords.addMember(altName, null, null, DataType.DOUBLE, loc.getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(sdata);

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}