/* Copyright */
package ucar.nc2.ft2.remote;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.grid.DtGridDatasetAdapter;
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

    if (wantFeatureType == FeatureType.GRID) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return reader.open();
    }

    return null;
  }

  boolean isRemote;
  String endpoint;

  public CdmrFeatureDataset(String endpoint) {
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {
      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      isRemote = true;

    } else if (endpoint.startsWith("http:")) {
      isRemote = true;

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    this.endpoint = endpoint;
  }

  public GridCoverageDataset open() throws IOException {
    if (isRemote) {
      CdmrfReader reader = new CdmrfReader(endpoint);
      return reader.open();
    }

    ucar.nc2.dt.grid.GridDataset gds = GridDataset.open(endpoint);
    if (gds.getGrids().size() > 0) {
      return new DtGridDatasetAdapter(gds);
    }

    return null;
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/cdmrfeature/test/testData2.grib2";
    GridCoverageDataset gdc = CdmrFeatureDataset.factory(FeatureType.GRID, endpoint);
    System.out.printf("%n%s%n", gdc);
  }
}
