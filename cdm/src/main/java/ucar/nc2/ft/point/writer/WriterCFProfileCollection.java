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
import ucar.ma2.*;
import ucar.unidata.geoloc.EarthLocation;

import java.util.*;
import java.io.IOException;

/**
 * Write a CF "Discrete Sample" profile collection file.
 * Example H.3.5. Indexed ragged array representation of profiles.
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#time-series-data"
 * @author caron
 * @since Aug 19, 2009
 */
public class WriterCFProfileCollection extends CFWriter {
  private static final String profileDimName = "profile";
  private static final String idName = "profile_id";
  private static final String profileIndexName = "profileIndex";
  private static final boolean debug = false;

  private int name_strlen = 1;

  private List<Dimension> profileDims = new ArrayList<Dimension>(1);

  public WriterCFProfileCollection(String fileOut, String title) throws IOException {
    super(fileOut, title);

    ncfile.addGlobalAttribute(CF.FEATURE_TYPE, CF.FeatureType.profile.name());
  }

  public void writeHeader(List<String> profileNames, List<VariableSimpleIF> vars, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;
    
    createProfiles(profileNames);
    createObsVariables(timeUnit);
    createDataVariables(vars);

    ncfile.create(); // done with define mode

    writeProfileData(profileNames); // write out the profile info

    // now write the observations
    if (!(Boolean) ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE))
      throw new IllegalStateException("can't add record variable");
  }

  /*
    variables:
      int profile(profile) ;
          profile:cf_name = "profile_id";
      double time(profile);
          time:standard_name = "time";
          time:long_name = "time" ;
          time:units = "days since 1970-01-01 00:00:00" ;
      float lon(profile);
          lon:standard_name = "longitude";
          lon:long_name = "longitude" ;
          lon:units = "degrees_east" ;
      float lat(profile);
          lat:standard_name = "latitude";
          lat:long_name = "latitude" ;
          lat:units = "degrees_north" ;
   */
  private void createProfiles(List<String> profileNames) throws IOException {
    int nprofiles = profileNames.size();

    // find string lengths
    for (String name : profileNames) {
      name_strlen = Math.max(name_strlen, name.length());
    }

    // add the dimensions
    ncfile.addUnlimitedDimension(recordDimName);
    Dimension profileDim = ncfile.addDimension(profileDimName, nprofiles);
    profileDims.add(profileDim);

    // add the profile Variables using the profile dimension
    Variable v = ncfile.addStringVariable(idName, profileDims, name_strlen);
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "profile identifier"));
    ncfile.addVariableAttribute(v, new Attribute(CF.CF_ROLE, CF.PROFILE_ID));

    v = ncfile.addVariable(latName, DataType.DOUBLE, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, "degrees_north"));
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station latitude"));

    v = ncfile.addVariable(lonName, DataType.DOUBLE, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, "degrees_east"));
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station longitude"));

    v = ncfile.addVariable(altName, DataType.DOUBLE, profileDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.UNITS, altUnits));
    ncfile.addVariableAttribute(v, new Attribute(CF.POSITIVE, CF.POSITIVE_UP));
  }

  private void createObsVariables(DateUnit timeUnit) throws IOException {
    // time variable
    Variable timeVar = ncfile.addVariable(timeName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.LONG_NAME, "time of measurement"));

    Variable v = ncfile.addVariable(profileIndexName, DataType.INT, recordDimName);
    ncfile.addVariableAttribute(v, new Attribute(CDM.LONG_NAME, "station index for this observation record"));
    ncfile.addVariableAttribute(v, new Attribute(CF.INSTANCE_DIMENSION, profileDimName));
  }

  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
    String coordNames = latName + " " + lonName + " " + altName + " " + timeName;

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
    
    // find all variables already in use 
    List<VariableSimpleIF> useDataVars = new ArrayList<VariableSimpleIF>(dataVars.size());
    for (VariableSimpleIF var : dataVars) {
      if (ncfile.findVariable(var.getShortName()) == null) useDataVars.add(var);
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : useDataVars) {
      List<Dimension> dims = oldVar.getDimensions();
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      }
      Variable newVar = ncfile.addVariable(oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());

      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {
        newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordNames));
    }
  }

  private HashMap<String, Integer> stationMap;

  private void writeProfileData(List<String> profiles) throws IOException {
    int nprofiles = profiles.size();
    stationMap = new HashMap<String, Integer>(2 * nprofiles);
    if (debug) System.out.println("stationMap created");

    // now write the station data
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nprofiles);

    for (int i = 0; i < profiles.size(); i++) {
      String name = profiles.get(i);
      stationMap.put(name, i);
      idArray.set(i, name);
    }

    try {
      ncfile.writeStringData(idName, idArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  private int recno = 0;
  private ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 latArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 lonArray = new ArrayDouble.D1(1);
  private ArrayDouble.D1 altArray = new ArrayDouble.D1(1);
  private ArrayInt.D1 parentArray = new ArrayInt.D1(1);
  private int[] origin = new int[1];

  public void writeRecord(String profileName, PointFeature sobs, StructureData sdata) throws IOException {
    writeRecord(profileName, sobs.getObservationTime(), sobs.getObservationTimeAsCalendarDate(), sobs.getLocation(), sdata);
  }

  public void writeRecord(String profileName, double timeCoordValue, CalendarDate obsDate, EarthLocation loc, StructureData sdata) throws IOException {
    Integer parentIndex = stationMap.get(profileName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find station " + profileName);

    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate)) minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate)) maxDate = obsDate;

    timeArray.set(0, timeCoordValue);
    latArray.set(0, loc.getLatitude());
    lonArray.set(0, loc.getLongitude());
    altArray.set(0, loc.getAltitude());
    parentArray.set(0, parentIndex);

    // write the recno record
    origin[0] = recno;
    try {
      ncfile.write("record", origin, sArray);
      ncfile.write(timeName, origin, timeArray);
      ncfile.write(profileIndexName, origin, parentArray);

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

}