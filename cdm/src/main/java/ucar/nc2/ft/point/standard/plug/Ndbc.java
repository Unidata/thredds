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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.NetcdfFile;

import java.util.Formatter;

/**
 * @author caron
 * @since Apr 23, 2008
 */
public class Ndbc implements TableConfigurer {

  public boolean isMine(NetcdfDataset ds) {
    if (!ds.findAttValueIgnoreCase(null, "Conventions", "").equalsIgnoreCase("COARDS")) return false;
    if (!ds.findAttValueIgnoreCase(null, "data_provider", "").equalsIgnoreCase("National Data Buoy Center"))
      return false;

    if (null == ds.findAttValueIgnoreCase(null, "station", null)) return false;
    if (null == ds.findAttValueIgnoreCase(null, "location", null)) return false;

    if (ds.findVariable("lat") == null) return false;
    if (ds.findVariable("lon") == null) return false;

    // DODS wont have record !!
    if (!ds.hasUnlimitedDimension()) return false;

    return true;
  }

  /*
  <!-- C:/data/dt2/station/ndbc.nc -->
  <stationFeature>
    <stationId>":station"</stationId>
    <stationDesc>":description"</stationDesc>
    <coordAxis type="lat">lat</coordAxis>
    <coordAxis type="lon">lon</coordAxis>
    <coordAxis type="height">0</coordAxis>
    <table dim="time">
      <coordAxis type="time">time</coordAxis>
    </table>
  </stationFeature>
   */

  public TableConfig getConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(NestedTable.TableType.Singleton, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = Evaluator.getVariableName(ds, "lat", errlog);
    nt.lon = Evaluator.getVariableName(ds, "lon", errlog);

    nt.stnId = Evaluator.getVariableName(ds, ":station", errlog);
    nt.stnDesc = Evaluator.getVariableName(ds, ":description", errlog);

    TableConfig obs = new TableConfig(NestedTable.TableType.Structure, "record");
    obs.dim = Evaluator.getDimension(ds, "time", errlog);
    obs.time = Evaluator.getVariableName(ds, "time", errlog);
    nt.addChild(obs);

    obs.join = new TableConfig.JoinConfig(Join.Type.Singleton);

    return nt;
  }
}
