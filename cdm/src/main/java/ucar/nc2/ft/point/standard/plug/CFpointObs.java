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
      return false;

    if (CF.FeatureType.valueOf(datatype) == null)
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.startsWith("CF-1.0"))
        return false;  // let default analyser try
      if (toke.startsWith("CF"))
        return true;
    }
    return false;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
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
    TableConfig nt = new TableConfig(Table.Type.Structure, "record");
    nt.structName = "record";
    nt.featureType = FeatureType.POINT;
    CoordSysEvaluator.findCoords(nt, ds);
    return nt;
  }

  protected TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig nt = new TableConfig(Table.Type.Structure, "station");
    nt.featureType = FeatureType.STATION;
    nt.isPsuedoStructure = true;
    nt.addIndex = true;
    CoordSysEvaluator.findCoords(nt, ds);

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("Must have a Latitude coordinate");
      return null;
    }
    nt.lat= lat.getName();

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("Must have a Longitude coordinate");
      return null;
    }
    nt.lon= lon.getName();

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null)
      nt.elev = alt.getName();

    // station id
    nt.stnId = Evaluator.getVariableWithAttribute(ds, "standard_name", "station_id");
    if (nt.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id");
      return null;
    }
    Variable stnId = ds.findVariable(nt.stnId);

    // check dimensions
    Dimension stationDim = stnId.getDimension(0);
    if (!lat.getDimension(0).equals(stationDim)) {
      errlog.format("Station id outer dimension must match latitude dimension");
      return null;
    }
    if (!lon.getDimension(0).equals(stationDim)) {
      errlog.format("Station id outer dimension must match longitude dimension");
      return null;
    }

    nt.dim = stationDim;

    // ragged array
    String ragged_parentIndex = Evaluator.getVariableWithAttribute(ds, "standard_name", "ragged_parentIndex");
    Variable rpIndex = ds.findVariable(ragged_parentIndex);
    Dimension obsDim = rpIndex.getDimension(0);
    boolean hasStruct = obsDim.isUnlimited();

    TableConfig obs = new TableConfig(Table.Type.Structure, obsDim.getName());
    obs.structName = hasStruct ? "record" : obsDim.getName();
    obs.dim = obsDim;
    obs.isPsuedoStructure = !hasStruct;

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

    Variable time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("Must have a Time coordinate");
      return null;
    }
    obs.time = time.getName();
    nt.addChild(obs);

    return nt;
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
