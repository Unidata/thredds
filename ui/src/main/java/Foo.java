import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * @author cwardgar
 * @since 2015/02/20
 */
public class Foo {
  public static void main(String[] args) throws IOException {
    File ncmlFile = new File("C:/Users/cwardgar/dev/data/station_700_agg/station700.ncml");
    String ncmlLocation = ncmlFile.toURI().toURL().toString();

    FeatureDataset featureDataset = FeatureDatasetFactoryManager.open(
            null, ncmlLocation, null, new Formatter(System.out));
    try {
      long startTime = System.currentTimeMillis();
      featureDataset.calcBounds();
      long endTime = System.currentTimeMillis();

      System.out.println("Bounding box: " + featureDataset.getBoundingBox());
      System.out.println("Date range: " + featureDataset.getDateRange());
      System.out.println("Total execution time: " + (endTime - startTime) + "ms");
    } finally {
      featureDataset.close();
    }
  }
}
