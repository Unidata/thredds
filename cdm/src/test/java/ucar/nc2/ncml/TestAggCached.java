package ucar.nc2.ncml;

import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
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

    String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";
    boolean ok = true;

    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, true, false);
      System.out.printf("==========%n%s%n", out);

      EnumSet<NetcdfDataset.Enhance> modes =  ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
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

}
