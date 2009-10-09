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
      //if (toke.startsWith("CF-1.0"))               LOOK ???
      //  return false;  // let default analyser try
      if (toke.startsWith("CF"))
        return true;
    }
    return false;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    if (ftypeS == null)
      ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt2, null);
    if (ftypeS == null)
      ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt3, null);

    CF.FeatureType ftype;
    if (ftypeS == null)
      ftype = CF.FeatureType.point;
    else {
      try {
        ftype = CF.FeatureType.valueOf(ftypeS);
      } catch (Throwable t) {
        if (ftypeS.equalsIgnoreCase("stationProfileTimeSeries"))
          ftype = CF.FeatureType.stationProfile;
        else
          ftype = CF.FeatureType.point; // ??
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

  private Encoding identifyEncoding(NetcdfDataset ds, CF.FeatureType ftype, Formatter errlog) {
    String ragged_rowSize = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_rowSize");
    if (ragged_rowSize != null)
      return Encoding.raggedContiguous;

    String ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_parentIndex");
    if (ragged_parentIndex != null)
      return Encoding.raggedIndex;

    String ragged_parentId = Evaluator.getVariableWithAttribute(ds, "standard_name", "parentId");
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

  private boolean checkCoordinates(NetcdfDataset ds, Formatter errlog) {
    boolean ok = true;
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("CFpointObs cant find a Time coordinate");
      ok = false;
    }

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("CFpointObs cant find a Latitude coordinate");
      ok = false;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("CFpointObs cant find a Longitude coordinate");
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
      errlog.format("CFpointObs type=point: coord time must have rank 1, coord var= " + time.getName());
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
        throw new UnsupportedOperationException("CFpointObs flat encoding");
    }
    if (obsConfig == null) return null;

    stnTable.addChild(obsConfig);
    return stnTable;
  }

  private TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.trajectory, errlog);
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
        throw new UnsupportedOperationException("CFpointObs flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;  }

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
        throw new UnsupportedOperationException("CFpointObs flat encoding");
    }
    if (obsConfig == null) return null;

    parentTable.addChild(obsConfig);
    return parentTable;
  }

  private TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    Encoding encoding = identifyEncoding(ds, CF.FeatureType.stationProfile, errlog);
    if (encoding == null) return null;

    TableConfig parentTable = makeStationTable(ds, FeatureType.STATION_PROFILE, encoding, errlog);
    if (parentTable == null) return null;

    Dimension profileDim = null;
    Dimension zDim = null;
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() == 0) {
      errlog.format("stationProfile cannot have a scalar time coordinate");
      return null;
    }
    Variable z = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (z.getRank() == 0) {
      errlog.format("stationProfile cannot have a scalar z coordinate");
      return null;
    }

    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(profile) or time(profile, z)

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

  // for station and stationProfile, not flat

  private TableConfig makeStationTable(NetcdfDataset ds, FeatureType ftype, Encoding encoding, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    Dimension stationDim = (encoding == Encoding.single) ? null : lat.getDimension(0);

    Table.Type stationTableType = null;
    switch (encoding) {
      case single:
        stationTableType = Table.Type.Top;
        break;
      default:
        stationTableType = Table.Type.Structure;
    }

    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = ftype;
    stnTable.stnId = matchStandardName(ds, "station_id", stationDim, errlog);
    stnTable.stnDesc = matchStandardName(ds, "station_desc", stationDim, errlog);
    stnTable.stnWmoId = matchStandardName(ds, "station_wmoid", stationDim, errlog);
    stnTable.stnAlt = matchStandardName(ds, "station_altitude", stationDim, errlog);
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
    parentTable.lat = matchAxisType(ds, AxisType.Lat, parentDim);
    parentTable.lon = matchAxisType(ds, AxisType.Lon, parentDim);
    parentTable.elev = matchAxisType(ds, AxisType.Height, parentDim);
    parentTable.time = matchAxisType(ds, AxisType.Time, parentDim);
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


  private String matchStandardName(NetcdfDataset ds, String standard_name, Dimension outer, Formatter errlog) {
    String varname = Evaluator.getVariableWithAttribute(ds, "standard_name", standard_name);
    if ((varname != null) && (outer != null)) {
      Variable var = ds.findVariable(varname);
      if (!var.getDimension(0).equals(outer)) {
        errlog.format("Station variable %s must have outer dimension that matches latitude/longitude dimension %s%n", standard_name, outer);
        return null;
      }
    }
    return varname;
  }

  private String matchAxisType(NetcdfDataset ds, AxisType type, Dimension outer) {
    Variable var = CoordSysEvaluator.findCoordByTypeAndDimension(ds, type, outer);
    if (var == null) return null;
    return var.getShortName();
  }

  /////////////////////////////////////////////////////////////////////////////////

  private TableConfig makeRaggedContiguous(NetcdfDataset ds, TableConfig parentTable, Dimension obsDim, Formatter errlog) throws IOException {
    TableConfig obsTable = new TableConfig(Table.Type.Contiguous, obsDim.getName());
    obsTable.dim = obsDim;

    obsTable.lat = matchAxisType(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisType(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisType(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisType(ds, AxisType.Time, obsDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.isPsuedoStructure = !obsIsStruct;

    obsTable.numRecords = matchStandardName(ds, "ragged_rowSize", parentTable.dim, errlog);
    if (null == obsTable.numRecords)
      return null;

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

    obsTable.lat = matchAxisType(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisType(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisType(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisType(ds, AxisType.Time, obsDim);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsTable.structName = obsIsStruct ? "record" : obsDim.getName();
    obsTable.isPsuedoStructure = !obsIsStruct;

    obsTable.parentIndex = matchStandardName(ds, "ragged_parentIndex", obsDim, errlog);
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

    obsTable.lat = matchAxisType(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisType(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisType(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisType(ds, AxisType.Time, obsDim);

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

    obsTable.lat = matchAxisType(ds, AxisType.Lat, obsDim);
    obsTable.lon = matchAxisType(ds, AxisType.Lon, obsDim);
    obsTable.elev = matchAxisType(ds, AxisType.Height, obsDim);
    obsTable.time = matchAxisType(ds, AxisType.Time, obsDim);

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
