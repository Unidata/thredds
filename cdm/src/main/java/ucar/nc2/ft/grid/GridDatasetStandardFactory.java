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
package ucar.nc2.ft.grid;

import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;
import java.util.Formatter;

/**
 * FeatureDatasetFactory for Grids, using standard coord sys analysis
 * @author caron
 * @since Dec 30, 2008
 */
public class GridDatasetStandardFactory implements FeatureDatasetFactory {

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) throws IOException {
    // If they ask for a grid, and there seems to be some grids, go for it
    if (wantFeatureType == FeatureType.GRID ) {
      ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
      if (gds.getGrids().size() > 0) {
        return gds;
      }
    }
    return null;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    // already been opened by isMine
    return (GridDataset) analysis;
  }

  public FeatureType[] getFeatureType() {
    return new FeatureType[] {FeatureType.GRID};
  }
}
