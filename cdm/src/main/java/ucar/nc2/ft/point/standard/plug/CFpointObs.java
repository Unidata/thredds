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
 * @since Nov 3, 2008
 */
public class CFpointObs extends TableConfigurerImpl {

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

  private TableConfig getPointConfig(NetcdfDataset ds, Formatter errlog) {
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() != 1) {
      errlog.format("CFpointObs type=point: coord time must have rank 1, coord var= %s %n", time.getNameAndDimensions());
      return null;
    }
    Dimension obsDim = time.getDimension(0);
    boolean hasStruct = Evaluator.hasRecordStructure(ds);

    TableConfig obsTable = new TableConfig(Table.Type.Structure, obsDim.getName());
    obsTable.structName = hasStruct ? "record" : obsDim.getName();
    obsTable.isPsuedoStructure = !hasStruct;
    obsTable.dim = obsDim;
    obsTable.time = time.getName();
    obsTable.featureType = FeatureType.POINT;
    CoordSysEvaluator.findCoords(obsTable, ds);

    return obsTable;
  }

  private TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.stationTimeSeries, errlog);
    if (encoding == null) return null;

    TableConfig stnTable = makeStationTable(ds, FeatureType.STATION, encoding, errlog);
    if (stnTable == null) return null;

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(stn, obs)

    TableConfig obsConfig = null;
    switch (encoding) {
      case single:
        obsConfig = makeSingle(ds, obsDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, stnTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidim(ds, stnTable, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: stationTimeSeries flat encoding");
    }
    if (obsConfig == null) return null;

    stnTable.addChild(obsConfig);
    return stnTable;
  }

  private TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.profile, errlog);
    if (encoding == null) return null;

    TableConfig parentTable = makeParentTable(ds, FeatureType.PROFILE, encoding, errlog);
    if (parentTable == null) return null;

    // obs table
    Variable z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    Dimension obsDim = z.getDimension(z.getRank() - 1); // may be z(z) or z(profile, z)

    TableConfig obsConfig = null;
    switch (encoding) {
      case single:
        obsConfig = makeSingle(ds, obsDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, parentTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidim(ds, parentTable, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: profile flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  /////
  private TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.trajectory, errlog);
    if (encoding == null) return null;

    TableConfig parentTable = makeParentTable(ds, FeatureType.TRAJECTORY, encoding, errlog);
    if (parentTable == null) return null;

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(traj, obs)

    TableConfig obsConfig = null;
    switch (encoding) {
      case single:
        obsConfig = makeSingle(ds, obsDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, parentTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidim(ds, parentTable, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs: trajectory flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  /////
  private TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.stationProfile, errlog);
    if (encoding == null) return null;

    TableConfig parentTable = makeStationTable(ds, FeatureType.STATION_PROFILE, encoding, errlog);
    if (parentTable == null) return null;

    Dimension stationDim = parentTable.dim;
    Dimension profileDim = null;
    Dimension zDim = null;
    Dimension obsDim = null;

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

    switch (encoding) {
      case single:
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
        break;

      case multidim:
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
          if (z.getRank() == 2) { // 2d time, 3d z
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
        break;

      case raggedContiguous:
        obsDim = z.getDimension(0);

        Variable numProfiles = findVariableWithStandardNameAndDimension(ds, "ragged_rowSize", stationDim, errlog);
        if (numProfiles == null) {
          errlog.format("stationProfile raggedContiguous must have a ragged_rowSize variable with station dimension %s%n", stationDim);
          return null;
        }
        if (numProfiles.getRank() != 1) {
          errlog.format("stationProfile ragged_rowSize %s variable must be rank 1%n", numProfiles.getName());
          return null;
        }

        Variable numObs = findVariableWithStandardNameAndNotDimension(ds, "ragged_rowSize", stationDim, errlog);
        if (numObs == null) {
          errlog.format("stationProfile raggedContiguous must have a ragged_rowSize variable for observations%n");
          return null;
        }
        if (numObs.getRank() != 1) {
          errlog.format("stationProfile ragged_rowSize %s variable for observations must be rank 1%n", numObs.getName());
          return null;
        }
        if (!numObs.getDimension(0).equals(obsDim)) {
          errlog.format("stationProfile ragged_rowSize %s variable for observations must have obs dimension%n", obsDim);
          return null;
        }

        profileDim = numProfiles.getDimension(0);
        break;

      case raggedIndex:
         obsDim = z.getDimension(0);

         if (time.getRank() != 1) {
          errlog.format("stationProfile raggedIndex time coordinate %s have rank 1 time%n", time);
          return null;
        }

        Variable profileIndex = findVariableWithStandardNameAndDimension(ds, "ragged_parentIndex", obsDim, errlog);
        if (profileIndex == null) {
          errlog.format("stationProfile raggedIndex must have a ragged_rowSize variable for observations%n");
          return null;
        }
        if (profileIndex.getRank() != 1) {
          errlog.format("stationProfile ragged_parentIndex %s variable for observations must be rank 1%n", profileIndex.getName());
          return null;
        }
        profileDim = profileIndex.getDimension(0);

        Variable stationIndex = findVariableWithStandardNameAndNotDimension(ds, "ragged_parentIndex", obsDim, errlog);
        if (stationIndex == null) {
          errlog.format("stationProfile raggedIndex must have a ragged_parentIndex for profiles with dimension %s%n", stationDim);
          return null;
        }
        if (stationIndex.getRank() != 1) {
          errlog.format("stationProfile ragged_parentIndex %s variable must be rank 1%n", stationIndex.getName());
          return null;
        }
        break;

      case flat:
        throw new UnsupportedOperationException("CFpointObs: stationProfile flat encoding");
    }

    TableConfig obsConfig = null;
    switch (encoding) {
      case single:
        obsConfig = makeSingle(ds, obsDim, errlog);
        break;
      case raggedContiguous:
        obsConfig = makeRaggedContiguous(ds, parentTable, obsDim, errlog);
        break;
      case raggedIndex:
        obsConfig = makeRaggedIndex(ds, obsDim, errlog);
        break;
      case multidim:
        obsConfig = makeMultidim(ds, parentTable, obsDim, errlog);
        break;
      case flat:
        throw new UnsupportedOperationException("CFpointObs flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  private TableConfig getSectionConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }

  /////////////////////////////////////////////////////////////////////

  private Encoding identifyEncoding(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    String ragged_rowSize = Evaluator.getNameOfVariableWithAttribute(ds, "standard_name", "ragged_rowSize");
    if (ragged_rowSize != null)
      return Encoding.raggedContiguous;

    String ragged_parentIndex = Evaluator.getNameOfVariableWithAttribute(ds, "standard_name", "ragged_parentIndex");
    if (ragged_parentIndex != null)
      return Encoding.raggedIndex;

    String ragged_parentId = Evaluator.getNameOfVariableWithAttribute(ds, "standard_name", "parentId");
    if (ragged_parentId != null)
      return Encoding.flat;

    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate");
      return null;
    }

    switch (ftype) {
      case point:
        return Encoding.multidim;

      case stationTimeSeries:
      case profile:
      case stationProfile:
        if (lat.getRank() == 0)
          return Encoding.single;
        else if (lat.getRank() == 1)
          return Encoding.multidim;

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 0 or 1", ftype);
        return null;

      case trajectory:
      case section:
        if (lat.getRank() == 1)
          return Encoding.single;
        else if (lat.getRank() == 2)
          return Encoding.multidim;

        errlog.format("CFpointObs %s Must have Lat/Lon coordinates of rank 1 or 2", ftype);
        return null;
    }

    return null;
  }


  // for station and stationProfile, not flat
  private TableConfig makeStationTable(NetcdfDataset ds, FeatureType ftype, Encoding encoding, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    Dimension stationDim = (encoding == Encoding.single) ? null : lat.getDimension(0);

    Table.Type stationTableType = (encoding == Encoding.single) ? Table.Type.Top : Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = ftype;
    stnTable.stnId = findNameVariableWithStandardNameAndDimension(ds, "station_id", stationDim, errlog);
    stnTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, "station_desc", stationDim, errlog);
    stnTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, "station_wmoid", stationDim, errlog);
    stnTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, "station_altitude", stationDim, errlog);
    stnTable.lat = lat.getName();
    stnTable.lon = lon.getName();

    if (encoding != Encoding.single) {
      // set up structure
      boolean stnIsStruct = Evaluator.hasRecordStructure(ds) && stationDim.isUnlimited();
      stnTable.isPsuedoStructure = !stnIsStruct;
      stnTable.dim = stationDim;
      stnTable.structName = stnIsStruct ? "record" : stationDim.getName();

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
        if ((encoding == Encoding.single) && alt.getRank() == 0)
          stnTable.stnAlt = alt.getName();

        if ((encoding != Encoding.single) && (lat.getRank() == alt.getRank()) && alt.getDimension(0).equals(stationDim))
          stnTable.stnAlt = alt.getName();
      }
    }

    return stnTable;
  }

  private TableConfig makeParentTable(NetcdfDataset ds, FeatureType ftype, Encoding encoding, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Dimension parentDim = (encoding == Encoding.single) ? null : lat.getDimension(0);

    Table.Type parentTableType = null;
    switch (encoding) {
      case single:
        parentTableType = Table.Type.Top;
        break;
      default:
        parentTableType = Table.Type.Structure;
    }

    TableConfig parentTable = new TableConfig(parentTableType, ftype.toString());
    parentTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, parentDim);
    parentTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, parentDim);
    parentTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, parentDim);
    parentTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, parentDim);
    parentTable.featureType = ftype;

    if (encoding != Encoding.single) {
      // set up structure
      boolean stnIsStruct = Evaluator.hasRecordStructure(ds) && parentDim.isUnlimited();
      parentTable.isPsuedoStructure = !stnIsStruct;
      parentTable.dim = parentDim;
      parentTable.structName = stnIsStruct ? "record" : parentDim.getName();
    }

    return parentTable;
  }


  private String findNameVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    Variable v = findVariableWithStandardNameAndDimension(ds, standard_name, outer, errlog);
    return (v == null) ? null : v.getShortName();
  }

  private Variable findVariableWithStandardNameAndDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, "standard_name", null);
      if ((stdName != null) && stdName.equals(standard_name) && v.getRank() > 0 && v.getDimension(0).equals(outer))
        return v;
    }
    return null;
  }

  private Variable findVariableWithStandardNameAndNotDimension(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    for (Variable v : ds.getVariables()) {
      String stdName = ds.findAttValueIgnoreCase(v, "standard_name", null);
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
        if ((outer != null) && (axis.getDimension(0).equals(outer)))
          return true;
        return false;
      }
    });
    if (var == null) return null;
    return var.getShortName();
  }

  private CoordinateAxis findZAxisNotStationAlt(NetcdfDataset ds) {
    CoordinateAxis z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return null == axis.findAttribute("station_altitude"); // does not have this attribute
      }
    });
    if (z != null) return z;

    z = CoordSysEvaluator.findCoordByType(ds, AxisType.Pressure, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return null == axis.findAttribute("station_altitude"); // does not have this attribute
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
    obsTable.isPsuedoStructure = !obsIsStruct;

    obsTable.numRecords = findNameVariableWithStandardNameAndDimension(ds, "ragged_rowSize", parentTable.dim, errlog);
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
    obsTable.isPsuedoStructure = !obsIsStruct;

    obsTable.parentIndex = findNameVariableWithStandardNameAndDimension(ds, "ragged_parentIndex", obsDim, errlog);
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

  private TableConfig makeMultidim(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    Dimension stationDim = parentTable.dim;

    Table.Type obsTableType = parentTable.isPsuedoStructure ? Table.Type.MultiDimStructurePsuedo : Table.Type.MultiDimInner;
    TableConfig obsTable = new TableConfig(obsTableType, obsDim.getName());
    obsTable.dim = obsDim;

    obsTable.lat = matchAxisTypeAndDimension(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisTypeAndDimension(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisTypeAndDimension(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisTypeAndDimension(ds, AxisType.Time, obsDim);

    // divide up the variables between the stn and the obs
    List<String> obsVars = null;
    List<Variable> vars = ds.getVariables();
    List<String> stnVars = new ArrayList<String>(vars.size());
    obsVars = new ArrayList<String>(vars.size());
    for (Variable orgV : vars) {
      if (orgV instanceof Structure) continue;

      Dimension dim0 = orgV.getDimension(0);
      if ((dim0 != null) && dim0.equals(stationDim)) {
        if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
          stnVars.add(orgV.getShortName());
        } else {
          Dimension dim1 = orgV.getDimension(1);
          if ((dim1 != null) && dim1.equals(obsDim))
            obsVars.add(orgV.getShortName());
        }
      }
    }

    parentTable.vars = parentTable.isPsuedoStructure ? stnVars : null; // restrict to these if psuedoStruct

    obsTable.isPsuedoStructure = parentTable.isPsuedoStructure;
    obsTable.dim = stationDim;
    obsTable.inner = obsDim;
    obsTable.structName = parentTable.isPsuedoStructure ? stationDim.getName() : "record";
    obsTable.vars = obsVars;

    Variable time = ds.findVariable(obsTable.time);
    if (time.getRank() == 1)
      obsTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));

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
    obsTable.isPsuedoStructure = !obsIsStruct;

    return obsTable;
  }


  // station calls getObsTable(ds, errlog, stationTable, CF.FeatureType.station, AxisType.Time)
  /* stationProfile calls getObsTable(ds, errlog, profileTable, CF.FeatureType.stationProfile, AxisType.Height)
  private TableConfig getObsTable(NetcdfDataset ds, Formatter errlog, TableConfig parentTable, CF.FeatureType ftype, AxisType obsType) throws IOException {
    boolean needFinish = false;

    // find the inner coordinate
    Variable obsCoord = CoordSysEvaluator.findCoordByType(ds, obsType);
    if (obsCoord == null) {
      errlog.format(ftype+ " must have coord of type "+obsType+" in inner table");
      return null;
    }
    if (obsCoord.getRank() == 0) {
      errlog.format(ftype+ " coord type "+obsType+" may not be scalar, coord var="+obsCoord.getName());
      return null;
    }
    Dimension obsDim = obsCoord.getDimension(obsCoord.getRank() - 1);

    Table.Type obsTableType = null;
    String ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_parentIndex");
    String ragged_rowSize = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_rowSize");
    if (ragged_parentIndex != null)
      obsTableType = Table.Type.ParentIndex;
    else if (ragged_rowSize != null)
      obsTableType = Table.Type.Contiguous;

    // must be multidim case if not ragged
    List<String> obsVars = null;
    if (obsTableType == null) {

      // divide up the variables between the parent and the obs
      List<Variable> vars = ds.getVariables();
      List<String> parentVars = new ArrayList<String>(vars.size());
      obsVars = new ArrayList<String>(vars.size());
      for (Variable orgV : vars) {
        if (orgV instanceof Structure) continue;

        Dimension dim0 = orgV.getDimension(0);
        if ((dim0 != null) && dim0.equals(parentTable.dim)) {
          if ((orgV.getRank() == 1) || ((orgV.getRank() == 2) && orgV.getDataType() == DataType.CHAR)) {
            parentVars.add(orgV.getShortName());
          } else {
            Dimension dim1 = orgV.getDimension(1);
            if ((dim1 != null) && dim1.equals(obsDim))
              obsVars.add(orgV.getShortName());
          }
        }
      }

      // ok, must be multidim
      if (obsVars.size() > 0) {
        parentTable.vars = parentTable.isPsuedoStructure ? parentVars : null; // restrict to these if psuedo Struct
        obsTableType = parentTable.isPsuedoStructure ? Table.Type.MultiDimStructurePsuedo : Table.Type.MultiDimInner;
      }
    }

    if (obsTableType == null) {
      errlog.format("Unknown Station/Obs");
      return null;
    }

    TableConfig obsTable = new TableConfig(obsTableType, obsDim.getName());
    obsTable.dim = obsDim;
    obsTable.time = time.getName();
    parentTable.addChild(obsTable);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.isPsuedoStructure = !obsIsStruct;

    if ((obsTableType == Table.Type.MultiDimInner) || (obsTableType == Table.Type.MultiDimStructurePsuedo)) {
      obsTable.isPsuedoStructure = parentTable.isPsuedoStructure;
      obsTable.dim = stationDim;
      obsTable.inner = obsDim;
      obsTable.structName = parentTable.isPsuedoStructure ? stationDim.getName() : "record";
      obsTable.vars = obsVars;
      if (time.getRank() == 1)
        obsTable.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));

    } else if (obsTableType == Table.Type.Contiguous) {
      obsTable.numRecords = ragged_rowSize;
      //obsTable.start = "raggedStartVar";

      // read numRecords
      Variable v = ds.findVariable(ragged_rowSize);
      if (!v.getDimension(0).equals(stationDim)) {
        errlog.format("Station - contiguous numRecords must use station dimension");
        return null;
      }
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

      /* VariableDS startV = new VariableDS(ds,  v.getParentGroup(), v.getParentStructure(), obsTable.start, v.getDataType(),
          v.getDimensionsString(), null, "starting record number for station");
      startV.setCachedData(startRecord, false);
      ds.addVariable(v.getParentGroup(), startV);
      needFinish = true; /

    } else if (obsTableType == Table.Type.ParentIndex) {
      obsTable.parentIndex = ragged_parentIndex;

      // non-contiguous ragged array
      Variable rpIndex = ds.findVariable(ragged_parentIndex);

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
    }

    if (needFinish) ds.finish();
    return obsTable;
  }  */

}
