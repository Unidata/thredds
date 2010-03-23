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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Aggregation on datasets to be simply combined - aka "union".
 *
 * The variables are transferred from the component files to the ncml dataset
 *
 * @author caron
 */
public class AggregationUnion extends Aggregation {
  private List<NetcdfFile> openDatasets = new ArrayList<NetcdfFile>();

  public AggregationUnion(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Aggregation.Type.union, recheckS);
  }

  @Override
  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    // each Dataset just gets "transfered" into the resulting NetcdfDataset
    List<Dataset> nestedDatasets = getDatasets();
    for (Dataset vnested : nestedDatasets) {
      // LOOK could just open the file, not use acquire.
      NetcdfFile ncfile = vnested.acquireFile(cancelTask);
      DatasetConstructor.transferDataset(ncfile, ncDataset, null);
      // do not close - all stay open. Could use Proxy if need to open only as needed.
      openDatasets.add(ncfile);
    }
  }

  @Override
  protected void rebuildDataset() throws IOException {
    ncDataset.empty();
    buildNetcdfDataset( null);
  }

  @Override
  protected void closeDatasets() throws IOException {
    for (NetcdfFile ncfile : openDatasets) {
      try {
        ncfile.close();
      } catch (IOException e) {
       // ignore
      }
    }
    super.closeDatasets();
  }

}
