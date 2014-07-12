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
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;

import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" trajectory collection file.
 * Example H.3.5. Contiguous ragged array representation of trajectories, H.4.4
 *
 * @author caron
 * @since 7/11/2014
 */
public class WriterCFTrajectoryCollection extends CFPointWriter {

  private static final String featureStructName = "traj";
  private static final String featureDimName = "traj";
  private static final String idName = "trajectoryId";
  private static final String numberOfObsName = "nobs";

  ///////////////////////////////////////////////////
  protected Structure featureStruct;  // used for netcdf4 extended

  private boolean headerDone = false;
  private int ntrajs, name_strlen;

  private Map<String, Variable> dataMap  = new HashMap<>();
  private Map<String, Variable> profileMap  = new HashMap<>();

  public WriterCFTrajectoryCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars, List<Variable> extra, DateUnit timeUnit, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, extra, config);
    this.timeUnit = timeUnit;
    this.dataVars = dataVars;
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectory.name()));
  }

  public void setHeaderInfo(int nprofiles, int name_strlen) {
    this.ntrajs = nprofiles;
    this.name_strlen = name_strlen;
  }

  public int writeTrajectory (TrajectoryFeature feature) throws IOException {
    feature.resetIteration();
    int count = 0;
    while (feature.hasNext()) {
      PointFeature pf = feature.next();
      if (!headerDone) {
        if (name_strlen == 0) name_strlen = feature.getName().length() * 2;
        writeHeader(name_strlen, ntrajs, feature, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    writeTrajectoryData(feature, count);
    return count;
  }

  private void writeHeader(int name_strlen, int ntrajs, TrajectoryFeature feature, PointFeature obs) throws IOException {
    this.recordDim = writer.addUnlimitedDimension(recordDimName);
    this.altUnits = feature.getAltUnits();
    DateUnit timeUnit = feature.getTimeUnit();

    StructureData trajData = feature.getFeatureData();
    StructureData obsData = obs.getFeatureData();

    // obs
    List<VariableSimpleIF> coords = new ArrayList<>();
    coords.add(VariableSimpleImpl.makeScalar(timeName, "time of measurement", timeUnit.getUnitsString(), DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeScalar(latName,  "latitude of measurement", CDM.LAT_UNITS, DataType.DOUBLE));
    coords.add(VariableSimpleImpl.makeScalar(lonName,  "longitude of measurement", CDM.LON_UNITS, DataType.DOUBLE));
    Formatter coordNames = new Formatter().format("%s %s %s", timeName, latName, lonName);
    if (altUnits != null) {
      coords.add( VariableSimpleImpl.makeScalar(altName, "altitude of measurement", altUnits, DataType.DOUBLE)
                      .add(new Attribute(CF.POSITIVE, CF1Convention.getZisPositive(altName, altUnits))));
      coordNames.format(" %s", altName);
    }

    addExtraVariables();

    if (writer.getVersion().isExtendedModel()) {
      makeFeatureVariables(name_strlen, ntrajs, trajData, timeUnit, true);
      record = (Structure) writer.addVariable(null, recordName, DataType.STRUCTURE, recordDimName);
      addCoordinatesExtended(record, coords);
      addDataVariablesExtended(obsData, coordNames.toString());
      record.calcElementSize();
      writer.create();

    } else {
      makeFeatureVariables(name_strlen, ntrajs, trajData, timeUnit, false);
      addCoordinatesClassic(recordDim, coords, dataMap);
      addDataVariablesClassic(recordDim, obsData, dataMap, coordNames.toString());
      writer.create();
      record = writer.addRecordStructure(); // for netcdf3
    }

    writeExtraVariables();
  }

  private void makeFeatureVariables(int name_strlen, int ntrajs, StructureData trajData, DateUnit timeUnit, boolean isExtended) throws IOException {
    name_strlen = Math.max( name_strlen, 10);

    // LOOK why not unlimited here ?
    Dimension profileDim = writer.addDimension(null, featureDimName, ntrajs);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> featureVars = new ArrayList<>();
    featureVars.add(VariableSimpleImpl.makeString(idName, "trajectory identifier", null, name_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.TRAJECTORY_ID)));

    featureVars.add(VariableSimpleImpl.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .add(new Attribute(CF.SAMPLE_DIMENSION, recordDimName)));

    for (StructureMembers.Member m : trajData.getMembers()) {
      VariableSimpleIF dv = getDataVar(m.getName());
      if (dv != null)
        featureVars.add(dv);
    }

    if (isExtended) {
      featureStruct = (Structure) writer.addVariable(null, featureStructName, DataType.STRUCTURE, featureDimName);
      addCoordinatesExtended(featureStruct, featureVars);
    } else {
      addCoordinatesClassic(profileDim, featureVars, profileMap);
    }
  }

  private int profileRecno = 0;
  public void writeTrajectoryData(TrajectoryFeature profile, int nobs) throws IOException {

    StructureDataScalar profileCoords = new StructureDataScalar("Coords");
    profileCoords.addMemberString(idName, null, null, profile.getName().trim(), name_strlen);
    profileCoords.addMember(numberOfObsName, null, null, DataType.INT, false, nobs);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(profileCoords); // coords first so it takes precedence
    sdall.add(profile.getFeatureData());

    int[] origin = new int[1];
    origin[0] = profileRecno;
    try {
      if (isExtendedModel)
        super.writeStructureData(featureStruct, origin, sdall);
      else {
        super.writeStructureDataClassic(profileMap, origin, sdall);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    profileRecno++;
  }


  private int recno = 0;
  public void writeObsData(PointFeature pf) throws IOException {
    EarthLocation loc = pf.getLocation();
    trackBB(loc.getLatLon(), timeUnit.makeCalendarDate(pf.getObservationTime()));

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(timeName, null, null, DataType.DOUBLE, false, pf.getObservationTime());
    coords.addMember(latName,  null, null, DataType.DOUBLE, false, loc.getLatitude());
    coords.addMember(lonName,  null, null, DataType.DOUBLE, false, loc.getLongitude());
    if (altUnits != null) coords.addMember(altName, null, null, DataType.DOUBLE, false, loc.getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getFeatureData());

    // write the recno record
    int[] origin = new int[1];
    origin[0] = recno;
    try {
      if (isExtendedModel)
        super.writeStructureData(record, origin, sdall);
      else {
        super.writeStructureDataClassic(dataMap, origin, sdall);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    recno++;
  }

}
