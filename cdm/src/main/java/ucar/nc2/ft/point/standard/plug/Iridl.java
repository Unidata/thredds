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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.util.Formatter;

/**
 * Class Description.
 *
 * @author caron
 * @since Dec 18, 2008
 */
public class Iridl implements TableConfigurer {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if (!ds.findAttValueIgnoreCase(null, "Conventions", "").equalsIgnoreCase("IRIDL"))
      return false;
    return true;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    Dimension stationDim = CoordSysEvaluator.findDimensionByType(ds, AxisType.Lat);
    if (stationDim == null) {
      errlog.format("Must have a latitude coordinate");
      return null;
    }

    Variable stationVar = ds.findVariable(stationDim.getName());
    if (stationVar == null) {
      errlog.format("Must have a station coordinate variable");
      return null;
    }

    Dimension obsDim = CoordSysEvaluator.findDimensionByType(ds, AxisType.Time);
    if (obsDim == null) {
      errlog.format("Must have a Time coordinate");
      return null;
    }

    // station table
    TableConfig stationTable = new TableConfig(FlattenedTable.TableType.PseudoStructure, "station");
    stationTable.featureType = FeatureType.STATION;
    stationTable.dim = stationDim;

    stationTable.stnId = stationVar.getName();

    stationTable.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    stationTable.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);
    stationTable.elev = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Height);

    // obs table
    TableConfig obsTable;
    obsTable = new TableConfig(FlattenedTable.TableType.MultiDim, "obs");
    obsTable.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    obsTable.outer = stationDim;
    obsTable.dim = obsDim;
    obsTable.join = new TableConfig.JoinConfig(Join.Type.MultiDim);
    obsTable.featureType = FeatureType.STATION;

    stationTable.addChild(obsTable);
    return stationTable;
  }
}
