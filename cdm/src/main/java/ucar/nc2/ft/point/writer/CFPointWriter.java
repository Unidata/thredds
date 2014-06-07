/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.ACDD;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.*;

/**
 * Write Point Feature Collections into netcdf3/4 files in CF 1.6 point obs conventions.
 * <ul>
 *   <li>netcdf3: use indexed ragged array representation</li>
 * </ul>
 *
 * @author caron
 * @since 4/11/12
 */
public class CFPointWriter {
  private static boolean debug = false;

  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, NetcdfFileWriter.Version version) throws IOException {
    if (debug) System.out.printf("CFPointWriter write to file %s%n ", fileOut);

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut, version);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut, version);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut, version);

      } /* else if (fc instanceof StationProfileFeatureCollection) {
        count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, show);
        if (showStructureData) showStructureData((StationProfileFeatureCollection) fc );

      } else if (fc instanceof SectionFeatureCollection) {
        count = checkSectionFeatureCollection((SectionFeatureCollection) fc, show);

      } else {
        count = checkNestedPointFeatureCollection((NestedPointFeatureCollection) fc, show);
      } */
    }

    return 0;
  }

  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc, String fileOut,
                                                 NetcdfFileWriter.Version version) throws IOException {

    WriterCFPointCollection writer = new WriterCFPointCollection(version, fileOut, fdpoint.getGlobalAttributes());

    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (count == 0)
        writer.writeHeader(fdpoint.getDataVariables(), pf.getTimeUnit(), null);

      writer.writeRecord(pf, pf.getData());
      count++;
      if (debug && count % 100 == 0) System.out.printf("%d ", count);
      if (debug && count % 1000 == 0) System.out.printf("%n ");
    }

    writer.finish();
    return count;
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint fdpoint, StationTimeSeriesFeatureCollection fds,
                                                   String fileOut, NetcdfFileWriter.Version version) throws IOException {

    WriterCFStationCollection writer = new WriterCFStationCollection(version, fileOut, fdpoint.getGlobalAttributes());
    ucar.nc2.ft.PointFeatureCollection pfc = fds.flatten(null, (CalendarDateRange) null); // LOOK

    int count = 0;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (count == 0)
        writer.writeHeader(fds.getStations(), fdpoint.getDataVariables(), pf.getTimeUnit(), "");

      StationPointFeature spf = (StationPointFeature) pf;
      writer.writeRecord(spf.getStation(), pf, pf.getData());
      count++;
      if (debug && count % 100 == 0) System.out.printf("%d ", count);
      if (debug && count % 1000 == 0) System.out.printf("%n ");
    }

    writer.finish();
    return count;
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection pds,
                                                   String fileOut, NetcdfFileWriter.Version version) throws IOException {

    WriterCFProfileCollection writer = new WriterCFProfileCollection(fileOut, fdpoint.getGlobalAttributes(), version);

    int count = 0;
    List<String> profiles = new ArrayList<>();
    pds.resetIteration();
    while (pds.hasNext()) {
      profiles.add(pds.next().getName());  // LOOK why
    }

    pds.resetIteration();
    while (pds.hasNext()) {
      ucar.nc2.ft.ProfileFeature profile = pds.next();

      profile.resetIteration();
      while (profile.hasNext()) {
        ucar.nc2.ft.PointFeature pf = profile.next();
        if (count == 0)
          writer.writeHeader(profiles, fdpoint.getDataVariables(), pf.getTimeUnit(), null); // LOOK altitude units ??

        writer.writeRecord(profile.getName(), pf, pf.getData());
        count++;
        if (debug && count % 100 == 0) System.out.printf("%d ", count);
        if (debug && count % 1000 == 0) System.out.printf("%n ");
      }
    }

    writer.finish();
    return count;
  }

  /////////////////////////////////////////////////

  // attributes with these names will not be copied to the output file
  private static final String[] reservedGAtts = new String[]{
          CDM.CONVENTIONS, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.TIME_START, ACDD.TIME_END,
          _Coordinate._CoordSysBuilder, CF.featureTypeAtt2, CF.featureTypeAtt3};

  private static final String[] reservedVAtts = new String[]{
          _Coordinate.AxisType};

  protected static final List<String> reservedGlobalAtts = Arrays.asList(reservedGAtts);
  protected static final List<String> reservedVariableAtts = Arrays.asList(reservedVAtts);

  protected static final String recordDimName = "obs";
  protected static final String latName = "latitude";
  protected static final String lonName = "longitude";
  protected static final String altName = "altitude";
  protected static final String timeName = "time";

  /////////////////////////////////////////////////
  //protected final boolean isNetcdf3;
  protected NetcdfFileWriter writer;
  protected Map<String, Variable> dataVarMap = new HashMap<>(); // used for netcdf4
  protected Structure record;  // used for netcdf3
  protected Set<Dimension> dimSet = new HashSet<>(20);

  protected String altUnits = null;
  protected LatLonRect llbb = null;
  protected CalendarDate minDate = null;
  protected CalendarDate maxDate = null;

  protected final boolean addTimeCoverage;
  protected final boolean isNetcdf3;

  protected CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version) throws IOException {
    createWriter(fileOut, version);
    addGlobalAtts(atts);
    this.addTimeCoverage = true;
    this.isNetcdf3 = (writer.getVersion() == NetcdfFileWriter.Version.netcdf3);
    if (isNetcdf3) addNetcdf3UnknownAtts(true);
  }

  /**
   * Ctor
   * @param fileOut         name of the output file
   * @param atts            attributes to be added
   * @param version         netcdf file version
   * @param addTimeCoverage for files that don't have time dimension indicates we won't have time coverage attributes either
   * @throws IOException
   */
  protected CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version, boolean addTimeCoverage) throws IOException {
    createWriter(fileOut, version);
    addGlobalAtts(atts);
    this.addTimeCoverage = addTimeCoverage;
    this.isNetcdf3 = (writer.getVersion() == NetcdfFileWriter.Version.netcdf3);
    if (isNetcdf3) addNetcdf3UnknownAtts(addTimeCoverage);
  }

  private void createWriter(String fileOut, NetcdfFileWriter.Version version) throws IOException {
    writer = NetcdfFileWriter.createNew(version, fileOut, null);
    writer.setFill(false);
  }

  private void addGlobalAtts(List<Attribute> atts) {
    writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    writer.addGroupAttribute(null, new Attribute(CDM.HISTORY, "Written by CFPointWriter"));
    for (Attribute att : atts) {
      if (!reservedGlobalAtts.contains(att.getShortName()))
        writer.addGroupAttribute(null, att);
    }
  }

  // netcdf3 has to add attributes up front, but we dont know values until the end.
  // so we have this updateAttribute hack; values set in finish()
  private void addNetcdf3UnknownAtts(boolean addTimeCoverage) {
    // dummy values, update in finish()
    if (addTimeCoverage) {
      CalendarDate now = CalendarDate.of(new Date());
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(now)));
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(now)));
    }
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, 0.0));
  }

  public void setLength(long size) {
    writer.setLength(size);
  }

  protected void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {

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
      if (writer.findVariable(oldVar.getShortName()) != null)
        continue;  // eliminate coordinate variables

      // make dimension list
      List<Dimension> oldDims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : oldDims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getShortName());
      }

      Variable newVar;
       if ((oldVar.getDataType().equals(DataType.STRING))) {
         newVar = writer.addStringVariable(null, (Variable) oldVar, writer.makeDimList(null, dimNames.toString())); // LOOK can we cast to Variable ?
       } else {
         newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());
       }

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        if (!reservedVariableAtts.contains(att.getShortName()))
          newVar.addAttribute(att);
      }

      String coordNames = timeName + " " + latName +" "+ lonName;
      if (altUnits != null)
        coordNames = coordNames +" " + altName;
      newVar.addAttribute( new Attribute(CF.COORDINATES, coordNames));

      dataVarMap.put(newVar.getShortName(), newVar);
    }

  }

  protected void trackBB(EarthLocation loc, CalendarDate obsDate) {
    if (loc != null) {
      if (llbb == null) {
        llbb = new LatLonRect(loc.getLatLon(), .001, .001);
        return;
      }
      llbb.extend(loc.getLatLon());
    }

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate)) maxDate = obsDate;
  }

  public void finish() throws IOException {
    if (llbb != null) {
      updateAtt(new Attribute(ACDD.LAT_MIN, llbb.getLowerLeftPoint().getLatitude()));
      updateAtt(new Attribute(ACDD.LAT_MAX, llbb.getUpperRightPoint().getLatitude()));
      updateAtt(new Attribute(ACDD.LON_MIN, llbb.getLowerLeftPoint().getLongitude()));
      updateAtt(new Attribute(ACDD.LON_MAX, llbb.getUpperRightPoint().getLongitude()));
    }

    if (addTimeCoverage) {
      if (minDate == null) minDate = CalendarDate.present();
      if (maxDate == null) maxDate = CalendarDate.present();
      updateAtt(new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(minDate)));
      updateAtt(new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(maxDate)));
    }

    writer.close();
  }

  private void updateAtt(Attribute att) throws IOException {
    if (writer.getVersion() == NetcdfFileWriter.Version.netcdf3)
      writer.updateAttribute(null, att);
    else
      writer.addGroupAttribute(null, att);
  }

  protected int writeStructureData(int[] origin, StructureData sdata) throws IOException, InvalidRangeException {
    if (writer.getVersion() == NetcdfFileWriter.Version.netcdf3) {
      return writer.appendStructureData(record, sdata);  // can write it all at once along unlimited dimension

    } else  {
      for (StructureMembers.Member m : sdata.getMembers()) {  // netcdf4 assume classic model
        Array org = sdata.getArray(m);
        Array orgPlus1 = Array.makeArrayRankPlusOne(org);  // add dimension on the left (slow)
        int[] useOrigin = origin;

        if (org.getRank() > 0) {                          // if rank 0 (common case, this is a nop, so skip
          useOrigin = new int[org.getRank()+1];
          useOrigin[0] = origin[0]; // the rest are 0
        }

        Variable mv = dataVarMap.get(m.getName());
        if (mv == null)
          continue;     // LOOK
        writer.write(mv, useOrigin, orgPlus1);
      }
      return origin[0];
    }

  }



}
