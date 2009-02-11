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
