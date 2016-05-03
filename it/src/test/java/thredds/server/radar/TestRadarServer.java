package thredds.server.radar;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestRadarServer {

  @Parameterized.Parameters(name="{0}")
  public static java.util.Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            {"/radar/radarCollections.xml"},
            {"/radarServer/nexrad/level2/IDD/dataset.xml"},
            {"/radarServer/nexrad/level2/IDD/stations.xml"},
            {"/radarServer/nexrad/level2/IDD?stn=KDGX&time_start=2014-06-05T12:47:17&time_end=2014-06-05T16:07:17"},
            {"/radarServer/nexrad/level3/IDD/stations.xml"},
            {"/radarServer/terminal/level3/IDD/stations.xml"},
    });
  }

  String xmlEncoding = "application/xml;charset=UTF-8";
  String path;
  public TestRadarServer(String path) {
    this.path = TestWithLocalServer.withPath(path);
  }

  @org.junit.Test
  public void testReadRadarXml() {
    URI catUri = null;
    try {
      catUri = new URI(path);
    } catch (URISyntaxException e) {
      fail("Bad syntax in catalog URI [" + path + "]: " + e.getMessage());
    }

    try {
      HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver(catUri);
      httpUriResolver.makeRequest();
      int status = httpUriResolver.getResponseStatusCode();
      assert (status == 200) : path + " response status= " +  status;
      //assert (httpUriResolver.getResponseContentType().equals(xmlEncoding)) :
      //        " status = " +  httpUriResolver.getResponseContentType()+" expected= "+xmlEncoding;

      InputStream is = httpUriResolver.getResponseBodyAsInputStream();
      System.out.printf("response= '%s'%n", IO.readContents(is));

      /* InputStream is = httpUriResolver.getResponseBodyAsInputStream();

      InputStreamReader isr = new InputStreamReader(is, "UTF-8");
      int cnt = 1;
      while (isr.ready()) {
        char[] c = new char[1000];
        int num = isr.read(c);
        System.out.println(cnt + "[" + num + "]" + new String(c));
        cnt++;
      }   */

    } catch (IOException e) {
      fail("Failed to read catalog [" + path + "]: " + e.getMessage());
    }

  }


}
