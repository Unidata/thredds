package thredds.server.catalog;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.client.catalog.Catalog;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read different kinds of catalogs to make sure the mappings are correct
 *
 * @author caron
 * @since 10/23/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCatalogMapping {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {

    List<Object[]> result = new ArrayList<Object[]>(10);
    result.add(new Object[]{""});
    result.add(new Object[]{"/"});
    result.add(new Object[]{"catalog.xml"});
    result.add(new Object[]{"catalog.html"});
    result.add(new Object[]{"/catalog.html"});
    result.add(new Object[]{"catalog/scanLocal/catalog.html"});
    result.add(new Object[]{"catalog/scanLocal/catalog.xml"});
    result.add(new Object[]{"catalog/scanCdmUnitTests/formats/netcdf3/catalog.xml"});
    result.add(new Object[]{"catalog/scanCdmUnitTests/formats/netcdf3/catalog.html"});
    result.add(new Object[]{"somedir/somecat.xml"});
    result.add(new Object[]{"somedir/somecat.html"});
    result.add(new Object[]{"somedir/anotherdir/anothercat.xml"});
    result.add(new Object[]{"somedir/anotherdir/anothercat.html"});
    return result;
  }

  String catPath;

  public TestCatalogMapping(String catPath) {
    this.catPath = catPath;
  }

  @Test
  public void testRead() throws IOException {

    if (catPath.endsWith(".xml")) {
      Catalog cat = TestTdsLocal.open(catPath);
      assert cat != null;

    }  else {
      catPath = TestWithLocalServer.withPath(catPath);
      System.out.printf("Open html page %s%n", catPath);
      IO.readURLcontentsWithException(catPath);
    }
  }
}
