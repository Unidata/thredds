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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayObject;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;

/**
 * Write a CF "Discrete Sample" profile collection file.
 * Example H.3.5. Indexed ragged array representation of profiles
 * LOOK: better to use contiguous, H.3.4
 *
 * <p/>
 * <pre>
 *   writeHeader()
 *   iterate { writeRecord() }
 *   finish()
 * </pre>
 *
 * @see "http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#idp8372832"
 * @author caron
 * @since April, 2012
 */
public class WriterCFProfileCollection extends CFPointWriter {
  private static final String profileDimName = "profile";
  private static final String idName = "profileId";
  private static final String profileIndexName = "profileIndex";
  private static final boolean debug = false;

  ///////////////////////////////////////////////////
  private int name_strlen = 1;
  private Variable id, index, time, record;

  public WriterCFProfileCollection(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version) throws IOException {
    super(fileOut, atts, version);

    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.profile.name()));
  }

  public void writeHeader(List<String> profileNames, List<VariableSimpleIF> dataVars, DateUnit timeUnit, String altUnits) throws IOException {
    this.altUnits = altUnits;
    
    createProfiles(profileNames);
    createObsVariables(timeUnit);
    createDataVariables(dataVars);

    writer.create(); // done with define mode
    
    record = writer.addRecordStructure();
    
    // System.out.printf("%s%n", ncfile);

    writeProfileData(profileNames); // write out the profile info
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
    writer.addUnlimitedDimension(recordDimName);

    List<Dimension> profileDims = new ArrayList<Dimension>(1);
    Dimension profileDim = writer.addDimension(null, profileDimName, nprofiles);
    profileDims.add(profileDim);

    // add the profile Variables using the profile dimension
    id = writer.addStringVariable(null, idName, profileDims, name_strlen);
    writer.addVariableAttribute(id, new Attribute(CDM.LONG_NAME, "profile identifier"));
    writer.addVariableAttribute(id, new Attribute(CF.CF_ROLE, CF.PROFILE_ID));

    Variable lat = writer.addVariable(null, latName, DataType.DOUBLE, profileDimName);
    writer.addVariableAttribute(lat, new Attribute(CDM.UNITS, CDM.LAT_UNITS));
    writer.addVariableAttribute(lat, new Attribute(CDM.LONG_NAME, "profile latitude"));

    Variable lon = writer.addVariable(null, lonName, DataType.DOUBLE, profileDimName);
    writer.addVariableAttribute(lon, new Attribute(CDM.UNITS, CDM.LON_UNITS));
    writer.addVariableAttribute(lon, new Attribute(CDM.LONG_NAME, "profile longitude"));

    if (altUnits != null) {
      Variable alt = writer.addVariable(null, altName, DataType.DOUBLE, profileDimName);
      writer.addVariableAttribute(alt, new Attribute(CDM.UNITS, altUnits));
      writer.addVariableAttribute(alt, new Attribute(CDM.LONG_NAME, "profile altitude"));
    }
  }

  private void createObsVariables(DateUnit timeUnit) throws IOException {

    // time variable LOOK could also be time(profile)
    time = writer.addVariable(null, timeName, DataType.DOUBLE, recordDimName);
    writer.addVariableAttribute(time, new Attribute(CDM.UNITS, timeUnit.getUnitsString()));
    writer.addVariableAttribute(time, new Attribute(CDM.LONG_NAME, "time of measurement"));

    
    /*Variable zVar = ncfile.addVariable(zName, DataType.DOUBLE, recordDimName);
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.UNITS, zUnit));
    ncfile.addVariableAttribute(timeVar, new Attribute(CDM.LONG_NAME, "time of measurement")); */

    index = writer.addVariable(null, profileIndexName, DataType.INT, recordDimName);
    writer.addVariableAttribute(index, new Attribute(CDM.LONG_NAME, "profile index for this observation record"));
    writer.addVariableAttribute(index, new Attribute(CF.INSTANCE_DIMENSION, profileDimName));
  }

  private void createDataVariables(List<VariableSimpleIF> dataVars) throws IOException {
    String coordNames = latName + " " + lonName  + " " + timeName;
    if (altUnits != null) coordNames += " " + altName;

    /* find all dimensions needed by the data variables
    for (VariableSimpleIF var : dataVars) {
      List<Dimension> dims = var.getDimensions();
      dimSet.addAll(dims);
    }

    // add them
    for (Dimension d : dimSet) {
      if (!d.isUnlimited())
        ncfile.addDimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
    } */
    
    // find all variables already in use 
    List<VariableSimpleIF> useDataVars = new ArrayList<VariableSimpleIF>(dataVars.size()); // LOOK doesnt make sense - must eliminate non data vars in calling routine
    for (VariableSimpleIF var : dataVars) {
      if (writer.findVariable(var.getShortName()) == null) useDataVars.add(var);
    }

    // add the data variables all using the record dimension
    for (VariableSimpleIF oldVar : useDataVars) {
      /* List<Dimension> dims = oldVar.getDimensions();  // LOOK missing vectors - must be able to eliminate the z dimension
      StringBuilder dimNames = new StringBuilder(recordDimName);
      for (Dimension d : dims) {
        if (!d.isUnlimited())
          dimNames.append(" ").append(d.getName());
      } */

      Variable newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), recordDimName);
      List<Attribute> atts = oldVar.getAttributes();
      for (Attribute att : atts) {  // LOOK filter ??
        newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordNames));
    }
  }

  private HashMap<String, Integer> profileMap;

  private void writeProfileData(List<String> profiles) throws IOException {
    int nprofiles = profiles.size();
    profileMap = new HashMap<String, Integer>(2 * nprofiles);
    if (debug) System.out.println("stationMap created");

    // now write the profile data
    ArrayObject.D1 idArray = new ArrayObject.D1(String.class, nprofiles);

    for (int i = 0; i < profiles.size(); i++) {
      String name = profiles.get(i);
      profileMap.put(name, i);
      idArray.set(i, name);
    }

    try {
      writer.writeStringData(id, idArray);

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
    trackBB(loc, obsDate);

    Integer parentIndex = profileMap.get(profileName);
    if (parentIndex == null)
      throw new RuntimeException("Cant find profile " + profileName);

    int[] parentIndexArr = {parentIndex};
    
    // needs to be wrapped as an ArrayStructure, even though we are only writing one at a time.
    ArrayStructureW sArray = new ArrayStructureW(sdata.getStructureMembers(), new int[]{1});
    sArray.setStructureData(sdata, 0);

    //timeArray.set(0, timeCoordValue);
    latArray.set(0, loc.getLatitude());
    lonArray.set(0, loc.getLongitude());
    altArray.set(0, loc.getAltitude());
    parentArray.set(0, parentIndex);

    // write the recno record
    origin[0] = recno;
    
    try {

    	//if record is null variables that use the unlimited dimension are not in a structure
    	//we decompose the structure data and write the variables contained on it sequentially?
    	// --> fails!!
    	if(record == null){
    		StructureMembers sm = sdata.getStructureMembers();
    		for( Member m : sm.getMembers() ){
    			Variable v = writer.findVariable(m.getName());
    			if( v != null && !v.getShortName().equals(lonName) && !v.getShortName().equals(latName) ){
    				//DataType dt = m.getDataType();
    				DataType v_dt =v.getDataType();
    				DataType m_dt =m.getDataType();
    				
    				if(m_dt == DataType.DOUBLE ){
    					Double data = m.getDataArray().getDouble(0);
    					ArrayDouble.D1 tmpArray = new ArrayDouble.D1(1);
    					tmpArray.setDouble(0, data);
    					writer.write( writer.findVariable(m.getName()) , origin, tmpArray );
    				}
    				
    				if(m_dt == DataType.FLOAT){
    					Float data = m.getDataArray().getFloat(0);
    					ArrayFloat.D1 tmpArray = new ArrayFloat.D1(1);
    					tmpArray.setFloat(0, data);
    					writer.write( writer.findVariable(m.getName()) , origin, tmpArray );
    				}    				    				    				
    			}
    		}
    	}
    	else{
    		writer.write(record, origin, sArray);
    	}
    	
    	//writer.write(time, origin, timeArray); //--> time was added in the structure and is written in write(record,...)
    	writer.write(index, origin, parentArray);
    	
   		writer.write(writer.findVariable(latName), parentIndexArr, latArray);
   		writer.write(writer.findVariable(lonName), parentIndexArr, lonArray);


    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

}