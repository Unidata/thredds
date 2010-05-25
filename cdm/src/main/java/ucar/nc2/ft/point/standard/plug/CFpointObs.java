/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.point.standard.CoordSysEvaluator;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * CF "point obs" Convention.
 *
 * @author caron
 * @see "http://www.unidata.ucar.edu/software/netcdf-java/reference/FeatureDatasets/CFencodingTable.html"
 * @since Nov 3, 2008
 */
public class CFpointObs extends TableConfigurerImpl {

  private enum Encoding {
    single, multidim, raggedContiguous, raggedIndex, flat
  }

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.startsWith("CF"))
        return true;
    }
    return false;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    // figure out the actual feature type of the dataset
    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    if (ftypeS == null)
      ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt2, null);
    if (ftypeS == null)
      ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt3, null);

    CF.FeatureType ftype;
    if (ftypeS == null)
      ftype = CF.FeatureType.point;
    else {
      ftype = CF.FeatureType.getFeatureType(ftypeS);
      if (ftypeS == null)
        ftype = CF.FeatureType.point;
    }

    // make sure lat, lon, time coordinates exist
    if (!checkCoordinates(ds, errlog)) return null;

    switch (ftype) {
      case point:
        return getPointConfig(ds, errlog);
      case timeSeries:
        return getStationConfig(ds, errlog);
      case trajectory:
        return getTrajectoryConfig(ds, errlog);
      case profile:
        return getProfileConfig(ds, errlog);
      case timeSeriesProfile:
        return getTimeSeriesProfileConfig(ds, errlog);
      case trajectoryProfile:
        return getSectionConfig(ds, errlog);
    }

    return null;
  }


  private boolean checkCoordinates(NetcdfDataset ds, Formatter errlog) {
    boolean ok = true;
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("CFpointObs cant find a Time coordinate %n");
      ok = false;
    }

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("CFpointObs cant find a Latitude coordinate %n");
      ok = false;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("CFpointObs cant find a Longitude coordinate %n");
      ok = false;
    }

    if (!ok) return false;

    // dimensions must match
    List<Dimension> dimLat = lat.getDimensions();
    List<Dimension> dimLon = lon.getDimensions();
    if (!dimLat.equals(dimLon)) {
      errlog.format("Lat and Lon coordinate dimensions must match lat=%s lon=%s %n", lat.getNameAndDimensions(), lon.getNameAndDimensions());
      ok = false;
    }

    return ok;
  }

  /////////////////////////////////////////////////////////////////////////////////


  private TableConfig getPointConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() != 1) {
      errlog.format("CFpointObs type=point: coord time must have rank 1, coord var= %s %n", time.getNameAndDimensions());
      return null;
    }
    Dimension obsDim = time.getDimension(0);

    TableConfig obsTable = makeSingle(ds, obsDim, errlog);
    obsTable.featureType = FeatureType.POINT;
    return obsTable;
  }

  ////

  private TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncodingStation(ds, CF.FeatureType.timeSeries, errlog);
    if (info == null) return null;

    // obs dimension
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    Variable parentId = identifyParent(ds, CF.FeatureType.timeSeries);
    Dimension obsDim = info.childDim;

    // make station table
    TableConfig stnTable = makeStationTable(ds, FeatureType.STATION, info, errlog);
    if (stnTable == null) return null;

    TableConfig obsTable = null;
    switch (info.encoding) {
      case single:
        obsTable = makeSingle(ds, obsDim, errlog);
        break;

      case multidim:
        obsTable = makeMultidimInner(ds, stnTable, obsDim, errlog);
        if (time.getRank() == 1) { // time(time)
          obsTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));
          obsTable.time = time.getShortName();
        }
        break;

      case raggedContiguous:
        obsTable = makeRaggedContiguous(ds, info.parentDim, info.childDim, errlog);
        break;

      case raggedIndex:
        obsTable = makeRaggedIndex(ds, info.parentDim, info.childDim, errlog);
        break;

      case flat:
        obsTable = makeStructTable(ds, FeatureType.STATION, new EncodingInfo(Encoding.flat, obsDim), errlog);
        obsTable.parentIndex = (parentId == null) ? null : parentId.getName();
        obsTable.stnId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ID, obsDim, errlog);
        obsTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_DESC, obsDim, errlog);
        obsTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_WMOID, obsDim, errlog);
        obsTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ALTITUDE, obsDim, errlog);
        break;
    }
    if (obsTable == null) return null;

    stnTable.addChild(obsTable);
    return stnTable;
  }

  ////

  private TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncodingTraj(ds, CF.FeatureType.trajectory, errlog);
    if (info == null) return null;

    TableConfig parentTable = makeStructTable(ds, FeatureType.TRAJECTORY, info, errlog);
    if (parentTable == null) return null;
    parentTable.feature_id = identifyParentId(ds, CF.FeatureType.trajectory);
    if (parentTable.feature_id == null) {
      errlog.format("getTrajectoryConfig cant find a trajectoy id %n");
    }

    // obs table
    //Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    //Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(traj, obs)

    TableConfig obsConfig = null;
    switch (info.encoding) {
      case single:
        obsConfig = makeSingle(ds, info.childDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidimInner(ds, parentTable, info.childDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, info.parentDim, info.childDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, info.parentDim, info.childDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: trajectory flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  ////

  private TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncodingProfile(ds, CF.FeatureType.profile, errlog);
    if (info == null) return null;

    TableConfig parentTable = makeStructTable(ds, FeatureType.PROFILE, info, errlog);
    if (parentTable == null) return null;
    parentTable.feature_id = identifyParentId(ds, CF.FeatureType.profile);
    if (parentTable.feature_id == null) {
      errlog.format("getProfileConfig cant find a profile id %n");
    }

    // obs table
    VariableDS z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (z == null) {
      errlog.format("getProfileConfig cant find a Height coordinate %n");
      return null;
    }
    //Dimension obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)

    TableConfig obsTable = null;
    switch (info.encoding) {
      case single:
        obsTable = makeSingle(ds, info.childDim, errlog);
        break;
      case multidim:
        obsTable = makeMultidimInner(ds, parentTable, info.childDim, errlog);
        if (z.getRank() == 1) // z(z)
          obsTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        break;
      case raggedContiguous:
        obsTable = makeRaggedContiguous(ds, info.parentDim, info.childDim, errlog);
        break;
      case raggedIndex:
        obsTable = makeRaggedIndex(ds, info.parentDim, info.childDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: profile flat encoding");
    }
    if (obsTable == null) return null;

    parentTable.addChild(obsTable);
    return parentTable;
  }

  ////

  private TableConfig getTimeSeriesProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncodingTimeSeriesProfile(ds, CF.FeatureType.timeSeriesProfile, errlog);
    if (info == null) return null;

    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() == 0) {
      errlog.format("timeSeriesProfile cannot have a scalar time coordinate%n");
      return null;
    }

    // find the non-station altitude
    VariableDS z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("timeSeriesProfile must have a z coordinate%n");
      return null;
    }
    if (z.getRank() == 0) {
      errlog.format("timeSeriesProfile cannot have a scalar z coordinate%n");
      return null;
    }

    /* distinguish multidim from flat
    if ((info.encoding == Encoding.multidim) && (time.getRank() < 3) && (z.getRank() < 3)) {
      Variable parentId = identifyParent(ds, CF.FeatureType.timeSeriesProfile);
      if ((parentId != null) && (parentId.getRank() == 1) && (parentId.getDimension(0).equals(time.getDimension(0)))) {
        if (time.getRank() == 1) // multidim time must be 2 or 3 dim
          info = new EncodingInfo(Encoding.flat, parentId);
        else if (time.getRank() == 2) {
          Dimension zDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)
          if (zDim.equals(time.getDimension(1))) // flat 2D time will have time as inner dim 
            info = new EncodingInfo(Encoding.flat, parentId);
        }
      }
    } */

    TableConfig stationTable = makeStationTable(ds, FeatureType.STATION_PROFILE, info, errlog);
    if (stationTable == null) return null;

    //Dimension stationDim = ds.findDimension(stationTable.dimName);
    //Dimension profileDim = null;
    //Dimension zDim = null;

    switch (info.encoding) {
      case single: {
        assert ((time.getRank() >= 1) && (time.getRank() <= 2)) : "time must be rank 1 or 2";
        assert ((z.getRank() >= 1) && (z.getRank() <= 2)) : "z must be rank 1 or 2";

        if (time.getRank() == 2) {
          if (z.getRank() == 2)  // 2d time, 2d z
            assert time.getDimensions().equals(z.getDimensions()) : "rank-2 time and z dimensions must be the same";
          else  // 2d time, 1d z
            assert time.getDimension(1).equals(z.getDimension(0)) : "rank-2 time must have z inner dimension";
          //profileDim = time.getDimension(0);
          //zDim = time.getDimension(1);

        } else { // 1d time
          if (z.getRank() == 2) { // 1d time, 2d z
            assert z.getDimension(0).equals(time.getDimension(0)) : "rank-2 z must have time outer dimension";
            //profileDim = z.getDimension(0);
            //zDim = z.getDimension(1);
          } else { // 1d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            //profileDim = time.getDimension(0);
            //zDim = z.getDimension(0);
          }
        }
        // make profile table
        TableConfig profileTable = makeStructTable(ds, FeatureType.PROFILE, new EncodingInfo(Encoding.multidim, info.childDim), errlog);
        if (profileTable == null) return null;
        if (time.getRank() == 1) // join time(time)
          profileTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));
        stationTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner(ds, profileTable, info.grandChildDim, errlog);
        if (z.getRank() == 1) // join z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);

        break;
      }

      case multidim: {
        assert ((time.getRank() >= 2) && (time.getRank() <= 3)) : "time must be rank 2 or 3";
        assert ((z.getRank() == 1) || (z.getRank() == 3)) : "z must be rank 1 or 3";

        if (time.getRank() == 3) {
          if (z.getRank() == 3)  // 3d time, 3d z
            assert time.getDimensions().equals(z.getDimensions()) : "rank-3 time and z dimensions must be the same";
          else  // 3d time, 1d z
            assert time.getDimension(2).equals(z.getDimension(0)) : "rank-3 time must have z inner dimension";
          //profileDim = time.getDimension(1);
          //zDim = time.getDimension(2);

        } else { // 2d time
          if (z.getRank() == 3) { // 2d time, 3d z
            assert z.getDimension(1).equals(time.getDimension(1)) : "rank-2 time must have time inner dimension";
            //profileDim = z.getDimension(1);
            //zDim = z.getDimension(2);
          } else { // 2d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            assert !time.getDimension(1).equals(z.getDimension(0)) : "time and z dimensions must be different";
            //profileDim = time.getDimension(1);
            //zDim = z.getDimension(0);
          }
        }

        // make profile table
        //   private TableConfig makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {

        TableConfig profileTable = makeMultidimInner(ds, stationTable, info.childDim, errlog);
        if (profileTable == null) return null;
        stationTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner3D(ds, stationTable, profileTable, info.grandChildDim, errlog);
        if (z.getRank() == 1) // join z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);
        break;
      }

      case raggedIndex: {
        //zDim = z.getDimension(0);

        Variable stationIndex = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_PARENTINDEX, info.parentDim.getName());
        if (stationIndex == null) {
          errlog.format("timeSeriesProfile stationIndex: must have a ragged_parentIndex variable with profile dimension%n");
          return null;
        }
        if (stationIndex.getRank() != 1) {
          errlog.format("timeSeriesProfile stationIndex: %s variable must be rank 1%n", stationIndex.getName());
          return null;
        }
        //profileDim = stationIndex.getDimension(0);

        Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, info.grandChildDim.getName());
        if (ragged_rowSize == null) {
          errlog.format("timeSeriesProfile numObs: must have a ragged_rowSize variable with profile dimension %s%n", info.childDim);
          return null;
        }
        if (ragged_rowSize.getRank() != 1) {
          errlog.format("timeSeriesProfile numObs: %s variable for observations must be rank 1%n", ragged_rowSize.getName());
          return null;
        }
        if (info.childDim.equals(info.grandChildDim)) {
          errlog.format("timeSeriesProfile profile dimension %s cannot be obs dimension %s%n", info.childDim, info.grandChildDim);
          return null;
        }

        TableConfig profileTable = makeRaggedIndex(ds, info.parentDim, info.childDim, errlog);
        stationTable.addChild(profileTable);
        TableConfig zTable = makeRaggedContiguous(ds, info.childDim, info.grandChildDim, errlog);
        profileTable.addChild(zTable);
        break;
      }

      case raggedContiguous:   // NOT USED
          throw new UnsupportedOperationException("CFpointObs: profile raggedContiguous encoding");

        /*
      case flat:
        //profileDim = time.getDimension(0); // may be time(profile) or time(profile, z)
        Variable parentId = identifyParent(ds, CF.FeatureType.timeSeriesProfile);

        TableConfig profileTable = makeStructTable(ds, FeatureType.PROFILE, info, errlog);
        profileTable.parentIndex = parentId.getName();
        profileTable.stnId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ID, info.childDim, errlog);
        profileTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_DESC, info.childDim, errlog);
        profileTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_WMOID, info.childDim, errlog);
        profileTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ALTITUDE, info.childDim, errlog);
        stationTable.addChild(profileTable);

        //zDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)
        TableConfig zTable = makeMultidimInner(ds, profileTable, info.grandChildDim, errlog);
        if (z.getRank() == 1) // z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);

        break; */
    }

    return stationTable;
  }

  private TableConfig getSectionConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncodingSection(ds, CF.FeatureType.trajectoryProfile, errlog);
    if (info == null) return null;

    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() == 0) {
      errlog.format("section cannot have a scalar time coordinate%n");
      return null;
    }

    /* if (info.encoding == Encoding.single) {
      Dimension profileDim = time.getDimension(0); // may be time(profile) or time(profile, z)
      Variable parentId = identifyParent(ds, CF.FeatureType.trajectoryProfile);
      if ((parentId != null) && (parentId.getRank() == 1) && (parentId.getDimension(0).equals(profileDim))) {
        info = new EncodingInfo(Encoding.flat, parentId);
      }
    } */

    TableConfig parentTable = makeStructTable(ds, FeatureType.SECTION, info, errlog);
    if (parentTable == null) return null;
    parentTable.feature_id = identifyParentId(ds, CF.FeatureType.trajectoryProfile);
    if (parentTable.feature_id == null) {
      errlog.format("getSectionConfig cant find a section id %n");
    }

    //Dimension sectionDim = ds.findDimension(parentTable.dimName);
    //Dimension profileDim = null;
    //Dimension zDim = null;

    // find the non-station altitude
    VariableDS z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("section must have a z coordinate%n");
      return null;
    }
    if (z.getRank() == 0) {
      errlog.format("section cannot have a scalar z coordinate%n");
      return null;
    }

    switch (info.encoding) {
      case single: {
        assert ((time.getRank() >= 1) && (time.getRank() <= 2)) : "time must be rank 1 or 2";
        assert ((z.getRank() >= 1) && (z.getRank() <= 2)) : "z must be rank 1 or 2";

        if (time.getRank() == 2) {
          if (z.getRank() == 2)  // 2d time, 2d z
            assert time.getDimensions().equals(z.getDimensions()) : "rank-2 time and z dimensions must be the same";
          else  // 2d time, 1d z
            assert time.getDimension(1).equals(z.getDimension(0)) : "rank-2 time must have z inner dimension";
          //profileDim = time.getDimension(0);
          //zDim = time.getDimension(1);

        } else { // 1d time
          if (z.getRank() == 2) { // 1d time, 2d z
            assert z.getDimension(0).equals(time.getDimension(0)) : "rank-2 z must have time outer dimension";
            //profileDim = z.getDimension(0);
            //zDim = z.getDimension(1);
          } else { // 1d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            //profileDim = time.getDimension(0);
            //zDim = z.getDimension(0);
          }
        }
        // make profile table
        TableConfig profileTable = makeStructTable(ds, FeatureType.PROFILE, new EncodingInfo(Encoding.multidim, info.childDim), errlog);
        if (profileTable == null) return null;
        if (time.getRank() == 1) // join time(time)
          profileTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));
        parentTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner(ds, profileTable, info.grandChildDim, errlog);
        if (z.getRank() == 1) // join z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);

        break;
      }

      case multidim: {
        assert ((time.getRank() >= 2) && (time.getRank() <= 3)) : "time must be rank 2 or 3";
        assert ((z.getRank() == 1) || (z.getRank() == 3)) : "z must be rank 1 or 3";

        if (time.getRank() == 3) {
          if (z.getRank() == 3)  // 3d time, 3d z
            assert time.getDimensions().equals(z.getDimensions()) : "rank-3 time and z dimensions must be the same";
          else  // 3d time, 1d z
            assert time.getDimension(2).equals(z.getDimension(0)) : "rank-3 time must have z inner dimension";
          //profileDim = time.getDimension(1);
          //zDim = time.getDimension(2);

        } else { // 2d time
          if (z.getRank() == 3) { // 2d time, 3d z
            assert z.getDimension(1).equals(time.getDimension(1)) : "rank-2 time must have time inner dimension";
           // profileDim = z.getDimension(1);
            //zDim = z.getDimension(2);
          } else { // 2d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            assert !time.getDimension(1).equals(z.getDimension(0)) : "time and z dimensions must be different";
            //profileDim = time.getDimension(1);
            //zDim = z.getDimension(0);
          }
        }

        // make profile table
        //   private TableConfig makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {

        TableConfig profileTable = makeMultidimInner(ds, parentTable, info.childDim, errlog);
        if (profileTable == null) return null;
        profileTable.feature_id = identifyParentId(ds, CF.FeatureType.profile);
        parentTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner3D(ds, parentTable, profileTable, info.grandChildDim, errlog);
        if (z.getRank() == 1) // join z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);
        break;
      }

      case raggedIndex: {
        //zDim = z.getDimension(0);

        Variable sectionIndex = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_PARENTINDEX, info.parentDim.getName());
        //Variable sectionIndex = findVariableWithStandardNameAndNotDimension(ds, CF.RAGGED_PARENTINDEX, info.parentDim, errlog);
        if (sectionIndex == null) {
          errlog.format("section sectionIndex: must have a ragged_parentIndex variable with profile dimension%n");
          return null;
        }
        if (sectionIndex.getRank() != 1) {
          errlog.format("section sectionIndex: %s variable must be rank 1%n", sectionIndex.getName());
          return null;
        }
        //profileDim = sectionIndex.getDimension(0);

        Variable numObs = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, info.grandChildDim.getName());
        //Variable numObs = findVariableWithStandardNameAndDimension(ds, CF.RAGGED_ROWSIZE, info.childDim, errlog);
        if (numObs == null) {
          errlog.format("section numObs: must have a ragged_rowSize variable with profile dimension %s%n", info.childDim);
          return null;
        }
        if (numObs.getRank() != 1) {
          errlog.format("section numObs: %s variable for observations must be rank 1%n", numObs.getName());
          return null;
        }
        if (info.childDim.equals(info.grandChildDim)) {
          errlog.format("section profile dimension %s cannot be obs dimension %s%n", info.childDim, info.grandChildDim);
          return null;
        }

        TableConfig profileTable = makeRaggedIndex(ds, info.parentDim, info.childDim, errlog);
        parentTable.addChild(profileTable);
        TableConfig zTable = makeRaggedContiguous(ds, info.childDim, info.grandChildDim, errlog);
        profileTable.addChild(zTable);
        break;
      }

      case raggedContiguous: {
        throw new UnsupportedOperationException("CFpointObs: section raggedContiguous encoding%n");
      }

      /*
      case flat:
        parentTable.type = Table.Type.Construct; // override default
        profileDim = time.getDimension(0); // may be time(profile) or time(profile, z)
        Variable parentId = identifyParent(ds, CF.FeatureType.trajectoryProfile);

        TableConfig profileTable = makeStructTable(ds, FeatureType.SECTION, info, errlog);
        profileTable.parentIndex = parentId.getName();
        profileTable.feature_id = identifyParentId(ds, CF.FeatureType.profile);
        parentTable.addChild(profileTable);

        zDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)
        TableConfig zTable = makeMultidimInner(ds, profileTable, zDim, errlog);
        if (z.getRank() == 1) // z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);

        break;  */
    }

    return parentTable;
  }

  /////////////////////////////////////////////////////////////////////

  private class EncodingInfo {
    Encoding encoding;
    Dimension parentDim, childDim, grandChildDim;

    EncodingInfo(Encoding encoding, Dimension parentDim) {
      this.encoding = encoding;
      this.parentDim = parentDim;
    }

    EncodingInfo(Encoding encoding, Dimension parentDim, Dimension childDim) {
      this.encoding = encoding;
      this.parentDim = parentDim;
      this.childDim = childDim;
    }

    EncodingInfo(Encoding encoding, Dimension parentDim, Dimension childDim, Dimension grandChildDim) {
      this.encoding = encoding;
      this.parentDim = parentDim;
      this.childDim = childDim;
      this.grandChildDim = grandChildDim;
    }

    EncodingInfo(Encoding encoding, Variable v) {
      this.encoding = encoding;
      this.parentDim = (v == null) ? null : v.getDimension(0);
    }
  }

  // given the feature type, figure out the encoding

  private EncodingInfo identifyEncoding(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    Variable ragged_rowSize = Evaluator.getVariableWithAttribute(ds, CF.RAGGED_ROWSIZE);
    if (ragged_rowSize != null) {
      if (ftype == CF.FeatureType.trajectoryProfile) {
        Variable parentId = identifyParent(ds, ftype);
        if (parentId == null) {
          errlog.format("Section ragged must have section_id variable%n");
          return null;
        }
        return new EncodingInfo(Encoding.raggedContiguous, parentId);
      }
      return new EncodingInfo(Encoding.raggedContiguous, ragged_rowSize);
    }

    Variable ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, CF.RAGGED_PARENTINDEX);
    if (ragged_parentIndex != null) {
      Variable ragged_parentId = identifyParent(ds, ftype);
      return new EncodingInfo(Encoding.raggedIndex, ragged_parentId);
    }

    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate%n");
      return null;
    }

    switch (ftype) {
      case point:
        return new EncodingInfo(Encoding.multidim, (Dimension) null);

      case timeSeries:
      case profile:
      case timeSeriesProfile:
        if (lat.getRank() == 0)
          return new EncodingInfo(Encoding.single, (Dimension) null);
        else if (lat.getRank() == 1)
          return new EncodingInfo(Encoding.multidim, lat);

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 0 or 1%n", ftype);
        return null;

      case trajectory:
      case trajectoryProfile:
        if (lat.getRank() == 1)
          return new EncodingInfo(Encoding.single, (Dimension) null);
        else if (lat.getRank() == 2)
          return new EncodingInfo(Encoding.multidim, lat);

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 1 or 2%n", ftype);
        return null;
    }

    return null;
  }

  // given the feature type, figure out the encoding

  private EncodingInfo identifyEncodingStation(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    // find the obs dimension
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate%n");
      return null;
    }
    Dimension obsDim = null;
    if (time.getRank() > 0)
      obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(stn, obs)
    else if (time.getParentStructure() != null) {
      Structure parent = time.getParentStructure(); // if time axis is a structure member, try pulling dimension out of parent structure
      obsDim = parent.getDimension(parent.getRank() - 1);
    }
    if (obsDim == null) {
      errlog.format("Must have a non-scalar Time coordinate%n");
      return null;
    }

    // station dimension
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate%n");
      return null;
    }
    if (lat.getRank() == 0)
      return new EncodingInfo(Encoding.single, (Dimension) null, obsDim);
    Dimension stnDim = lat.getDimension(0);

    if (obsDim == stnDim)
      return new EncodingInfo(Encoding.flat, (Dimension) null, obsDim);

    // the raggeds
    Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, obsDim.getName());
    if (ragged_rowSize != null) {
      return new EncodingInfo(Encoding.raggedContiguous, stnDim, obsDim);
    }
    Variable ragged_parentIndex = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_PARENTINDEX, stnDim.getName());
    if (ragged_parentIndex != null) {
      return new EncodingInfo(Encoding.raggedIndex, stnDim, obsDim);
    }

    if (lat.getRank() == 1)
      return new EncodingInfo(Encoding.multidim, stnDim, obsDim);

    errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 0 or 1%n", ftype);
    return null;
  }

  private EncodingInfo identifyEncodingTraj(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    // find the obs dimension
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate%n");
      return null;
    }
    Dimension obsDim = null;
    if (time.getRank() > 0)
      obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(stn, obs)
    else if (time.getParentStructure() != null) {
      Structure parent = time.getParentStructure(); // if time axis is a structure member, try pulling dimension out of parent structure
      obsDim = parent.getDimension(parent.getRank() - 1);
    }
    if (obsDim == null) {
      errlog.format("Must have a non-scalar Time coordinate%n");
      return null;
    }

    // parent dimension
    Dimension parentDim = null;
    if (time.getRank() > 1) {
      parentDim = time.getDimension(0);
      return new EncodingInfo(Encoding.multidim, parentDim, obsDim);
    }

    // the raggeds
    String dimName = Evaluator.getVariableAttributeValue(ds, CF.RAGGED_PARENTINDEX);
    if (dimName != null) {
      parentDim = ds.findDimension(dimName);
      if (parentDim != null)
        return new EncodingInfo(Encoding.raggedIndex, parentDim, obsDim);
      else {
        errlog.format("CFpointObs %s ragged_parent_index must name parent dimension%n", ftype);
        return null;
      }
    }

    Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, obsDim.getName());
    if (ragged_rowSize != null) {
      parentDim = ragged_rowSize.getDimension(0);
      return new EncodingInfo(Encoding.raggedContiguous, parentDim, obsDim);
    }

    return new EncodingInfo(Encoding.single, null, obsDim);
  }

  private EncodingInfo identifyEncodingProfile(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    // find the obs dimension
    VariableDS z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (z == null) {
      errlog.format("Must have a Height coordinate%n");
      return null;
    }
    Dimension obsDim = null;
    if (z.getRank() > 0)
      obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or alt(profile, z)
    else if (z.getParentStructure() != null) {
      Structure parent = z.getParentStructure(); // if time axis is a structure member, try pulling dimension out of parent structure
      obsDim = parent.getDimension(parent.getRank() - 1);
    }
    if (obsDim == null) {
      errlog.format("Must have a non-scalar Height coordinate%n");
      return null;
    }

    // parent dimension
    Dimension parentDim = null;
    if (z.getRank() > 1) {
      parentDim = z.getDimension(0);
      return new EncodingInfo(Encoding.multidim, parentDim, obsDim);
    }

    // the raggeds
    String dimName = Evaluator.getVariableAttributeValue(ds, CF.RAGGED_PARENTINDEX);
    if (dimName != null) {
      parentDim = ds.findDimension(dimName);
      if (parentDim != null)
        return new EncodingInfo(Encoding.raggedIndex, parentDim, obsDim);
      else {
        errlog.format("CFpointObs %s ragged_parent_index must name parent dimension%n", ftype);
        return null;
      }
    }

    Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, obsDim.getName());
    if (ragged_rowSize != null) {
      parentDim = ragged_rowSize.getDimension(0);
      return new EncodingInfo(Encoding.raggedContiguous, parentDim, obsDim);
    }

    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate%n");
      return null;
    }
    if ((time.getRank() == 0) || (time.getDimension(0) == obsDim))
      return new EncodingInfo(Encoding.single, null, obsDim);

    parentDim = time.getDimension(0);
    return new EncodingInfo(Encoding.multidim, parentDim, obsDim);
  }

  private EncodingInfo identifyEncodingSection(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    // find the z dimension
    VariableDS z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (z == null) {
      errlog.format("Must have a Height coordinate%n");
      return null;
    }
    Dimension obsDim = null;
    if (z.getRank() > 0)
      obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or alt(profile, z)
    else if (z.getParentStructure() != null) {
      Structure parent = z.getParentStructure(); // if time axis is a structure member, try pulling dimension out of parent structure
      obsDim = parent.getDimension(parent.getRank() - 1);
    }
    if (obsDim == null) {
      errlog.format("Must have a non-scalar Height coordinate%n");
      return null;
    }

    // parent dimension
    Dimension trajDim = null;
    Dimension profileDim = null;
    if (z.getRank() > 2) {
      trajDim = z.getDimension(0);
      profileDim = z.getDimension(1);
      return new EncodingInfo(Encoding.multidim, trajDim, profileDim, obsDim);
    }

    // ragged
    String dimName = Evaluator.getVariableAttributeValue(ds, CF.RAGGED_PARENTINDEX);
    if (dimName != null) {
      trajDim = ds.findDimension(dimName);
      if (trajDim != null) {
        Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, obsDim.getName());
        if (ragged_rowSize != null) {
          profileDim = ragged_rowSize.getDimension(0);
          return new EncodingInfo(Encoding.raggedIndex, trajDim, profileDim, obsDim);
        } else {
          errlog.format("CFpointObs %s ragged_row_count must name obs dimension%n", ftype);
          return null;
        }
      } else {
        errlog.format("CFpointObs %s ragged_parent_index must name station dimension%n", ftype);
        return null;
      }
    }

    VariableDS lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Lat coordinate%n");
      return null;
    }
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate%n");
      return null;
    }
    if (lat.getRank() == 0) {
      errlog.format("CFpointObs %s may not have scalar lat/lon%n", ftype);
      return null;
    }

    if (time.getRank() > 2) {
      trajDim = time.getDimension(0);
      profileDim = time.getDimension(1);
      return new EncodingInfo(Encoding.multidim, trajDim, profileDim, obsDim);
    }

    if (lat.getRank() == 1) {
      profileDim = lat.getDimension(0);
      return new EncodingInfo(Encoding.single, null, profileDim, obsDim);
    }

    if (lat.getRank() == 2) {
      trajDim = lat.getDimension(0);
      profileDim = lat.getDimension(0);
      return new EncodingInfo(Encoding.multidim, trajDim, profileDim, obsDim);
    }

    // forget flat for now
    errlog.format("CFpointObs %s unrecognized form%n", ftype);
    return null;
  }

  private EncodingInfo identifyEncodingTimeSeriesProfile(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    // find the non-station altitude
    VariableDS z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("timeSeriesProfile must have a z coordinate, not the station altitude%n");
      return null;
    }
    if (z.getRank() == 0) {
      errlog.format("timeSeriesProfile cannot have a scalar z coordinate%n");
      return null;
    }

    Dimension obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or alt(profile, z)

    // parent dimension
    Dimension stnDim = null;
    Dimension profileDim = null;
    if (z.getRank() > 2) {
      stnDim = z.getDimension(0);
      profileDim = z.getDimension(1);
      return new EncodingInfo(Encoding.multidim, stnDim, profileDim, obsDim);
    }

    // ragged
    String dimName = Evaluator.getVariableAttributeValue(ds, CF.RAGGED_PARENTINDEX);
    if (dimName != null) {
      stnDim = ds.findDimension(dimName);
      if (stnDim != null) {
        Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, obsDim.getName());
        if (ragged_rowSize != null) {
          profileDim = ragged_rowSize.getDimension(0);
          return new EncodingInfo(Encoding.raggedIndex, stnDim, profileDim, obsDim);
        } else {
          errlog.format("CFpointObs %s ragged_row_count must name obs dimension%n", ftype);
          return null;
        }
      } else {
        errlog.format("CFpointObs %s ragged_parent_index must name station dimension%n", ftype);
        return null;
      }
    }

    VariableDS lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Lat coordinate%n");
      return null;
    }
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate%n");
      return null;
    }
    if (lat.getRank() == 0) {
      profileDim = time.getDimension(0); // may be time(profile) or time(profile, z)
      return new EncodingInfo(Encoding.single, null, profileDim, obsDim);
    }

    if ((time.getRank() == 1) || (time.getRank() == 2 && time.getDimension(1) == obsDim)) {
      profileDim = time.getDimension(0); // may be time(profile) or time(profile, z)
      return new EncodingInfo(Encoding.flat, null, profileDim, obsDim);
    }

    if (time.getRank() > 1) {
      stnDim = time.getDimension(0); // may be time(station, profile) or time(station, profile, z)
      profileDim = time.getDimension(1);
      return new EncodingInfo(Encoding.multidim, stnDim, profileDim, obsDim);
    }

    errlog.format("CFpointObs %s unrecognized form%n", ftype);
    return null;
  }

  private String identifyParentId(NetcdfDataset ds, CF.FeatureType ftype) {
    Variable v = identifyParent(ds, ftype);
    return (v == null) ? null : v.getShortName();
  }

  private Variable identifyParent(NetcdfDataset ds, CF.FeatureType ftype) {
    switch (ftype) {
      case timeSeriesProfile:
      case timeSeries:
        return Evaluator.getVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.STATION_ID);
      case trajectory:
        return Evaluator.getVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.TRAJ_ID);
      case profile:
        return Evaluator.getVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.PROFILE_ID);
      case trajectoryProfile:
        return Evaluator.getVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.TRAJ_ID);
    }
    return null;
  }

  // for station and stationProfile, not flat

  private TableConfig makeStationTable(NetcdfDataset ds, FeatureType ftype, EncodingInfo info, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);

    //Dimension stationDim = (info.encoding == Encoding.single) ? null : lat.getDimension(0); // assumes outer dim of lat is parent dimension, single = scalar

    Table.Type stationTableType = Table.Type.Structure;
    if (info.encoding == Encoding.single) stationTableType = Table.Type.Top;
    if (info.encoding == Encoding.flat) stationTableType = Table.Type.Construct;

    Dimension stationDim = (info.encoding == Encoding.flat) ? info.childDim : info.parentDim;
    String name = (stationDim == null) ? " single" : stationDim.getName();
    TableConfig stnTable = new TableConfig(stationTableType, name);
    stnTable.featureType = ftype;
    stnTable.stnId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ID, stationDim, errlog);
    stnTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_DESC, stationDim, errlog);
    stnTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_WMOID, stationDim, errlog);
    stnTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ALTITUDE, stationDim, errlog);
    stnTable.lat = lat.getName();
    stnTable.lon = lon.getName();

    // station id
    if (stnTable.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id%n");
      return null;
    }

    if (info.encoding != Encoding.single) {
      // set up structure
      boolean hasStruct = Evaluator.hasRecordStructure(ds) && stationDim.isUnlimited();
      stnTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      stnTable.dimName = stationDim.getName();
      stnTable.structName = hasStruct ? "record" : stationDim.getName();
    }

    // LOOK probably need a standard name here
    // optional alt coord - detect if its a station height or actually associated with the obs, eg for a profile
    if (stnTable.stnAlt == null) {
      Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
      if (alt != null) {
        if ((info.encoding == Encoding.single) && alt.getRank() == 0)
          stnTable.stnAlt = alt.getName();

        if ((info.encoding != Encoding.single) && (lat.getRank() == alt.getRank()) && alt.getDimension(0).equals(stationDim))
          stnTable.stnAlt = alt.getName();
      }
    }

    return stnTable;
  }

  private TableConfig makeStructTable(NetcdfDataset ds, FeatureType ftype, EncodingInfo info, Formatter errlog) throws IOException {
    Table.Type tableType = Table.Type.Structure;
    if (info.encoding == Encoding.single) tableType = Table.Type.Top;
    if (info.encoding == Encoding.flat) tableType = Table.Type.ParentId;

    String name = (info.parentDim == null) ? " single" : info.parentDim.getName();
    TableConfig tableConfig = new TableConfig(tableType, name);
    tableConfig.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, info.parentDim);
    tableConfig.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, info.parentDim);
    tableConfig.elev = matchAxisTypeAndDimension(ds, AxisType.Height, info.parentDim);
    tableConfig.time = matchAxisTypeAndDimension(ds, AxisType.Time, info.parentDim);
    tableConfig.featureType = ftype;

    if (info.encoding != Encoding.single) {
      // set up structure
      boolean stnIsStruct = Evaluator.hasRecordStructure(ds) && info.parentDim.isUnlimited();
      tableConfig.structureType = stnIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      tableConfig.dimName = info.parentDim.getName();
      tableConfig.structName = stnIsStruct ? "record" : tableConfig.dimName;
    }

    return tableConfig;
  }

  /////////////////////////////////////////////////////////////////////////////////

  private TableConfig makeRaggedContiguous(NetcdfDataset ds, Dimension parentDim, Dimension childDim, Formatter errlog) throws IOException {
    TableConfig obsTable = new TableConfig(Table.Type.Contiguous, childDim.getName());
    obsTable.dimName = childDim.getName();

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, childDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, childDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, childDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, childDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && childDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : childDim.getName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;

    Variable ragged_rowSize = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_ROWSIZE, childDim.getName());
    if ((null == ragged_rowSize) || (ragged_rowSize.getRank() == 0) || (ragged_rowSize.getDimension(0).getName() != parentDim.getName())) {
      errlog.format("there must be a ragged_row_count variable with outer dimension that matches latitude/longitude dimension %s%n", parentDim.getName());
      return null;
    }
    obsTable.numRecords = ragged_rowSize.getName();

    return obsTable;
  }

  private TableConfig makeRaggedIndex(NetcdfDataset ds,  Dimension parentDim, Dimension childDim, Formatter errlog) throws IOException {
    TableConfig obsTable = new TableConfig(Table.Type.ParentIndex, childDim.getName());
    obsTable.dimName = childDim.getName();

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, childDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, childDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, childDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, childDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && childDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : childDim.getName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;

    Variable ragged_parentIndex = Evaluator.getVariableWithAttributeValue(ds, CF.RAGGED_PARENTINDEX, parentDim.getName());
    if ((null == ragged_parentIndex) || (ragged_parentIndex.getRank() == 0) || (ragged_parentIndex.getDimension(0).getName() != childDim.getName())) {
      errlog.format("there must be a ragged_parent_index variable with outer dimension that matches obs dimension %s%n", childDim.getName());
      return null;
    }
    obsTable.parentIndex = ragged_parentIndex.getName();

    return obsTable;
  }

  // the inner table of Structure(outer, inner) and middle table of Structure(outer, middle, inner)

  private TableConfig makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    Dimension parentDim = ds.findDimension(parentTable.dimName);

    Table.Type obsTableType = (parentTable.structureType == TableConfig.StructureType.PsuedoStructure) ? Table.Type.MultidimInnerPsuedo : Table.Type.MultidimInner;
    TableConfig obsTable = new TableConfig(obsTableType, obsDim.getName());

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, parentDim, obsDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, parentDim, obsDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, parentDim, obsDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, parentDim, obsDim);

    // divide up the variables between the parent and the obs
    List<String> obsVars = null;
    List<Variable> vars = ds.getVariables();
    List<String> parentVars = new ArrayList<String>(vars.size());
    obsVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(parentDim)) {
        if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
          parentVars.add(orgV.getShortName());
        } else {
          Dimension dim1 = orgV.getDimension(1);
          if ((dim1 != null) && dim1.equals(obsDim))
            obsVars.add(orgV.getShortName());
        }
      }
    }
    parentTable.vars = parentVars;
    // parentTable.vars = parentTable.isPsuedoStructure ? parentVars : null; // restrict to these if psuedoStruct

    obsTable.structureType = parentTable.structureType;
    obsTable.outerName = parentDim.getName();
    obsTable.innerName = obsDim.getName();
    obsTable.dimName = (parentTable.structureType == TableConfig.StructureType.PsuedoStructure) ? obsTable.outerName : obsTable.innerName;
    obsTable.structName = obsDim.getName();
    obsTable.vars = obsVars;

    return obsTable;
  }

  // the inner table of Structure(outer, middle, inner)

  private TableConfig makeMultidimInner3D(NetcdfDataset ds, TableConfig outerTable, TableConfig middleTable, Dimension innerDim, Formatter errlog) throws IOException {
    Dimension outerDim = ds.findDimension(outerTable.dimName);
    Dimension middleDim = ds.findDimension(middleTable.innerName);

    Table.Type obsTableType = (outerTable.structureType == TableConfig.StructureType.PsuedoStructure) ? Table.Type.MultidimInnerPsuedo3D : Table.Type.MultidimInner3D;
    TableConfig obsTable = new TableConfig(obsTableType, innerDim.getName());
    obsTable.structureType = TableConfig.StructureType.PsuedoStructure2D;
    obsTable.dimName = outerTable.dimName;
    obsTable.outerName = middleTable.innerName;
    obsTable.innerName = innerDim.getName();
    obsTable.structName = innerDim.getName();

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, outerDim, middleDim, innerDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, outerDim, middleDim, innerDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, outerDim, middleDim, innerDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, outerDim, middleDim, innerDim);

    // divide up the variables between the 3 tables
    List<Variable> vars = ds.getVariables();
    List<String> outerVars = new ArrayList<String>(vars.size());
    List<String> middleVars = new ArrayList<String>(vars.size());
    List<String> innerVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
        if (outerDim.equals(orgV.getDimension(0)))
          outerVars.add(orgV.getShortName());

      } else if (orgV.getRank() == 2) {
        if (outerDim.equals(orgV.getDimension(0)) && middleDim.equals(orgV.getDimension(1)))
          middleVars.add(orgV.getShortName());

      } else if (orgV.getRank() == 3) {
        if (outerDim.equals(orgV.getDimension(0)) && middleDim.equals(orgV.getDimension(1)) && innerDim.equals(orgV.getDimension(2)))
          innerVars.add(orgV.getShortName());
      }
    }
    outerTable.vars = outerVars;
    middleTable.vars = middleVars;
    obsTable.vars = innerVars;

    return obsTable;
  }


  private TableConfig makeSingle(NetcdfDataset ds, Dimension obsDim, Formatter errlog) throws IOException {

    Table.Type obsTableType = Table.Type.Structure;
    TableConfig obsTable = new TableConfig(obsTableType, "single");
    obsTable.dimName = obsDim.getName();

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, obsDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;

    return obsTable;
  }

  private TableConfig makeMiddleTable(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    throw new UnsupportedOperationException("CFpointObs: middleTable encoding");
  }

  // Adds check for dimensions against parent structure if applicable...
  //
  // Note to John.  It may be that this implementation can be pushed into the super
  // class, I don't unserstand enough of the code base to anticipate implementation artifacts.

  @Override
  protected String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        if ((outer == null) && (axis.getRank() == 0))
          return true;
        if ((outer != null) && (axis.getRank() == 1) && (outer.equals(axis.getDimension(0))))
          return true;

        // if axis is structure member, try pulling dimension out of parent structure
        if (axis.getParentStructure() != null) {
          Structure parent = axis.getParentStructure();
          if ((outer != null) && (parent.getRank() == 1) && (outer.equals(parent.getDimension(0))))
            return true;
        }
        return false;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

}
