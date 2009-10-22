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

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.point.standard.CoordSysEvaluator;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.Array;
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
  private static String STANDARD_NAME = "standard_name";
  private static String RAGGED_ROWSIZE = "ragged_rowSize";
  private static String RAGGED_PARENTINDEX = "ragged_parentIndex";
  private static String STATION_ID = "station_id";
  private static String STATION_DESC = "station_desc";
  private static String STATION_ALTITUDE = "station_altitude";
  private static String STATION_WMOID = "station_wmoid";

  // ??
  private static String TRAJ_ID = "trajectory_id";
  private static String PROFILE_ID = "profile_id";

  private enum Encoding {
    single, multidim, raggedContiguous, raggedIndex, flat
  }

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    // find datatype
    String datatype = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    if (datatype == null)
      datatype = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt2, null);
    if (datatype == null)
      datatype = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt3, null);
    if (datatype == null)
      return false;

    if (CF.FeatureType.valueOf(datatype) == null)
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      //if (toke.startsWith("CF-1.0"))               LOOK also taking 1.0 ???
      //  return false;  // let default analyser try
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
      ftype = CF.FeatureType.point;  // ?? wantFeatureType ??
    else {
      try {
        ftype = CF.FeatureType.valueOf(ftypeS);
      } catch (Throwable t) {
        if (ftypeS.equalsIgnoreCase("stationProfileTimeSeries"))
          ftype = CF.FeatureType.stationProfile;
        else if (ftypeS.equalsIgnoreCase("station"))
          ftype = CF.FeatureType.stationTimeSeries;
        else
          ftype = CF.FeatureType.point; // ?? error ??
      }
    }

    // make sure lat, lon, time coordinates exist
    if (!checkCoordinates(ds, errlog)) return null;

    switch (ftype) {
      case point:
        return getPointConfig(ds, errlog);
      case stationTimeSeries:
        return getStationConfig(ds, errlog);
      case profile:
        return getProfileConfig(ds, errlog);
      case trajectory:
        return getTrajectoryConfig(ds, errlog);
      case stationProfile:
        return getStationProfileConfig(ds, errlog);
      case section:
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
    EncodingInfo info = identifyEncoding(ds, CF.FeatureType.stationTimeSeries, errlog);
    if (info == null) return null;

    TableConfig stnTable = makeStationTable(ds, FeatureType.STATION, info, errlog);
    if (stnTable == null) return null;

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(stn, obs)

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
        obsTable = makeRaggedContiguous(ds, stnTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsTable = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: stationTimeSeries flat encoding");
    }
    if (obsTable == null) return null;

    stnTable.addChild(obsTable);
    return stnTable;
  }

  ////
  private TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncoding(ds, CF.FeatureType.profile, errlog);
    if (info == null) return null;

    TableConfig parentTable = makeParentTable(ds, FeatureType.PROFILE, info, errlog);
    if (parentTable == null) return null;

    // obs table
    Variable z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (z == null) {
      errlog.format("CFpointObs cant find a Height coordinate %n");
      return null;
    }
    Dimension obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)

    TableConfig obsTable = null;
    switch (info.encoding) {
      case single:
        obsTable = makeSingle(ds, obsDim, errlog);
        break;
      case multidim:
        obsTable = makeMultidimInner(ds, parentTable, obsDim, errlog);
        if (z.getRank() == 1) // z(z)
          obsTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        break;
      case raggedContiguous:
        obsTable = makeRaggedContiguous(ds, parentTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsTable = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: profile flat encoding");
    }
    if (obsTable == null) return null;

    parentTable.addChild(obsTable);
    return parentTable;
  }

  ////
  private TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncoding(ds, CF.FeatureType.trajectory, errlog);
    if (info == null) return null;

    TableConfig parentTable = makeParentTable(ds, FeatureType.TRAJECTORY, info, errlog);
    if (parentTable == null) return null;

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(traj, obs)

    TableConfig obsConfig = null;
    switch (info.encoding) {
      case single:
        obsConfig = makeSingle(ds, obsDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidimInner(ds, parentTable, obsDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, parentTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: trajectory flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  ////
  private TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = identifyEncoding(ds, CF.FeatureType.stationProfile, errlog);
    if (info == null) return null;

    TableConfig stationTable = makeStationTable(ds, FeatureType.STATION_PROFILE, info, errlog);
    if (stationTable == null) return null;

    Dimension stationDim = stationTable.dim;
    Dimension profileDim = null;
    Dimension zDim = null;

    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() == 0) {
      errlog.format("stationProfile cannot have a scalar time coordinate%n");
      return null;
    }

    // find the non-station altitude
    Variable z = findZAxisNotStationAlt(ds);
    if (z == null) {
      errlog.format("stationProfile must have a z coordinate%n");
      return null;
    }
    if (z.getRank() == 0) {
      errlog.format("stationProfile cannot have a scalar z coordinate%n");
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
          profileDim = time.getDimension(0);
          zDim = time.getDimension(1);

        } else { // 1d time
          if (z.getRank() == 2) { // 1d time, 2d z
            assert z.getDimension(0).equals(time.getDimension(0)) : "rank-2 z must have time outer dimension";
            profileDim = z.getDimension(0);
            zDim = z.getDimension(1);
          } else { // 1d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            profileDim = time.getDimension(0);
            zDim = z.getDimension(0);
          }
        }
        // make profile table
        TableConfig profileTable = makeParentTable(ds, FeatureType.PROFILE, new EncodingInfo(Encoding.multidim, profileDim), errlog);
        if (profileTable == null) return null;
        if (time.getRank() == 1) // join time(time)
          profileTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));
        stationTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner(ds, profileTable, zDim, errlog);
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
          profileDim = time.getDimension(1);
          zDim = time.getDimension(2);

        } else { // 2d time
          if (z.getRank() == 3) { // 2d time, 3d z
            assert z.getDimension(1).equals(time.getDimension(1)) : "rank-2 time must have time inner dimension";
            profileDim = z.getDimension(1);
            zDim = z.getDimension(2);
          } else { // 2d time, 1d z
            assert !time.getDimension(0).equals(z.getDimension(0)) : "time and z dimensions must be different";
            assert !time.getDimension(1).equals(z.getDimension(0)) : "time and z dimensions must be different";
            profileDim = time.getDimension(1);
            zDim = z.getDimension(0);
          }
        }

        // make profile table
        //   private TableConfig makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {

        TableConfig profileTable = makeMultidimInner(ds, stationTable, profileDim, errlog);
        if (profileTable == null) return null;
        stationTable.addChild(profileTable);

        // make the inner (z) table
        TableConfig zTable = makeMultidimInner3D(ds, stationTable, profileTable, zDim, errlog);
        if (z.getRank() == 1) // join z(z)
          zTable.addJoin(new JoinArray(z, JoinArray.Type.raw, 0));
        profileTable.addChild(zTable);
        break;
      }

      case raggedContiguous: {
        zDim = z.getDimension(0);

        Variable numProfiles = findVariableWithStandardNameAndDimension(ds, RAGGED_ROWSIZE, stationDim, errlog);
        if (numProfiles == null) {
          errlog.format("stationProfile numProfiles: must have a ragged_rowSize variable with station dimension %s%n", stationDim);
          return null;
        }
        if (numProfiles.getRank() != 1) {
          errlog.format("stationProfile numProfiles: %s variable must be rank 1%n", numProfiles.getName());
          return null;
        }

        Variable numObs = findVariableWithStandardNameAndNotDimension(ds, RAGGED_ROWSIZE, stationDim, errlog);
        if (numObs == null) {
          errlog.format("stationProfile numObs: must have a ragged_rowSize variable for observations%n");
          return null;
        }
        if (numObs.getRank() != 1) {
          errlog.format("stationProfile numObs: %s variable for observations must be rank 1%n", numObs.getName());
          return null;
        }
        profileDim = numObs.getDimension(0);

        if (profileDim.equals(zDim)) {
          errlog.format("stationProfile numObs (%s) must have profile dimension, not obs dimension %s%n", numObs.getNameAndDimensions(), zDim);
          return null;
        }

        TableConfig profileTable = makeMiddleTable(ds, stationTable, profileDim, errlog);
        stationTable.addChild(profileTable);
        TableConfig zTable = makeMultidimInner(ds, stationTable, zDim, errlog);
        profileTable.addChild(zTable);
        break;
      }

      case raggedIndex: {
        zDim = z.getDimension(0);

        if (time.getRank() != 1) {
          errlog.format("stationProfile raggedIndex time coordinate %s have rank 1 time%n", time);
          return null;
        }

        Variable profileIndex = findVariableWithStandardNameAndDimension(ds, RAGGED_PARENTINDEX, zDim, errlog);
        if (profileIndex == null) {
          errlog.format("stationProfile raggedIndex must have a ragged_rowSize variable for observations%n");
          return null;
        }
        if (profileIndex.getRank() != 1) {
          errlog.format("stationProfile ragged_parentIndex %s variable for observations must be rank 1%n", profileIndex.getName());
          return null;
        }
        profileDim = profileIndex.getDimension(0);

        Variable stationIndex = findVariableWithStandardNameAndNotDimension(ds, RAGGED_PARENTINDEX, zDim, errlog);
        if (stationIndex == null) {
          errlog.format("stationProfile raggedIndex must have a ragged_parentIndex for profiles with dimension %s%n", stationDim);
          return null;
        }
        if (stationIndex.getRank() != 1) {
          errlog.format("stationProfile ragged_parentIndex %s variable must be rank 1%n", stationIndex.getName());
          return null;
        }
        TableConfig profileTable = makeMiddleTable(ds, stationTable, profileDim, errlog);
        stationTable.addChild(profileTable);
        TableConfig zTable = makeMultidimInner(ds, stationTable, zDim, errlog);
        profileTable.addChild(zTable);
        break;
      }

      case flat:
        throw new UnsupportedOperationException("CFpointObs: stationProfile flat encoding");
    }

    return stationTable;
  }

  private TableConfig getSectionConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }

  /////////////////////////////////////////////////////////////////////

  private class EncodingInfo {
    Encoding encoding;
    Dimension parentDim;

    EncodingInfo(Encoding encoding, Dimension parentDim) {
      this.encoding = encoding;
      this.parentDim = parentDim;
    }

    EncodingInfo(Encoding encoding, Variable v) {
      this.encoding = encoding;
      this.parentDim = (v == null) ? null : v.getDimension(0);
    }
  }

  private EncodingInfo identifyEncoding(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    Variable ragged_rowSize = Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, RAGGED_ROWSIZE);
    if (ragged_rowSize != null)
      return new EncodingInfo(Encoding.raggedContiguous, ragged_rowSize);

    Variable ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, RAGGED_PARENTINDEX);
    if (ragged_parentIndex != null) {
      Variable ragged_parentId = identifyParent(ds, ftype);
      return new EncodingInfo(Encoding.raggedIndex, ragged_parentId);
    }

    Variable ragged_parentId = Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, "parentId");
    if (ragged_parentId != null)
      return new EncodingInfo(Encoding.flat, ragged_parentIndex);

    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate");
      return null;
    }

    switch (ftype) {
      case point:
        return new EncodingInfo(Encoding.multidim, (Dimension) null);

      case stationTimeSeries:
      case profile:
      case stationProfile:
        if (lat.getRank() == 0)
          return new EncodingInfo(Encoding.single, (Dimension) null);
        else if (lat.getRank() == 1)
          return new EncodingInfo(Encoding.multidim, lat);

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 0 or 1", ftype);
        return null;

      case trajectory:
      case section:
        if (lat.getRank() == 1)
          return new EncodingInfo(Encoding.single, (Dimension) null);
        else if (lat.getRank() == 2)
          return new EncodingInfo(Encoding.multidim, lat);

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 1 or 2", ftype);
        return null;
    }

    return null;
  }

  private Variable identifyParent(NetcdfDataset ds, CF.FeatureType ftype) {
    switch (ftype) {
      case stationTimeSeries:
        return Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, STATION_ID);
      case trajectory:
        return Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, TRAJ_ID);
      case profile:
        return Evaluator.getVariableWithAttribute(ds, STANDARD_NAME, PROFILE_ID);
    }
    return null;
  }

  // for station and stationProfile, not flat
  private TableConfig makeStationTable(NetcdfDataset ds, FeatureType ftype, EncodingInfo info, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    Dimension stationDim = (info.encoding == Encoding.single) ? null : lat.getDimension(0); // assumes outer dim of lat is parent dimension, single = scalar

    Table.Type stationTableType = (info.encoding == Encoding.single) ? Table.Type.Top : Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = ftype;
    stnTable.stnId = findNameVariableWithStandardNameAndDimension(ds, STATION_ID, stationDim, errlog);
    stnTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, STATION_DESC, stationDim, errlog);
    stnTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, STATION_WMOID, stationDim, errlog);
    stnTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, STATION_ALTITUDE, stationDim, errlog);
    stnTable.lat = lat.getName();
    stnTable.lon = lon.getName();

    if (info.encoding != Encoding.single) {
      // set up structure
      boolean hasStruct = Evaluator.hasRecordStructure(ds) && stationDim.isUnlimited();
      stnTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      stnTable.dim = stationDim;
      stnTable.structName = hasStruct ? "record" : stationDim.getName();

      // station id
      if (stnTable.stnId == null) {
        errlog.format("Must have a Station id variable with standard name station_id");  // why ??
        return null;
      }
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

  private TableConfig makeParentTable(NetcdfDataset ds, FeatureType ftype, EncodingInfo info, Formatter errlog) throws IOException {
    Table.Type parentTableType = (info.encoding == Encoding.single) ? Table.Type.Top : Table.Type.Structure;
    TableConfig parentTable = new TableConfig(parentTableType, ftype.toString());
    parentTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, info.parentDim);
    parentTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, info.parentDim);
    parentTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, info.parentDim);
    parentTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, info.parentDim);
    parentTable.featureType = ftype;

    if (info.encoding != Encoding.single) {
      // set up structure
      boolean stnIsStruct = Evaluator.hasRecordStructure(ds) && info.parentDim.isUnlimited();
      parentTable.structureType = stnIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      parentTable.dim = info.parentDim;
      parentTable.structName = stnIsStruct ? "record" : info.parentDim.getName();
    }

    return parentTable;
  }

  /* the middle table of Structure(outer, middle, inner)
  private TableConfig makeMiddleTable(NetcdfDataset ds, FeatureType ftype, TableConfig parentTable, Dimension middle) throws IOException {
    Table.Type middleTableType = parentTable.isPsuedoStructure ? Table.Type.MultidimInnerPsuedo : Table.Type.MultidimInner;
    Dimension outer = parentTable.dim;

    TableConfig middleTable = new TableConfig(middleTableType, ftype.toString());
    middleTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, outer, middle);
    middleTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, outer, middle);
    middleTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, outer, middle);
    middleTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, outer, middle);
    middleTable.featureType = ftype;

    // set up structure
    boolean stnIsStruct = Evaluator.hasRecordStructure(ds) && outer.isUnlimited();
    middleTable.isPsuedoStructure = !stnIsStruct;
    middleTable.dim = outer;
    middleTable.outer = outer;
    middleTable.inner = middle;
    middleTable.structName = stnIsStruct ? "record" : outer.getName();

    return middleTable;
  }  */

  private String findNameVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    Variable v = findVariableWithStandardNameAndDimension(ds, standard_name, outer, errlog);
    return (v == null) ? null : v.getShortName();
  }

  private Variable findVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, STANDARD_NAME, null);
      if ((stdName != null) && stdName.equals(standard_name)) {
        if (v.getRank() > 0 && v.getDimension(0).equals(outer))
          return v;
        if ((v.getRank() == 0) && (outer == null))
          return v;
      }
    }
    return null;
  }

  private Variable findVariableWithStandardNameAndNotDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, STANDARD_NAME, null);
      if ((stdName != null) && stdName.equals(standard_name) && v.getRank() > 0 && !v.getDimension(0).equals(outer))
        return v;
    }
    return null;
  }

  private String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        if ((outer == null) && (axis.getRank() == 0))
          return true;
        if ((outer != null) && (axis.getRank() == 1) && (outer.equals(axis.getDimension(0))))
          return true;
        return false;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  private String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return ((axis.getRank() == 2) && outer.equals(axis.getDimension(0)) && inner.equals(axis.getDimension(1)));
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  private String matchAxisTypeAndDimension(NetcdfDataset ds, AxisType type, final Dimension outer, final Dimension middle, final Dimension inner) {
    Variable var = CoordSysEvaluator.findCoordByType(ds, type, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        boolean ok = ((axis.getRank() == 3) && outer.equals(axis.getDimension(0)) && middle.equals(axis.getDimension(1)) && inner.equals(axis.getDimension(2)));
        return ok;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  private CoordinateAxis findZAxisNotStationAlt(NetcdfDataset ds) {
    CoordinateAxis z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return null == axis.findAttribute(STATION_ALTITUDE); // does not have this attribute
      }
    });
    if (z != null) return z;

    z = CoordSysEvaluator.findCoordByType(ds, AxisType.Pressure, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return null == axis.findAttribute(STATION_ALTITUDE); // does not have this attribute
      }
    });
    return z;
  }


  /////////////////////////////////////////////////////////////////////////////////

  private TableConfig makeRaggedContiguous(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    TableConfig obsTable = new TableConfig(Table.Type.Contiguous, obsDim.getName());
    obsTable.dim = obsDim;

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, obsDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;

    obsTable.numRecords = findNameVariableWithStandardNameAndDimension(ds, RAGGED_ROWSIZE, parentTable.dim, errlog);
    if (null == obsTable.numRecords) {
      errlog.format("there must be a ragged_rowSize variable with outer dimension that matches latitude/longitude dimension %s%n", parentTable.dim);
      return null;
    }

    // read numRecords
    Variable v = ds.findVariable(obsTable.numRecords);
    Array numRecords = v.read();
    int n = (int) v.getSize();

    // construct the start variable
    obsTable.startIndex = new int[n];
    int i = 0;
    int count = 0;
    while (numRecords.hasNext()) {
      obsTable.startIndex[i++] = count;
      count += numRecords.nextLong();
    }

    return obsTable;
  }

  private TableConfig makeRaggedIndex(NetcdfDataset ds, Dimension obsDim, Formatter errlog) throws IOException {
    TableConfig obsTable = new TableConfig(Table.Type.ParentIndex, obsDim.getName());
    obsTable.dim = obsDim;

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, obsDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;

    obsTable.parentIndex = findNameVariableWithStandardNameAndDimension(ds, RAGGED_PARENTINDEX, obsDim, errlog);
    if (null == obsTable.parentIndex)
      return null;

    Variable rpIndex = ds.findVariable(obsTable.parentIndex);

    // construct the map
    Array index = rpIndex.read();
    int childIndex = 0;
    Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>((int) (2 * index.getSize()));
    while (index.hasNext()) {
      int parent = index.nextInt();
      List<Integer> list = map.get(parent);
      if (list == null) {
        list = new ArrayList<Integer>();
        map.put(parent, list);
      }
      list.add(childIndex);
      childIndex++;
    }
    obsTable.indexMap = map;

    return obsTable;
  }

  // the inner table of Structure(outer, inner) and middle table of Structure(outer, middle, inner)
  private TableConfig makeMultidimInner(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    Dimension parentDim = parentTable.dim;

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
    obsTable.outer = parentDim;
    obsTable.inner = obsDim;
    obsTable.dim = (parentTable.structureType == TableConfig.StructureType.PsuedoStructure) ? parentDim : obsDim;
    obsTable.structName = obsDim.getName();
    obsTable.vars = obsVars;

    return obsTable;
  }

  // the inner table of Structure(outer, middle, inner)
  private TableConfig makeMultidimInner3D(NetcdfDataset ds, TableConfig outerTable, TableConfig middleTable, Dimension innerDim, Formatter errlog) throws IOException {
    Dimension outerDim = outerTable.dim;
    Dimension middleDim = middleTable.inner;

    Table.Type obsTableType = (outerTable.structureType == TableConfig.StructureType.PsuedoStructure) ? Table.Type.MultidimInnerPsuedo3D : Table.Type.MultidimInner3D;
    TableConfig obsTable = new TableConfig(obsTableType, innerDim.getName());
    obsTable.structureType = TableConfig.StructureType.PsuedoStructure2D;
    obsTable.dim = outerDim;
    obsTable.outer = middleDim;
    obsTable.inner = innerDim;
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

      if (orgV.getRank() == 1) {
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
    TableConfig obsTable = new TableConfig(obsTableType, obsDim.getName());
    obsTable.dim = obsDim;

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

}
