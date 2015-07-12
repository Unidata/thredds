/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.remote.CdmrFeatureDataset;

import java.io.IOException;

/**
 * factory for CoverageDataset
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {

  static public CoverageDataset openCoverage(String endpoint) throws IOException {

    // remote CdmrFeatureDataset
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {

      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return reader.openCoverageDataset();

    } else if (endpoint.startsWith("http:")) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      if (reader.isCmrfEndpoint()) {
        return reader.openCoverageDataset();
      } // otherwise let it fall through to DtCoverageDataset.open(endpoint)

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    // adapt a DtCoverageDataset (forked from ucar.nc2.dt.GridDataset), eg a local file
    DtCoverageDataset gds = DtCoverageDataset.open(endpoint);
    if (gds.getGrids().size() > 0)
      return DtCoverageAdapter.factory(gds);

    return null;
  }

}
