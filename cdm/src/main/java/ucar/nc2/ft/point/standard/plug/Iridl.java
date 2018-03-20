/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.CDM;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.util.Formatter;

/**
 * Class Description.
 *
 * @author caron
 * @since Dec 18, 2008
 */
public class Iridl extends TableConfigurerImpl  {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if (!ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, "").equalsIgnoreCase("IRIDL"))
      return false;
    return true;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    Dimension stationDim = CoordSysEvaluator.findDimensionByType(ds, AxisType.Lat);
    if (stationDim == null) {
      errlog.format("Must have a latitude coordinate");
      return null;
    }

    Variable stationVar = ds.findVariable(stationDim.getShortName());
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
    TableConfig stationTable = new TableConfig(Table.Type.Structure, "station");
    stationTable.structName = "station";
    stationTable.structureType = TableConfig.StructureType.PsuedoStructure;
    stationTable.featureType = FeatureType.STATION;
    stationTable.dimName = stationDim.getShortName();

    stationTable.stnId = stationVar.getShortName();

    stationTable.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    stationTable.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);
    stationTable.stnAlt = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Height);

    // obs table
    TableConfig obsTable;
    obsTable = new TableConfig(Table.Type.MultidimInner, "obs");
    obsTable.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    obsTable.outerName = stationDim.getShortName();
    obsTable.dimName = obsDim.getShortName();

    stationTable.addChild(obsTable);
    return stationTable;
  }
}
