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
    TableConfig stationTable = new TableConfig(Table.Type.Structure, "station");
    stationTable.structName = "station";
    stationTable.structureType = TableConfig.StructureType.PsuedoStructure;
    stationTable.featureType = FeatureType.STATION;
    stationTable.dimName = stationDim.getName();

    stationTable.stnId = stationVar.getName();

    stationTable.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    stationTable.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);
    stationTable.stnAlt = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Height);

    // obs table
    TableConfig obsTable;
    obsTable = new TableConfig(Table.Type.MultidimInner, "obs");
    obsTable.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    obsTable.outerName = stationDim.getName();
    obsTable.dimName = obsDim.getName();

    stationTable.addChild(obsTable);
    return stationTable;
  }
}
