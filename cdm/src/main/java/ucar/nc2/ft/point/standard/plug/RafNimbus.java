/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.Dimension;

import java.util.Formatter;
import java.io.IOException;

/**
 * RAF Nimbus conventions
 *
 * @author caron
 * @since Nov 6, 2009
 */
public class RafNimbus extends TableConfigurerImpl {
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String center = ds.findAttValueIgnoreCase(null, "Convention", null);
    return center != null && center.equals("NCAR-RAF/nimbus");
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig topTable = new TableConfig(Table.Type.Top, "singleTrajectory");

    CoordinateAxis coordAxis = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (coordAxis == null) {
      errlog.format("Cant find a time coordinate");
      return null;
    }
    final Dimension innerDim = coordAxis.getDimension(0);
    boolean obsIsStruct = Evaluator.hasNetcdf3RecordStructure(ds) && innerDim.isUnlimited();

    TableConfig obsTable = new TableConfig(Table.Type.Structure, innerDim.getShortName());
    obsTable.dimName = innerDim.getShortName();
    obsTable.time = coordAxis.getFullName();
    obsTable.structName = obsIsStruct ? "record" : innerDim.getShortName();
    obsTable.structureType = obsIsStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
    CoordSysEvaluator.findCoords(obsTable, ds, new CoordSysEvaluator.Predicate() {
      public boolean match(CoordinateAxis axis) {
        return innerDim.equals(axis.getDimension(0));
      }
    });

    topTable.addChild(obsTable);
    return topTable;
  }
}