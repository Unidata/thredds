package ucar.nc2.ft.point.writer;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Test WriterCFPointCollection
 *
 * @author caron
 * @since 4/11/12
 */
public class TestPointWriter {
  
  @Test
  public void testWrite() throws IOException {
    writePointDataset(TestDir.cdmUnitTestDir +  "ft/point/netcdf/Surface_Buoy_20090921_0000.nc");
  }
  
  void writePointDataset(String location) throws IOException {
    File fileIn = new File(location);
    System.out.printf("================ TestPointWriter read %s size=%d %n", location, fileIn.length());

    // open point dataset
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.POINT, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }
    
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    
    PointFeatureCollection pfc = null;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      if (fc instanceof PointFeatureCollection)
        pfc = (PointFeatureCollection) fc;
    }
    assert pfc != null;
    
    // write CF equivilent
    int pos = location.lastIndexOf("/");
    String name = location.substring(pos+1);
    File fileOut = new File(TestDir.temporaryLocalDataDir, name);
    System.out.printf("write to file %s%n ", fileOut.getAbsolutePath());

    WriterCFPointCollection writer = new WriterCFPointCollection(fileOut.getPath(), "Rewrite as CF: "+ name);

    int count = 0;
    boolean first = true;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (first) {
        writer.writeHeader(fdataset.getDataVariables(), pf.getTimeUnit(), null);
        first = false;
      }
      writer.writeRecord(pf, pf.getData());
      count++;
      if (count % 100 == 0) System.out.printf("%d ", count);
      if (count % 1000 == 0) System.out.printf("%n ");
    }
    
    writer.finish();
    
    ////////////////////////////////
    // open result

    System.out.printf("%s size = %d (%f) %n", fileOut.getPath(), fileOut.length(), ((double)fileOut.length() / fileIn.length()) );
    FeatureDataset result = FeatureDatasetFactoryManager.open(FeatureType.POINT, fileOut.getPath(), null, out);
    System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
    result.getDetailInfo(out);
    System.out.printf("%s %n", out);       
  }
  
  
}
