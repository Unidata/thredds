package ucar.nc2.ncml;

import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.cache.FileCache;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Formatter;

/**
 * Description
 *
 * @author John
 * @since 9/11/13
 */
public class TestAggCached {

  @Test
  public void TestAggCached() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = "G:/work/CherylMorse/wqb.ncml";
    //String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";
    boolean ok = true;

    System.out.printf("==========%n");
    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, true, false);
      System.out.printf("----------------%nfile=%s%n%s%n", filename, out);

      EnumSet<NetcdfDataset.Enhance> modes =  ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
      System.out.printf("==========%n");
    }

    FileCache cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
    assert ok;
  }

  private void showModes(EnumSet<NetcdfDataset.Enhance> modes) {
    for (NetcdfDataset.Enhance mode : modes) {
      System.out.printf("%s,", mode);
    }
    System.out.printf("%n");
  }

  @Test
  // this is the CherlyMorse problem
  public void TestAggCached2() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = "G:/work/CherylMorse/wqb.ncml";
    //String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";
    boolean ok = true;

    System.out.printf("==========%n");
    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);

      NetcdfDataset ncd2 = new NetcdfDataset(ncd);
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false);
      System.out.printf("---fd2%n");

      System.out.printf("--------------%nfile=%s%n%s%n", filename, out);
      FeatureDataset fd2 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, ncd2, null,
               new Formatter(System.out));
      assert fd2 == null;

      System.out.printf("---fd3%n");
      NetcdfDataset ncd3 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      FeatureDataset fd3 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, ncd3, null,
               new Formatter(System.out));
      assert fd3 != null;

      System.out.printf("---fd4%n");
      NetcdfDataset ncd4 = NetcdfDataset.wrap(ncd, null);
      FeatureDataset fd4 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, ncd4, null,
               new Formatter(System.err));
      assert fd4 != null;

      ncd2.close();
      System.out.printf("==========%n");
    }

    FileCache cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
    assert ok;
  }


}
