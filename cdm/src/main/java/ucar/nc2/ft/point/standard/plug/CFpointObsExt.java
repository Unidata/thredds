/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft.point.standard.Evaluator;
import ucar.nc2.ft.point.standard.TableConfig;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * CFpointObs using extended model, namely netcdf-4 Structures
 *
 * @author caron
 * @since 6/26/2014
 */
public class CFpointObsExt extends CFpointObs {

  @Override
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String conv = ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, null);
    return conv != null && (conv.equalsIgnoreCase(CDM.CF_EXTENDED));
  }

  @Override
  protected boolean identifyEncodingStation(NetcdfDataset ds, EncodingInfo info, CF.FeatureType ftype, Formatter errlog) {
    Structure obs =  info.time.getParentStructure();
    if (obs.getRank() == 0) {
      errlog.format("CFpointObs: must have a non-scalar Time coordinate%n");
      return false;
    }
    Dimension obsDim = obs.getDimension(0);

    Structure station =  info.lat.getParentStructure();
    if (station.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, obsDim);
    }
    Dimension stnDim = station.getDimension(0);

    // the raggeds
    if (identifyRaggeds(ds, info, stnDim, obsDim, errlog))
      return true;

    errlog.format("CFpointObsExt: %s Must have Lat/Lon coordinates of rank 0 or 1%n", ftype);
    return false;
  }

  @Override
  protected boolean identifyEncodingProfile(NetcdfDataset ds, EncodingInfo info, Formatter errlog) {
    Evaluator.VarAtt varatt = Evaluator.findVariableWithAttribute(ds, CF.SAMPLE_DIMENSION);
    if (varatt == null) return false;
    String dimName = varatt.att.getStringValue();
    Dimension obsDim = ds.findDimension(dimName);

    Structure profile =  info.lat.getParentStructure();
    if (profile.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, obsDim);
    }
    Dimension profileDim = profile.getDimension(0);

    // now find the child structure
    info.childStruct = Evaluator.findStructureWithDimensions(ds, obsDim, null);

    // the raggeds
    if (identifyRaggeds(ds, info, profileDim, obsDim, errlog))
      return true;

    errlog.format("CFpointObsExt: %s only supports ragged array representation%n", CF.FeatureType.profile);
    return false;
  }

  @Override
  protected boolean identifyEncodingTraj(NetcdfDataset ds, EncodingInfo info, Formatter errlog) {
    // find the obs structure
    info.childStruct =  info.lat.getParentStructure();
    Dimension obsDim = info.childStruct.getDimension(0);

    // find the traj structure
    Variable varatt = Evaluator.findVariableWithAttributeValue(ds, CF.CF_ROLE, CF.TRAJECTORY_ID);
    Structure traj =  varatt.getParentStructure();
    if (traj.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, obsDim);
    }
    Dimension trajDim = traj.getDimension(0);

    if (identifyRaggeds(ds, info, trajDim, obsDim, errlog))
      return true;

    errlog.format("CFpointObsExt: %s only supports ragged array representation%n", CF.FeatureType.trajectory);
    return false;
  }


  @Override
  protected boolean identifyEncodingTimeSeriesProfile(NetcdfDataset ds, EncodingInfo info, CF.FeatureType ftype, Formatter errlog) {
    // find the obs structure
    Evaluator.VarAtt varatt = Evaluator.findVariableWithAttribute(ds, CF.SAMPLE_DIMENSION);
    if (varatt == null) return false;
    String dimName = varatt.att.getStringValue();
    info.grandChildDim = ds.findDimension(dimName);
    info.grandChildStruct = Evaluator.findStructureWithDimensions(ds, info.grandChildDim, null);

   // find the station structure
    Variable stdId = Evaluator.findVariableWithAttributeValue(ds, CF.CF_ROLE, CF.TIMESERIES_ID);
    Structure stn =  stdId.getParentStructure();
    if (stn.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, info.grandChildDim);
    }
    info.parentDim = stn.getDimension(0);
    info.parentStruct = stn;

    // find the profile structure
    Variable profileId = Evaluator.findVariableWithAttributeValue(ds, CF.CF_ROLE, CF.PROFILE_ID);
    Structure profile =  profileId.getParentStructure();
    info.childDim = profile.getDimension(0);
    info.childStruct = profile;

    // find the non-station altitude
    VariableDS z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("CFpointObs: timeSeriesProfile must have a z coordinate, not the station altitude%n");
      return false;
    }
    info.alt = z;

    // raggeds
    if (identifyDoubleRaggeds(ds, info, errlog))
      return true;

    errlog.format("CFpointObsExt: %s only supports ragged array representation%n", CF.FeatureType.timeSeriesProfile);
    return false;
  }

  @Override
  protected boolean identifyEncodingSection(NetcdfDataset ds, EncodingInfo info, CF.FeatureType ftype, Formatter errlog) {
    // find the obs structure
    Evaluator.VarAtt varatt = Evaluator.findVariableWithAttribute(ds, CF.SAMPLE_DIMENSION);
    if (varatt == null) return false;
    String dimName = varatt.att.getStringValue();
    info.grandChildDim = ds.findDimension(dimName);
    info.grandChildStruct = Evaluator.findStructureWithDimensions(ds, info.grandChildDim, null);

   // find the traj structure
    Variable trajId = Evaluator.findVariableWithAttributeValue(ds, CF.CF_ROLE, CF.TRAJECTORY_ID);
    Structure traj =  trajId.getParentStructure();
    if (traj.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, info.grandChildDim);
    }
    info.parentDim = traj.getDimension(0);
    info.parentStruct = traj;

    // find the profile structure
    Variable profileId = Evaluator.findVariableWithAttributeValue(ds, CF.CF_ROLE, CF.PROFILE_ID);
    Structure profile =  profileId.getParentStructure();
    info.childDim = profile.getDimension(0);
    info.childStruct = profile;

    // find the non-station altitude
    VariableDS z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("CFpointObs: section must have a z coordinate%n");
      return false;
    }
    if (z.getRank() == 0 && z.getParentStructure() == null) {
      errlog.format("CFpointObs: section cannot have a scalar z coordinate%n");
      return false;
    }
    info.alt = z;

        // raggeds
    if (identifyDoubleRaggeds(ds, info, errlog))
      return true;

    errlog.format("CFpointObsExt: %s only supports ragged array representation%n", CF.FeatureType.trajectoryProfile);
    return false;
  }
}
