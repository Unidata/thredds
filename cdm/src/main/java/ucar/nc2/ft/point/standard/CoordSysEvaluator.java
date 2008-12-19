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

package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.AxisType;

/**
 * CoordinateSystem Evaluation utilities.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class CoordSysEvaluator {

  static public void findCoords(TableConfig nt, NetcdfDataset ds) {

    CoordinateSystem use = findBestCoordinateSystem(ds);
    if (use == null) return;

    for (CoordinateAxis axis : use.getCoordinateAxes()) {
      if (axis.getAxisType() == AxisType.Lat)
        nt.lat = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Lon)
        nt.lon = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Time)
        nt.time = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Height)
        nt.elev = axis.getShortName();
    }
  }

  static public String findCoordNameByType(NetcdfDataset ds, AxisType atype) {
    CoordinateAxis coordAxis = findCoordByType(ds, atype);
    return coordAxis == null ? null : coordAxis.getName();
  }


  static public CoordinateAxis findCoordByType(NetcdfDataset ds, AxisType atype) {

    CoordinateSystem use = findBestCoordinateSystem(ds);
    if (use == null) return null;

    for (CoordinateAxis axis : use.getCoordinateAxes()) {
      if (axis.getAxisType() == atype)
        return axis;
    }

    return null;
  }

  static private CoordinateSystem findBestCoordinateSystem(NetcdfDataset ds) {
        // find coordinate system with highest rank (largest number of axes)
    CoordinateSystem use = null;
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      if (use == null) use = cs;
      else if (cs.getCoordinateAxes().size() > use.getCoordinateAxes().size())
        use = cs;
    }
    return use;
  }

}
