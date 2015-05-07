/* Copyright */
package ucar.nc2.ft2.remote;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 5/2/2015
 */
public class CdmrFeatureDataset {

  static public GridCoverageDataset factory(FeatureType wantFeatureType, String endpoint) throws IOException {
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME))
      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());

    if (wantFeatureType == FeatureType.GRID) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return reader.open();
    }

    return null;
  }

  String endpoint;

  public CdmrFeatureDataset(String endpoint) {
    this.endpoint = endpoint;
  }

  public GridCoverageDataset open() throws IOException {
    CdmrfReader reader = new CdmrfReader(endpoint);
    return reader.open();
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/cdmrfeature/test/testData2.grib2";
    GridCoverageDataset gdc = CdmrFeatureDataset.factory(FeatureType.GRID, endpoint);
    System.out.printf("%n%s%n", gdc);
  }
}
