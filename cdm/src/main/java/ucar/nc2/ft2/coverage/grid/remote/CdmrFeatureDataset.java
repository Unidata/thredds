/* Copyright */
package ucar.nc2.ft2.coverage.grid.remote;

import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;

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

  public GridCoverageDataset openGridCoverage() throws IOException {
    CdmrfReader reader = new CdmrfReader(endpoint);
    return reader.open();
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/cdmrfeature/test/testData2.grib2";
    GridCoverageDataset gdc = CoverageDatasetFactory.openGridCoverage(endpoint);
    System.out.printf("%n%s%n", gdc);
  }
}
