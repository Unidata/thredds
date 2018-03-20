/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
 * FslWindProfiler netccdf files - identify coordinates
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
      switch (v.getShortName()) {
        case "staName":
          v.addAttribute(new Attribute("standard_name", "station_name"));
          break;
        case "staLat":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
          break;
        case "staLon":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
          break;
        case "staElev":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
          break;
        case "levels":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
          break;
        case "timeObs":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
          break;
      }
    }
    ds.finish();
  }

}
