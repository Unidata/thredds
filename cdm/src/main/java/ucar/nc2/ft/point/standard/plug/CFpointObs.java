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
        ftype = CF.FeatureType.point; // ??
      }
    }

    if (!checkForCoordinates(ds, errlog)) return null;

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
      default:
        throw new IllegalStateException("invalid ftype= " + ftype);
    }
  }

  private boolean checkForCoordinates(NetcdfDataset ds, Formatter errlog) {
    boolean ok = true;
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Cant find a Time coordinate");
      ok = false;
    }

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Cant find a Latitude coordinate");
      ok = false;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("Cant find a Longitude coordinate");
      ok = false;
    }

    return ok;
  }

  private TableConfig getPointConfig(NetcdfDataset ds, Formatter errlog) {
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time.getRank() == 0) {
      errlog.format("FeqatureType point: coord time may not be scalar, coord var= "+time.getName());
      return null;
    }
    Dimension obsDim = time.getDimension(0);
    boolean hasStruct = Evaluator.hasRecordStructure(ds);

    TableConfig obs = new TableConfig(Table.Type.Structure, obsDim.getName());
    obs.structName = hasStruct ? "record" : obsDim.getName();
    obs.isPsuedoStructure = !hasStruct;
    obs.dim = obsDim;
    obs.time = time.getName();
    obs.featureType = FeatureType.POINT;
    CoordSysEvaluator.findCoords(obs, ds);

    return obs;
  }

  private TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    boolean needFinish = false;

    TableConfig stnTable = getStationTable(ds, errlog);
    if (stnTable == null) return null;
    Dimension stationDim = stnTable.dim;

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Unknown Station/Obs");
      return null;
    }
    Dimension obsDim = time.getDimension(time.getRank() - 1); // may be time(time) or time(stn, obs)

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

      // divide up the variables bewteen the stn and the obs
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

      // ok, must be multidim
      if (obsVars.size() > 0) {
        stnTable.vars = stnTable.isPsuedoStructure ? stnVars : null; // restrict to these if psuedo Struct
        obsTableType = stnTable.isPsuedoStructure ? Table.Type.MultiDimStructurePsuedo : Table.Type.MultiDimInner;
      }
    }

    if (obsTableType == null) {
      errlog.format("Unknown Station/Obs");
      return null;
    }

    TableConfig obsConfig = new TableConfig(obsTableType, obsDim.getName());
    obsConfig.dim = obsDim;
    obsConfig.time = time.getName();
    stnTable.addChild(obsConfig);

    boolean obsIsStruct = Evaluator.hasRecordStructure(ds) && obsDim.isUnlimited();
    obsConfig.structName = obsIsStruct ? "record" : obsDim.getName();
    obsConfig.isPsuedoStructure = !obsIsStruct;

    if ((obsTableType == Table.Type.MultiDimInner) || (obsTableType == Table.Type.MultiDimStructurePsuedo)) {
      obsConfig.isPsuedoStructure = stnTable.isPsuedoStructure;
      obsConfig.dim = stationDim;
      obsConfig.inner = obsDim;
      obsConfig.structName = stnTable.isPsuedoStructure ? stationDim.getName() : "record";
      obsConfig.vars = obsVars;
      if (time.getRank() == 1)
        obsConfig.addJoin(new JoinArray(time, JoinArray.Type.raw, 0));

    } else if (obsTableType == Table.Type.Contiguous) {
      obsConfig.numRecords = ragged_rowSize;
      //obsConfig.start = "raggedStartVar";

      // read numRecords
      Variable v = ds.findVariable(ragged_rowSize);
      if (!v.getDimension(0).equals(stationDim)) {
        errlog.format("Station - contiguous numRecords must use station dimension");
        return null;
      }
      Array numRecords = v.read();
      int n = (int) v.getSize();

      // construct the start variable
      obsConfig.startIndex = new int[n];
      int i = 0;
      int count = 0;
      while (numRecords.hasNext()) {
        obsConfig.startIndex[i++] = count;
        count += numRecords.nextLong();
      }

      /* VariableDS startV = new VariableDS(ds,  v.getParentGroup(), v.getParentStructure(), obsConfig.start, v.getDataType(),
          v.getDimensionsString(), null, "starting record number for station");
      startV.setCachedData(startRecord, false);
      ds.addVariable(v.getParentGroup(), startV);
      needFinish = true; */

    } else if (obsTableType == Table.Type.ParentIndex) {
      obsConfig.parentIndex = ragged_parentIndex;

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
      obsConfig.indexMap = map;
    }

    if (needFinish) ds.finish();
    return stnTable;
  }

  private TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }

  private TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(Table.Type.Structure, "trajectory");
    nt.featureType = FeatureType.TRAJECTORY;

    CoordSysEvaluator.findCoords(nt, ds);

    TableConfig obs = new TableConfig(Table.Type.MultiDimInner, "record");
    obs.dim = ds.findDimension("sample");
    obs.outer = ds.findDimension("traj");
    nt.addChild(obs);

    return nt;
  }

  private TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig stnTable = getStationTable(ds, errlog);
    if (stnTable == null) return null;
    Dimension stationDim = stnTable.dim;

    TableConfig nt = new TableConfig(Table.Type.Structure, "stationProfile");
    nt.featureType = FeatureType.STATION_PROFILE;

    CoordSysEvaluator.findCoords(nt, ds);

    TableConfig obs = new TableConfig(Table.Type.MultiDimInner, "record");
    obs.dim = ds.findDimension("sample");
    obs.outer = ds.findDimension("traj");
    nt.addChild(obs);

    return stnTable;
  }

  /////////////////////////////////////////////////////////////////////

  // for station and stationProfile
  private TableConfig getStationTable(NetcdfDataset ds, Formatter errlog) throws IOException {
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lat.getRank() != lon.getRank()) {
      errlog.format("Lat and Lon coordinate must have same rank");
      return null;
    }

    // check dimensions
    boolean stnIsScalar = (lat.getRank() == 0);
    boolean stnIsSingle = (lat.getRank() == 1) && (lat.getSize() == 1);
    Dimension stationDim = null;

    if (!stnIsScalar) {
      if (lat.getDimension(0) != lon.getDimension(0)) {
        errlog.format("Lat and Lon coordinate must have same outer dimension");
        return null;
      }
      stationDim = lat.getDimension(0);
    }

    Table.Type stationTableType = stnIsScalar ? Table.Type.Top : Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = FeatureType.STATION;

    if (!stnIsScalar) {
      boolean stnIsStruct = (stationDim != null) && Evaluator.hasRecordStructure(ds) && stationDim.isUnlimited();
      stnTable.isPsuedoStructure = !stnIsStruct;
      stnTable.dim = stationDim;
      stnTable.structName = stnIsStruct ? "record" : stationDim.getName();
    }

    // station id
    stnTable.stnId = Evaluator.getVariableWithAttribute(ds, "standard_name", "station_id");
    if (!stnIsScalar) {
      if (stnTable.stnId == null) {
        errlog.format("Must have a Station id variable with standard name station_id");
        return null;
      }
      Variable stnId = ds.findVariable(stnTable.stnId);
      if (!stnId.getDimension(0).equals(stationDim)) {
        errlog.format("Station id outer dimension must match latitude/longitude dimension");
        return null;
      }
    }


    stnTable.lat = lat.getName();
    stnTable.lon = lon.getName();

    // optional alt coord - detect if its a station height or actuaklly associated with the obs, eg for a profile
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null) {
      if (stnIsScalar && alt.getRank() == 0)
        stnTable.stnAlt = alt.getName();

      if (!stnIsScalar && (lat.getRank()) == alt.getRank() && (lat.getDimension(0) == alt.getDimension(0)))
        stnTable.stnAlt = alt.getName();
    }

    return stnTable;
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
