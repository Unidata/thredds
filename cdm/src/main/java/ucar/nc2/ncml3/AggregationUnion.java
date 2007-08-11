/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.ncml3;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation on datasets to be simply combined - aka "union".
 *
 * @author caron
 */
public class AggregationUnion extends Aggregation {

  public AggregationUnion(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.UNION, recheckS);
  }

  @Override
  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {

    // each dataset just gets "transferred" into this dataset
    List<Dataset> nestedDatasets = datasetManager.getDatasets();
    for (Dataset vnested : nestedDatasets) {
      NetcdfFile ncfile = vnested.acquireFile(cancelTask);
      DatasetConstructor.transferDataset(ncfile, ncDataset, null);
    }
  }

  @Override
  public Array read(Variable mainv, CancelTask cancelTask) throws IOException {
    throw new IllegalStateException(); // should never be called
  }


  @Override
  public Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
    throw new IllegalStateException(); // should never be called
  }

}
