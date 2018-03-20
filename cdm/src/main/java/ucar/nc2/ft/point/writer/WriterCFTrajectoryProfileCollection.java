/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.time.CalendarDateUnit;

import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" trajectory profile (section) collection file.
 * Contiguous ragged array representation of trajectory profile, H.6.3
 * *
 * @author caron
 * @since 7/14/2014
 */
public class WriterCFTrajectoryProfileCollection extends CFPointWriter {
  public static final String trajectoryIndexName = "trajectoryIndex";

  private int ntraj;
  private int traj_strlen;
  private Structure trajStructure;  // used for netcdf4 extended
  private HashMap<String, Integer> trajIndexMap;

  private Map<String, Variable> trajVarMap  = new HashMap<>();

  ///////////////////////////////////////////////////
  private Structure profileStruct;  // used for netcdf4 extended
  private Map<String, Variable> profileVarMap = new HashMap<>();
  private boolean headerDone = false;

  public WriterCFTrajectoryProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars,
                                             CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectoryProfile.name()));
    writer.addGroupAttribute(null, new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of trajectory profile, H.6.3"));
  }

  public void setFeatureAuxInfo2(int ntraj, int traj_strlen) {
    this.ntraj = ntraj;
    this.traj_strlen = traj_strlen;
    trajIndexMap = new HashMap<>(2 * ntraj);
  }

  public int writeProfile (TrajectoryProfileFeature section, ProfileFeature profile) throws IOException {
    int count = 0;
    for (PointFeature pf : profile) {
      if (!headerDone) {
        if (id_strlen == 0) id_strlen = profile.getName().length() * 2;
        writeHeader(section, profile, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    Integer sectionIndex = trajIndexMap.get(section.getName());
    if (sectionIndex == null) {
      sectionIndex = writeSectionData(section);
      trajIndexMap.put(section.getName(), sectionIndex);
    }
    writeProfileData(sectionIndex, profile, count);
    return count;
  }

  private void writeHeader(TrajectoryProfileFeature section, ProfileFeature profile, PointFeature obs) throws IOException {

    StructureData sectionData = section.getFeatureData();
    StructureData profileData = profile.getFeatureData();
    StructureData obsData = obs.getFeatureData();

    Formatter coordNames = new Formatter().format("%s %s %s", profileTimeName, latName, lonName);
    List<VariableSimpleIF> obsCoords = new ArrayList<>();
    if (useAlt) {
      obsCoords.add( VariableSimpleImpl.makeScalar(altitudeCoordinateName, "obs altitude", altUnits, DataType.DOUBLE)
              .add(new Attribute(CF.STANDARD_NAME, "altitude"))
              .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altitudeCoordinateName, altUnits))));
      coordNames.format(" %s", altitudeCoordinateName);
    }

    super.writeHeader2(obsCoords, sectionData, profileData, obsData, coordNames.toString());
  }

  protected void makeFeatureVariables(StructureData trajData, boolean isExtended) throws IOException {

    // add the dimensions : extended model can use an unlimited dimension
    Dimension trajDim = writer.addDimension(null, trajDimName, ntraj);

    List<VariableSimpleIF> trajVars = new ArrayList<>();

    trajVars.add(VariableSimpleImpl.makeString(trajIdName, "trajectory identifier", null, traj_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.TRAJECTORY_ID)));

    for (StructureMembers.Member m : trajData.getMembers()) {
      if (getDataVar(m.getName()) != null)
        trajVars.add(new VariableSimpleAdapter(m));
    }

    if (isExtended) {
      trajStructure = (Structure) writer.addVariable(null, trajStructName, DataType.STRUCTURE, trajDimName);
      addCoordinatesExtended(trajStructure, trajVars);
    } else {
      addCoordinatesClassic(trajDim, trajVars, trajVarMap);
    }

  }

  private int trajRecno = 0;
  private int writeSectionData(TrajectoryProfileFeature section) throws IOException {

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMemberString(trajIdName, null, null, section.getName().trim(), traj_strlen);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(section.getFeatureData());

    trajRecno = super.writeStructureData(trajRecno, trajStructure, sdall, trajVarMap);
    return trajRecno-1;
  }

  @Override
  protected void makeMiddleVariables(StructureData profileData, boolean isExtended) throws IOException {

    Dimension profileDim = writer.addDimension(null, profileDimName, nfeatures);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> profileVars = new ArrayList<>();
    profileVars.add(VariableSimpleImpl.makeString(profileIdName, "profile identifier", null, id_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.PROFILE_ID))             // profileId:cf_role = "profile_id";
            .add(new Attribute(CDM.MISSING_VALUE, String.valueOf(idMissingValue))));

    profileVars.add(VariableSimpleImpl.makeScalar(latName, "profile latitude", CDM.LAT_UNITS, DataType.DOUBLE));
    profileVars.add(VariableSimpleImpl.makeScalar(lonName, "profile longitude", CDM.LON_UNITS, DataType.DOUBLE));
    profileVars.add(VariableSimpleImpl.makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUdUnit(), DataType.DOUBLE)
            .add(new Attribute(CF.CALENDAR, timeUnit.getCalendar().toString())));

    profileVars.add(VariableSimpleImpl.makeScalar(trajectoryIndexName, "trajectory index for this profile", null, DataType.INT)
            .add(new Attribute(CF.INSTANCE_DIMENSION, trajDimName)));

    profileVars.add(VariableSimpleImpl.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .add(new Attribute(CF.SAMPLE_DIMENSION, recordDimName)));

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
  public void writeProfileData(int sectionIndex, ProfileFeature profile, int nobs) throws IOException {
    trackBB(profile.getLatLon(), profile.getTime());

    StructureDataScalar profileCoords = new StructureDataScalar("Coords");
    profileCoords.addMember(latName, null, null, DataType.DOUBLE, profile.getLatLon().getLatitude());
    profileCoords.addMember(lonName, null, null, DataType.DOUBLE, profile.getLatLon().getLongitude());
    // double time = (profile.getTime() != null) ? (double) profile.getTime().getTime() : 0.0;
    double timeInMyUnits = timeUnit.makeOffsetFromRefDate(profile.getTime());
    profileCoords.addMember(profileTimeName, null, null, DataType.DOUBLE, timeInMyUnits);  // LOOK time not always part of profile
    profileCoords.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    profileCoords.addMember(numberOfObsName, null, null, DataType.INT, nobs);
    profileCoords.addMember(trajectoryIndexName, null, null, DataType.INT, sectionIndex);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(profileCoords); // coords first so it takes precedence
    sdall.add(profile.getFeatureData());

    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, profileVarMap);
  }


  private int obsRecno = 0;
  public void writeObsData(PointFeature pf) throws IOException {

    StructureDataScalar coords = new StructureDataScalar("Coords");
    if (useAlt) coords.addMember(altitudeCoordinateName, null, null, DataType.DOUBLE, pf.getLocation().getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getFeatureData());

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
