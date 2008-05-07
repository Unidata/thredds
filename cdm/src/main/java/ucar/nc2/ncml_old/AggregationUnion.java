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

package ucar.nc2.ncml_old;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation on datasets to be simply combined - aka "union".
 *
 * @author caron
 */
public class AggregationUnion extends Aggregation {
  private List<NetcdfDataset> unionDatasets = new ArrayList<NetcdfDataset>();

  public AggregationUnion(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.UNION, recheckS);
  }

  @Override
  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);
  }

  /**
   * Add a nested union dataset, which has been opened externally
   *
   * @param ds add this dataset
   */
  public void addUnionDataset(NetcdfDataset ds) {
    unionDatasets.add(ds);
  }

  /**
   * Get the list of NetcdfDataset that are part of this union
   * @return List<NetcdfDataset> in the union
   */
  public List<NetcdfDataset> getUnionDatasets() {
    return unionDatasets;
  }

  /**
   * Release all resources associated with the aggregation
   * @throws IOException if close fails
   */
  public void close() throws IOException {
    for (NetcdfDataset ds : unionDatasets)
      ds.close();
    super.close();
  }
}
