/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.ncml;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetConstructor;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregation on datasets to be simply combined - aka "union".
 *
 * The variables are transferred from the component files to the ncml dataset
 *
 * @author caron
 */
public class AggregationUnion extends Aggregation {
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

      setDatasetAcquireProxy(vnested, ncDataset);
      vnested.close( ncfile);  // close it because we use DatasetProxyReader to acquire
    }

    ncDataset.finish();
  }

  @Override
  protected void rebuildDataset() throws IOException {
    ncDataset.empty();
    buildNetcdfDataset( null);
  }
}
