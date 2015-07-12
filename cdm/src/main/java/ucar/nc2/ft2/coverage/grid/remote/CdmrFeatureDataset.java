/* Copyright */
package ucar.nc2.ft2.coverage.grid.remote;

import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;

import java.io.IOException;

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

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/cdmrfeature/test/testData2.grib2";
    CoverageDataset gdc = CoverageDatasetFactory.openCoverage(endpoint);
    System.out.printf("%n%s%n", gdc);
  }
}
