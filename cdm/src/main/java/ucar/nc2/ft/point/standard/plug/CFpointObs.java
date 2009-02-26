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
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.Array;

import java.util.*;
import java.io.IOException;

/**
 * CF "point obs" Convention
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

    CF.FeatureType ftype = (ftypeS == null) ? CF.FeatureType.point : CF.FeatureType.valueOf(ftypeS);
    switch (ftype) {
      case point: return getPointConfig(ds, errlog);
      case stationTimeSeries: return getStationConfig(ds, errlog);
      case profile: return getProfileConfig(ds, errlog);
      case trajectory: return getTrajectoryConfig(ds, errlog);
      case stationProfile: return getStationProfileConfig(ds, errlog);
      default:
        throw new IllegalStateException("invalid ftype= "+ftype);
    }
  }

  protected TableConfig getPointConfig(NetcdfDataset ds, Formatter errlog) {
        // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate");
      return null;
    }
    Dimension obsDim = time.getDimension(0); // what about time(stn, obs) ??
    boolean hasStruct = obsDim.isUnlimited();
    TableConfig obs = new TableConfig(Table.Type.Structure, obsDim.getName());
    obs.structName = hasStruct ? "record" : obsDim.getName();
    obs.isPsuedoStructure = !hasStruct;
    obs.dim = obsDim;
    obs.time = time.getName();
    obs.featureType = FeatureType.POINT;
    CoordSysEvaluator.findCoords(obs, ds);
    return obs;
  }

  protected TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    boolean needFinish = false;

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("Must have a Longitude coordinate");
      return null;
    }

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
        errlog.format("Lat and Lon coordinate must have same size");
        return null;
      }
      stationDim = lat.getDimension(0);
    }

    Table.Type stationTableType = stnIsScalar ? Table.Type.Top : Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = FeatureType.STATION;
    stnTable.isPsuedoStructure = !stationDim.isUnlimited();
    stnTable.dim = stationDim;

    stnTable.lat= lat.getName();
    stnTable.lon= lon.getName();

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null)
      stnTable.stnAlt = alt.getName();

    // station id
    stnTable.stnId = Evaluator.getVariableWithAttribute(ds, "standard_name", "station_id");
    if (stnTable.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id");
      return null;
    }
    Variable stnId = ds.findVariable(stnTable.stnId);

    if (!stnIsScalar) {
      if (!stnId.getDimension(0).equals(stationDim)) {
        errlog.format("Station id outer dimension must match latitude/longitude dimension");
        return null;
      }
    }

    // obs table
    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate");
      return null;
    }
    Dimension obsDim = time.getDimension(time.getRank()-1); // may be time(time) or time(stn, obs)
    boolean hasStruct = obsDim.isUnlimited();

    Table.Type obsTableType = null;
    String ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_parentIndex");
    String ragged_rowSize = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_rowSize");
    if (ragged_parentIndex != null)
      obsTableType = Table.Type.ParentIndex;
    else if (ragged_rowSize != null)
      obsTableType = Table.Type.Contiguous;

    // multidim case with Structure (GEMPAK IOSP)
    Structure multidimStruct = null;
    if (obsTableType == null) {
      // Structure(station, time)
      multidimStruct = Evaluator.getStructureWithDimensions(ds, stationDim, obsDim);
      if (multidimStruct != null) {
        obsTableType = Table.Type.MultiDimStructure;
      }
    }

    // multidim case
    if (obsTableType == null) {
      // time(station, time)
      if (time.getRank() == 2) {
        obsTableType = Table.Type.MultiDimInner;
      }


      // vars(station, time)

    }

    if (obsTableType == null) {
        errlog.format("Cannot figure out Station/obs table structure");
        return null;
    }

    TableConfig obs = new TableConfig(obsTableType, obsDim.getName());
    obs.dim = obsDim;
    obs.time = time.getName();
    stnTable.addChild(obs);

    if ((obsTableType == Table.Type.Structure) || (obsTableType == Table.Type.Contiguous) ||
      (obsTableType == Table.Type.ParentIndex)) {
      obs.structName = hasStruct ? "record" : obsDim.getName();
      obs.isPsuedoStructure = !hasStruct;
    }

    if (obsTableType == Table.Type.MultiDimStructure) {
      obs.structName = multidimStruct.getName();
      obs.isPsuedoStructure = false;
      // if time is not in this structure, need to join it
      if (multidimStruct.findVariable( time.getShortName()) == null) {
        obs.extraJoin = new JoinArray( time);
      }
    }

    if (obsTableType == Table.Type.MultiDimInner) {
      obs.dim = obsDim;
    }

    if (obsTableType == Table.Type.Contiguous) {
      obs.numRecords = ragged_rowSize;
      obs.start = "raggedStartVar";

      // construct the start variable
      Variable v = ds.findVariable(ragged_rowSize);
      if (!v.getDimension(0).equals(stationDim)) {
        errlog.format("Station - contiguous numRecords must use station dimension");
        return null;
      }

      Array numRecords = v.read();
      Array startRecord = Array.factory(v.getDataType(), v.getShape());
      int i = 0;
      long count = 0;
      while (numRecords.hasNext()) {
        startRecord.setLong(i++, count);
        count += numRecords.nextLong();
      }

      VariableDS startV = new VariableDS(ds,  v.getParentGroup(), v.getParentStructure(), obs.start, v.getDataType(),
          v.getDimensionsString(), null, "starting record number for station");
      startV.setCachedData(startRecord, false);
      ds.addVariable(v.getParentGroup(), startV);
      needFinish = true;
    }

    if (obsTableType == Table.Type.ParentIndex) {
      // non-contiguous ragged array
      Variable rpIndex = ds.findVariable(ragged_parentIndex);
  
      // construct the map
      Array index = rpIndex.read();
      int childIndex = 0;
      Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>( (int) (2 * index.getSize()));
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
      obs.indexMap = map;
    }

    if (needFinish) ds.finish();
    return stnTable;
  }

  protected TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }

  protected TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(Table.Type.MultiDimOuter, "trajectory"); // LOOK
    nt.featureType = FeatureType.TRAJECTORY;

    CoordSysEvaluator.findCoords(nt, ds);

    TableConfig obs = new TableConfig(Table.Type.MultiDimInner, "record");
    obs.dim = ds.findDimension("sample");
    obs.outer = ds.findDimension("traj");
    nt.addChild(obs);

    return nt;
  }

  protected TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }
}
