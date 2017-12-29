/* Copyright */
package thredds.server.services;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
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
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {

    List<Object[]> result = new ArrayList<>(10);
    result.add(new Object[]{"metadata/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1?metadata=variableMap", null});
    result.add(new Object[]{"metadata/gribCollection/GFS_CONUS_80km/Best?metadata=variableMap", null});
    result.add(new Object[]{"metadata/restrictCollection/GFS_CONUS_80km/TwoD?metadata=variableMap", new int[] {HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_FORBIDDEN}});

    return result;
  }
  private static final boolean show = true;

  String url;
  int[] statusCodes;

  public TestMetadataService(String url, int[] statusCodes) {
    this.url = url;
    this.statusCodes = statusCodes;
  }

  @Test
  public void testOpenXml() {
    String endpoint = TestOnLocalServer.withHttpPath(url+"&accept=xml");
    byte[] response = TestOnLocalServer.getContent(endpoint, statusCodes, ContentType.xml);
    if (show && response != null)
      System.out.printf("%s%n", new String(response, CDM.utf8Charset));
  }

  @Test
  public void testOpenHtml() {
    String endpoint = TestOnLocalServer.withHttpPath(url);
    byte[] response = TestOnLocalServer.getContent(endpoint, statusCodes, ContentType.html);
    if (show && response != null)
      System.out.printf("%s%n", new String(response, CDM.utf8Charset));
  }
}

