// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class AggregationUnion extends Aggregation {
  private ArrayList unionDatasets = new ArrayList(); // List<NetcdfDataset>

  public AggregationUnion(NetcdfDataset ncd, String dimName, String recheckS) {
    super( ncd, dimName, Aggregation.Type.UNION, recheckS);
  }

  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    buildCoords(cancelTask);
  }

  /**
   * Add a nested union dataset, which has been opened externally
   */
  public void addDatasetUnion(NetcdfDataset ds) {
    unionDatasets.add(ds);
  }


  public List getUnionDatasets() {
    return unionDatasets;
  }

    /**
   * Release all resources associated with the aggregation
   *
   * @throws IOException
   */
  public void close() throws IOException {

    for (int i = 0; i < unionDatasets.size(); i++) {
      NetcdfDataset ds = (NetcdfDataset) unionDatasets.get(i);
      ds.close();
    }

    super.close();
  }
}
