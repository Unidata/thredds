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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.conv.CF1Convention;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;

import java.io.IOException;
import java.util.*;

/**
 * Write a CF "Discrete Sample" trajectory collection file.
 * Example H.3.5. Contiguous ragged array representation of trajectories, H.4.3
 *
 * @author caron
 * @since 7/11/2014
 */
public class WriterCFTrajectoryCollection extends CFPointWriter {

  ///////////////////////////////////////////////////
  private Structure featureStruct;  // used for netcdf4 extended
  private Map<String, Variable> featureVarMap = new HashMap<>();
  private boolean headerDone = false;

  public WriterCFTrajectoryCollection(String fileOut, List<Attribute> globalAtts, List<VariableSimpleIF> dataVars, List<Variable> extra,
                                      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    super(fileOut, globalAtts, dataVars, extra, timeUnit, altUnits, config);
    writer.addGroupAttribute(null, new Attribute(CF.FEATURE_TYPE, CF.FeatureType.trajectory.name()));
    writer.addGroupAttribute(null, new Attribute(CF.DSG_REPRESENTATION, "Contiguous ragged array representation of trajectories, H.4.3"));
  }

  public int writeTrajectory (TrajectoryFeature traj) throws IOException {
    int count = 0;
    for (PointFeature pf : traj) {
      if (!headerDone) {
        if (id_strlen == 0) id_strlen = traj.getName().length() * 2;
        writeHeader(traj, pf);
        headerDone = true;
      }
      writeObsData(pf);
      count++;
    }

    writeTrajectoryData(traj, count);
    return count;
  }

  private void writeHeader(TrajectoryFeature feature, PointFeature obs) throws IOException {

    // obs data
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

    super.writeHeader(coords, feature.getFeatureData(), obs.getFeatureData(), coordNames.toString());
  }

  protected void makeFeatureVariables(StructureData featureData, boolean isExtended) throws IOException {

    // LOOK why not unlimited here fro extended model ?
    Dimension profileDim = writer.addDimension(null, trajDimName, nfeatures);

    // add the profile Variables using the profile dimension
    List<VariableSimpleIF> featureVars = new ArrayList<>();
    featureVars.add(VariableSimpleImpl.makeString(trajIdName, "trajectory identifier", null, id_strlen)
            .add(new Attribute(CF.CF_ROLE, CF.TRAJECTORY_ID)));

    featureVars.add(VariableSimpleImpl.makeScalar(numberOfObsName, "number of obs for this profile", null, DataType.INT)
            .add(new Attribute(CF.SAMPLE_DIMENSION, recordDimName)));

    for (StructureMembers.Member m : featureData.getMembers()) {
      VariableSimpleIF dv = getDataVar(m.getName());
      if (dv != null)
        featureVars.add(dv);
    }

    if (isExtended) {
      featureStruct = (Structure) writer.addVariable(null, trajStructName, DataType.STRUCTURE, trajDimName);
      addCoordinatesExtended(featureStruct, featureVars);
    } else {
      addCoordinatesClassic(profileDim, featureVars, featureVarMap);
    }
  }

  private int trajRecno = 0;
  public void writeTrajectoryData(TrajectoryFeature profile, int nobs) throws IOException {

    StructureDataScalar profileCoords = new StructureDataScalar("Coords");
    profileCoords.addMemberString(trajIdName, null, null, profile.getName().trim(), id_strlen);
    profileCoords.addMember(numberOfObsName, null, null, DataType.INT, nobs);

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(profileCoords);
    sdall.add(profile.getFeatureData());

    trajRecno = super.writeStructureData(trajRecno, featureStruct, sdall, featureVarMap);
  }


  private int obsRecno = 0;
  public void writeObsData(PointFeature pf) throws IOException {
    EarthLocation loc = pf.getLocation();
    trackBB(loc.getLatLon(), timeUnit.makeCalendarDate(pf.getObservationTime()));

    StructureDataScalar coords = new StructureDataScalar("Coords");
    coords.addMember(timeName, null, null, DataType.DOUBLE, pf.getObservationTime());
    coords.addMember(latName,  null, null, DataType.DOUBLE, loc.getLatitude());
    coords.addMember(lonName,  null, null, DataType.DOUBLE, loc.getLongitude());
    if (altUnits != null) coords.addMember(altName, null, null, DataType.DOUBLE, loc.getAltitude());

    StructureDataComposite sdall = new StructureDataComposite();
    sdall.add(coords); // coords first so it takes precedence
    sdall.add(pf.getFeatureData());

    obsRecno = super.writeStructureData(obsRecno, record, sdall, dataMap);
  }


}
