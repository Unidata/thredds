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
package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;

import java.io.IOException;

/**
 * @author caron
 * @since Apr 17, 2008
 */
public class FslWindProfiler extends CoordSysBuilder {

  public static boolean isMine( NetcdfFile ncfile) {
    String title = ncfile.findAttValueIgnoreCase(null, "title", null);
    return title != null && (title.startsWith("WPDN data"));
  }

  public FslWindProfiler() {
    this.conventionName = "FslWindProfiler";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    for (Variable v : ds.getVariables()) {
      if (v.getShortName().equals("staName"))
        v.addAttribute( new Attribute("standard_name", "station_name"));
      else if (v.getShortName().equals("staLat"))
        v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
      else if (v.getShortName().equals("staLon"))
        v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
      else if (v.getShortName().equals("staElev"))
        v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      else if (v.getShortName().equals("levels"))
        v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      else if (v.getShortName().equals("timeObs"))
        v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    }
    ds.finish();
  }

}
