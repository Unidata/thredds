/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.writer;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" station profile collection file.
 * Example H.3.5. Contiguous ragged array representation of profiles, H.5.3
 * *
 * @author caron
 * @since 7/14/2014
 */
public class WriterCFStationProfileCollection extends CFPointWriter {

  private List<StationFeature> stnList;
  protected Structure stationStruct;  // used for netcdf4 extended
  private HashMap<String, Integer> stationIndexMap;

  private boolean useDesc = false;
  private boolean useAlt = false;
  private boolean useWmoId = false;

  private int desc_strlen = 1, wmo_strlen = 1;
  private Map<String, Variable> stationVarMap  = new HashMap<>();

  ///////////////////////////////////////////////////
  // private Formatter coordNames = new Formatter();
  protected Structure profileStruct;  // used for netcdf4 extended
  private Map<String, Variable> profileVarMap = new HashMap<>();
  private boolean headerDone = false;

  public WriterCFStationProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars, List<Variable> extra,
                                   DateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, extra, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.timeSeriesProfile.name()));
  }

  public void setStations(List<StationFeature> stns) throws IOException {
    this.stnList = stns;

    // see if there's altitude, wmoId for any stations
    for (StationFeature stn : stnList) {
      if (!Double.isNaN(stn.getAltitude()))
        useAlt = true;
      if ((stn.getWmoId() != null) && (stn.getWmoId().trim().length() > 0))
        useWmoId = true;
      if ((stn.getDescription() != null) && (stn.getDescription().trim().length() > 0))
        useDesc = true;

    // find string lengths
      id_strlen = Math.max(id_strlen, stn.getName().length());
      if (stn.getDescription() != null) desc_strlen = Math.max(desc_strlen, stn.getDescription().length());
      if (stn.getWmoId() != null) wmo_strlen = Math.max(wmo_strlen, stn.getWmoId().length());
    }

    llbb = CFPointWriterUtils.getBoundingBox(stnList); // gets written in super.finish();
  }

  public int writeProfile (StationProfileFeature spf, ProfileFeature profile) throws IOException {
    profile.resetIteration();
    int count = 0;
    while (profile.hasNext()) {
      PointFeature pf = profile.next();
      if (!headerDone) {
        if (id_strlen == 0) id_strlen = profile.getName().length() * 2;
        writeHeader(spf, profile, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    Integer stnIndex = stationIndexMap.get(spf.getName());
    if (stnIndex == null) {
      System.out.printf("BAD station %s%n", spf.getName());
    } else {
      writeProfileData(stnIndex, profile, count);
    }

    return count;
  }

  private void writeHeader(StationProfileFeature stn, ProfileFeature profile, PointFeature obs) throws IOException {

    StructureData stnData = stn.getFeatureData();
    StructureData profileData = profile.getFeatureData();
    StructureData obsData = obs.getFeatureData();

    List<VariableSimpleIF> obsCoords = new ArrayList<>();
    // obsCoords.add(VariableSimpleImpl.makeScalar(timeName, "time of measurement", timeUnit.getUnitsString(), DataType.DOUBLE)); // LOOK ??
    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    //if (useAlt) {
      obsCoords.add( VariableSimpleImpl.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
              .add(new Attribute(CF.STANDARD_NAME, "altitude"))
              .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits))));
      coordNames.format(" %s", altitudeCoordinateName);
    //}

    super.writeHeader2(obsCoords, stnData, profileData, obsData, coordNames.toString());

    // write the stations
    int count = 0;
    stationIndexMap = new HashMap<>(2 * stnList.size());
    for (StationFeature sf : stnList) {
      writeStationData(sf);
      stationIndexMap.put(sf.getName(), count);
      count++;
    }

    /* prefill any needed profile vars
    if (!isExtendedModel) {
      Variable stnIndexVar = writer.findVariable(stationIndexName);
      try {
        writer.write(stnIndexVar, Array.factory(DataType.INT, new int[]{nfeatures}));  // default 0
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }

      Variable profileIdxVar = writer.findVariable(profileIdName);
      try {
        Array prefill = Array.factory(DataType.INT, new int[]{nfeatures});
        MAMath.setDouble(prefill, idMissingValue);
        writer.write(stnIndexVar, prefill);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    } */

  }

  protected void makeFeatureVariables(StructureData stnData, boolean isExtended) throws IOException {

    // add the dimensions : extended model can use an unlimited dimension
    //Dimension stationDim = isExtended ? writer.addDimension(null, stationDimName, 0, true, true, false) : writer.addDimension(null, stationDimName, nstns);
    Dimension stationDim = writer.addDimension(null, stationDimName, stnList.size());

    List<VariableSimpleIF> stnVars = new ArrayList<>();
    stnVars.add(VariableSimpleImpl.makeScalar(latName, "station latitude", CDM.LAT_UNITS, DataType.DOUBLE));
    stnVars.add(VariableSimpleImpl.makeScalar(lonName, "station longitude", CDM.LON_UNITS, DataType.DOUBLE));

    if (useAlt) {
      stnVars.add(VariableSimpleImpl.makeScalar(stationAltName, "station altitude", altUnits, DataType.DOUBLE)
              .add(new Attribute(CF.STANDARD_NAME, CF.SURFACE_ALTITUDE))
              .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits))));
    }

    stnVars.add(VariableSimpleImpl.makeString(stationIdName, "station identifier", null, id_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.TIMESERIES_ID)));         // station_id:cf_role = "timeseries_id";

    if (useDesc) stnVars.add(VariableSimpleImpl.makeString(descName, "station description", null, desc_strlen)
            .add(new Attribute(CF.STANDARD_NAME, CF.PLATFORM_NAME)));

    if (useWmoId) stnVars.add(VariableSimpleImpl.makeString(wmoName, "station WMO id", null, wmo_strlen)
            .add(new Attribute(CF.STANDARD_NAME, CF.PLATFORM_ID)));

    for (StructureMembers.Member m : stnData.getMembers()) {
      if (getDataVar(m.getName()) != null)
        stnVars.add(new VariableSimpleAdapter(m));
    }

    if (isExtended) {
      stationStruct = (Structure) writer.addVariable(null, stationStructName, DataType.STRUCTURE, stationDimName);
      addCoordinatesExtended(stationStruct, stnVars);
    } else {
      addCoordinatesClassic(stationDim, stnVars, stationVarMap);
    }

  }

  private int stnRecno = 0;
  private void writeStationData(StationFeature stn) throws IOException {

    StructureDataScalar stnCoords = new StructureDataScalar("Coords");
    stnCoords.addMember(latName, null, null, DataType.DOUBLE, false, stn.getLatLon().getLatitude());
    stnCoords.addMember(lonName, null, null, DataType.DOUBLE, false, stn.getLatLon().getLongitude());
    if (useAlt) stnCoords.addMember(stationAltName, null, null, DataType.DOUBLE, false, stn.getAltitude());
    stnCoords.addMemberString(stationIdName, null, null, stn.getName().trim(), id_strlen);
    if (useDesc) stnCoords.addMemberString(descName, null, null, stn.getDescription().trim(), desc_strlen);
    if (useWmoId) stnCoords.addMemberString(wmoName, null, null, stn.getWmoId().trim(), wmo_strlen);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(stnCoords); // coords first so it takes precedence
    sdall.add(stn.getFeatureData());

    stnRecno = super.writeStructureData(stnRecno, stationStruct, sdall, stationVarMap);
  }

  @Override
  protected void makeMiddleVariables(StructureData profileData, boolean isExtended) throws IOException {

    Dimension profileDim = writer.addDimension(null, profileDimName, nfeatures);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> profileVars = new ArrayList<>();
    profileVars.add(VariableSimpleImpl.makeString(profileIdName, "profile identifier", null, id_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.PROFILE_ID))             // profileId:cf_role = "profile_id";
            .add(new Attribute(CDM.MISSING_VALUE, String.valueOf(idMissingValue))));

    profileVars.add(VariableSimpleImpl.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .add(new Attribute(CF.SAMPLE_DIMENSION, recordDimName)));         // rowSize:sample_dimension = "obs"

    profileVars.add(VariableSimpleImpl.makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUnitsString(), DataType.DOUBLE));

    profileVars.add(VariableSimpleImpl.makeScalar(stationIndexName, "station index for this profile", null, DataType.INT)
            .add(new Attribute(CF.INSTANCE_DIMENSION, stationDimName)));

    for (StructureMembers.Member m : profileData.getMembers()) {
      VariableSimpleIF dv = getDataVar(m.getName());
      if (dv != null)
        profileVars.add(dv);
    }

    if (isExtended) {
      profileStruct = (Structure) writer.addVariable(null, profileStructName, DataType.STRUCTURE, profileDimName);
      addCoordinatesExtended(profileStruct, profileVars);
    } else {
      addCoordinatesClassic(profileDim, profileVars, profileVarMap);
    }
  }

  private int profileRecno = 0;
  public void writeProfileData(int stnIndex, ProfileFeature profile, int nobs) throws IOException {
    trackBB(profile.getLatLon(), CalendarDate.of(profile.getTime()));

    StructureDataScalar profileCoords = new StructureDataScalar("Coords");
    profileCoords.addMember(latName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLatitude());
    profileCoords.addMember(lonName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLongitude());
    //Date date = (profile.getTime() != null) ? (double) profile.getTime().getTime() : 0.0;   // LOOK (profile.getTime() != null) ???
    double timeInMyUnits = timeUnit.makeValue(profile.getTime());
    profileCoords.addMember(profileTimeName, null, null, DataType.DOUBLE, false, timeInMyUnits);  // LOOK time not always part of profile
    profileCoords.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    profileCoords.addMember(numberOfObsName, null, null, DataType.INT, false, nobs);
    profileCoords.addMember(stationIndexName, null, null, DataType.INT, false, stnIndex);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(profileCoords); // coords first so it takes precedence
    sdall.add(profile.getFeatureData());

    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, profileVarMap);
  }


  private int obsRecno = 0;
  public void writeObsData(PointFeature pf) throws IOException {

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(altitudeCoordinateName, null, null, DataType.DOUBLE, false, pf.getLocation().getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getFeatureData());

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
