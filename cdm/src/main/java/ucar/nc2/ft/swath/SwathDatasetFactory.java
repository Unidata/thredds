/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.swath;

import ucar.nc2.Dimension;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.ft.FeatureDatasetImpl;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

/**
 * Swath dataset.
 *
 * @author caron
 * @since 2/11/12
 * @see "https://www.unidata.ucar.edu/jira/browse/CDM-42"
 */
public class SwathDatasetFactory implements FeatureDatasetFactory {

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    // If they ask for a grid, and there seems to be some grids, go for it
    if (wantFeatureType == FeatureType.SWATH ) {
      if (isSwath(ncd.getCoordinateSystems()))
        return true;
    }
    return null;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    return new SwathDataset(ncd);
  }

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[] {FeatureType.SWATH};
  }

  // LOOK - should also deal with the  case where the lat/lon grid is a sample, dot for every datapoint.
  private boolean isSwath(java.util.List<CoordinateSystem> csysList) {
    CoordinateSystem use = null;
    for (CoordinateSystem csys : csysList) {
      if (use == null) use = csys;
      else if (csys.getCoordinateAxes().size() > use.getCoordinateAxes().size())
        use = csys;
    }

    if (use == null) return false;
    CoordinateAxis lat = use.getLatAxis();
    CoordinateAxis lon = use.getLonAxis();
    CoordinateAxis time = use.getTaxis();
    if ((lat == null) || (lat.getRank() != 2)) return false;
    if ((lon == null) || (lon.getRank() != 2)) return false;
    if ((time == null)) return false; // must have time - otherwise it can be a grid

    // lat/lon must have the same dimensions
    if (!lat.getDimension(0).equals(lon.getDimension(0))) return false;
    if (!lat.getDimension(1).equals(lon.getDimension(1))) return false;

    Set<Dimension> dims = new HashSet<Dimension>(10);
    for (Dimension d : lat.getDimensions()) dims.add(d);
    for (Dimension d : lon.getDimensions()) dims.add(d);

    // diff with grid - time dimension(s) are a subset of lat/lon dimensions
    for (Dimension d : time.getDimensions()) {
      if (!dims.contains(d)) return false;
    }
    return true;
  }

  // fake
  private static class SwathDataset extends FeatureDatasetImpl {
    SwathDataset(NetcdfDataset ncd) {
      super(ncd);
    }

    @Override
    public FeatureType getFeatureType() {
      return FeatureType.SWATH;
    }

    @Override
    public void calcBounds() throws IOException {
      //To change body of implemented methods use File | Settings | File Templates.
    }
  }
}
