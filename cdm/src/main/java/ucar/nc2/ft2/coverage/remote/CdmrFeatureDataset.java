/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Remote GridCoverageDataset using cdmrFeature protocol.
 * May not be needed.
 *
 * @author caron
 * @since 5/2/2015
 */
public class CdmrFeatureDataset {

  String endpoint;

  public CdmrFeatureDataset(String endpoint) {
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {
      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
    }

    this.endpoint = endpoint;
  }

  public boolean isCmrfEndpoint() throws IOException {
    CdmrfReader reader = new CdmrfReader(endpoint);
    return reader.isCmrfEndpoint();
  }

  public CoverageDataset openCoverageDataset() throws IOException {
    CdmrfReader reader = new CdmrfReader(endpoint);
    return reader.open();
  }

}
