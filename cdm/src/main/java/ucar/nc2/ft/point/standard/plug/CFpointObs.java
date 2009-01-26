/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.point.standard.CoordSysEvaluator;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;

import java.util.*;

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

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    CF.FeatureType ftype = (ftypeS == null) ? CF.FeatureType.point : CF.FeatureType.valueOf(ftypeS);
    switch (ftype) {
      case point: return getPointConfig(ds, errlog);
      case station: return getStationConfig(ds, errlog);
      case profile: return getProfileConfig(ds, errlog);
      case trajectory: return getTrajectoryConfig(ds, errlog);
      case stationProfile: return getStationProfileConfig(ds, errlog);
      default:
        throw new IllegalStateException("invalid ftype= "+ftype);
    }
  }

  protected TableConfig getPointConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(Table.Type.Structure, "record");
    nt.featureType = FeatureType.POINT;
    CoordSysEvaluator.findCoords(nt, ds);
    return nt;
  }

  // ??
  protected TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(Table.Type.Singleton, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = Evaluator.getVariableName(ds, "latitude", errlog);
    nt.lon = Evaluator.getVariableName(ds, "longitude", errlog);

    nt.stnId = Evaluator.getVariableWithAttribute(ds, "standard_name", "station_name");
    //nt.stnDesc = Evaluator.getVariableName(ds, ":description", errlog);

    TableConfig obs = new TableConfig(Table.Type.Structure, "record");
    obs.dim = Evaluator.getDimension(ds, "time", errlog);
    obs.time = Evaluator.getVariableName(ds, "time", errlog);
    nt.addChild(obs);

    return nt;
  }

  protected TableConfig getProfileConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }

  protected TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(Table.Type.MultiDim, "trajectory");
    nt.featureType = FeatureType.TRAJECTORY;

    CoordSysEvaluator.findCoords(nt, ds);

    TableConfig obs = new TableConfig(Table.Type.MultiDim, "record");
    obs.dim = ds.findDimension("sample");
    obs.outer = ds.findDimension("traj");
    nt.addChild(obs);

    return nt;
  }

  protected TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) {
    return null;
  }
}
