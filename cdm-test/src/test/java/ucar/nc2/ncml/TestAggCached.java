package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Formatter;

/**
 * Test caching of NetcdfDataset in context of aggregation
 *
 * @author John
 * @since 9/11/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestAggCached {

  @Test
  public void TestAggCached() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = TestDir.cdmUnitTestDir + "agg/caching/wqb.ncml";
    //String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";
    boolean ok = true;

    System.out.printf("==========%n");
    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false);
      System.out.printf("----------------%nfile=%s%n%s%n", filename, out);

      EnumSet<NetcdfDataset.Enhance> modes =  ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
      System.out.printf("==========%n");
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
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
  // check if caching works
  public void TestAggCached2() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = TestDir.cdmUnitTestDir + "agg/caching/wqb.ncml";  // joinExisting

    for (int i=0; i<2; i++) {
      System.out.printf("%n=====Iteration %d =====%n", i+1);
      NetcdfDataset nc1 = NetcdfDataset.acquireDataset(filename, null); // put/get in the cache
      System.out.printf("-----------------------nc object == %d%n", nc1.hashCode());

      NetcdfDataset nc2 = new NetcdfDataset(nc1);
      System.out.printf("---new NetcdfDataset(nc1) object == %d%n", nc2.hashCode());
      FeatureDataset fd2 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc2, null, new Formatter(System.out));
      assert fd2 !=  null;  // no longer fails
      System.out.printf("---FeatureDataset not failed%n");

      Formatter out = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(nc1, nc2, out, false, false, false);
      System.out.printf("---fd compare ok %s%n%s%n", ok,out);

      NetcdfDataset nc3 = NetcdfDataset.wrap(nc1, NetcdfDataset.getEnhanceAll());
      System.out.printf("---NetcdfDataset.wrap(nc1, enhance) object == %d%n", nc3.hashCode());
      FeatureDataset fd3 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc3, null, new Formatter(System.out));
      assert fd3 != null;
      System.out.printf("---FeatureDataset not failed %d%n", i);

      /* out = new Formatter();
      ok = CompareNetcdf2.compareFiles(nc1, nc3, out, false, false, false);
      allok &= ok;
      System.out.printf("---fd compare ok %s iter %d%n", ok, i);
      System.out.printf("--------------%nfile=%s%n%s%n", filename, out); */

      NetcdfDataset nc4 = NetcdfDataset.wrap(nc1, null);
      System.out.printf("---NetcdfDataset.wrap(nc1, null) object == %d%n", nc4.hashCode());
      FeatureDataset fd4 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc4, null, new Formatter(System.err));
      assert fd4 != null;
      System.out.printf("---FeatureDataset not failed%n");

      nc1.close();
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
  }

}
