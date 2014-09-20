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
import ucar.nc2.ft.SectionFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" trajectory profile (section) collection file.
 * Example H.3.5. Contiguous ragged array representation of trajectory profile, H.5.3
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

  public WriterCFTrajectoryProfileCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars, List<Variable> extra,
                                   DateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, extra, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectoryProfile.name()));
  }

  public void setFeatureAuxInfo2(int ntraj, int traj_strlen) {
    this.ntraj = ntraj;
    this.traj_strlen = traj_strlen;
    trajIndexMap = new HashMap<>(2 * ntraj);
  }

  public int writeProfile (SectionFeature section, ProfileFeature profile) throws IOException {
    profile.resetIteration();
    int count = 0;
    while (profile.hasNext()) {
      PointFeature pf = profile.next();
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

  private void writeHeader(SectionFeature section, ProfileFeature profile, PointFeature obs) throws IOException {

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
  private int writeSectionData(SectionFeature section) throws IOException {

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
    profileVars.add(VariableSimpleImpl.makeScalar(profileTimeName, "nominal time of profile", timeUnit.getUnitsString(), DataType.DOUBLE));

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
    trackBB(profile.getLatLon(), CalendarDate.of(profile.getTime()));

    StructureDataScalar profileCoords = new StructureDataScalar("Coords");
    profileCoords.addMember(latName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLatitude());
    profileCoords.addMember(lonName, null, null, DataType.DOUBLE, false, profile.getLatLon().getLongitude());
    // double time = (profile.getTime() != null) ? (double) profile.getTime().getTime() : 0.0;
    double timeInMyUnits = timeUnit.makeValue(profile.getTime());
    profileCoords.addMember(profileTimeName, null, null, DataType.DOUBLE, false, timeInMyUnits);  // LOOK time not always part of profile
    profileCoords.addMemberString(profileIdName, null, null, profile.getName().trim(), id_strlen);
    profileCoords.addMember(numberOfObsName, null, null, DataType.INT, false, nobs);
    profileCoords.addMember(trajectoryIndexName, null, null, DataType.INT, false, sectionIndex);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(profileCoords); // coords first so it takes precedence
    sdall.add(profile.getFeatureData());

    profileRecno = super.writeStructureData(profileRecno, profileStruct, sdall, profileVarMap);
  }


  private int obsRecno = 0;
  public void writeObsData(PointFeature pf) throws IOException {

    StructureDataScalar coords = new StructureDataScalar("Coords");
    if (useAlt) coords.addMember(altitudeCoordinateName, null, null, DataType.DOUBLE, false, pf.getLocation().getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getFeatureData());

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }

}
