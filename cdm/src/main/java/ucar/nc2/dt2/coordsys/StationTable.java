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
package ucar.nc2.dt2.coordsys;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt2.point.UnidataPointDatasetHelper;

/**
 * @author caron
 * @since Apr 17, 2008
 */
public class StationTable extends NestedTable.Table {

  StationTable(NetcdfDataset ds) {
    // coordinate variables
    Variable latVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lat);
    Variable lonVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lon);
    Variable altVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Height);
    Variable timeVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Time);

  }

}
