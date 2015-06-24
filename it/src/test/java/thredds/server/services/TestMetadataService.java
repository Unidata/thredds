/* Copyright */
package thredds.server.services;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Metadata Service
 *
 * @author caron
 * @since 6/24/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestMetadataService {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {

    List<Object[]> result = new ArrayList<>(10);
    result.add(new Object[]{"metadata/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1?metadata=variableMap"});
    result.add(new Object[]{"metadata/gribCollection/GFS_CONUS_80km/Best?metadata=variableMap"});
    result.add(new Object[]{"metadata/restrictCollection/GFS_CONUS_80km/TwoD?metadata=variableMap"});

    return result;
  }
  private static final boolean show = true;

  String url;

  public TestMetadataService(String url) {
    this.url = url;
  }

  @Test
  public void testOpenXml() {
    String endpoint = TestWithLocalServer.withPath(url+"&accept=xml");
    String response = TestWithLocalServer.testWithHttpGet(endpoint, ContentType.xml);
    if (show)
      System.out.printf("%s%n", response);
  }

  @Test
  public void testOpenHtml() {
    String endpoint = TestWithLocalServer.withPath(url);
    String response = TestWithLocalServer.testWithHttpGet(endpoint, ContentType.html);
    if (show)
      System.out.printf("%s%n", response);
  }
}

