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
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;

import java.util.Formatter;

/**
 * Ndbc Convention (National Data Buoy Center)
 * @author caron
 * @since Apr 23, 2008
 */
public class Ndbc implements TableConfigurer {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if (!ds.findAttValueIgnoreCase(null, "Conventions", "").equalsIgnoreCase("COARDS"))
      return false;

    String dataProvider = ds.findAttValueIgnoreCase(null, "data_provider", null);
    if (dataProvider == null)
      dataProvider = ds.findAttValueIgnoreCase(null, "institution", "");
    if (!dataProvider.contains("National Data Buoy Center"))
      return false;

    if (null == ds.findAttValueIgnoreCase(null, "station", null)) return false;
    if (null == ds.findAttValueIgnoreCase(null, "location", null)) return false;

    //if (ds.findVariable("lat") == null) return false;
    //if (ds.findVariable("lon") == null) return false;

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

   public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    Dimension obsDim = ds.getUnlimitedDimension();
    if (obsDim == null) {
      CoordinateAxis axis = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
      if ((axis != null) && axis.isScalar())
        obsDim = axis.getDimension(0);
    }

    if (obsDim == null) {
      errlog.format("Must have an Observation dimension: unlimited dimension, or from Time Coordinate");
      return null;
    }
     FlattenedTable.TableType obsStructureType = obsDim.isUnlimited() ? FlattenedTable.TableType.Structure : FlattenedTable.TableType.PseudoStructure;

    // wants a Point
    if ((wantFeatureType == FeatureType.POINT)) {
      TableConfig nt = new TableConfig(obsStructureType, "record");
      nt.featureType = FeatureType.POINT;
      CoordSysEvaluator.findCoords(nt, ds);
      return nt;
    }

    // otherwise, make it a Station
    TableConfig nt = new TableConfig(FlattenedTable.TableType.Singleton, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    nt.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);

    nt.stnId = ds.findAttValueIgnoreCase(null, "station", null);
    nt.stnDesc = ds.findAttValueIgnoreCase(null, "description", null);
    if (nt.stnDesc == null)
      nt.stnDesc = ds.findAttValueIgnoreCase(null, "comment", null);

    TableConfig obs = new TableConfig(obsStructureType, "record");
    obs.dim = obsDim;
    obs.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    nt.addChild(obs);

    obs.join = new TableConfig.JoinConfig(Join.Type.Singleton);

    return nt;
  }
}
