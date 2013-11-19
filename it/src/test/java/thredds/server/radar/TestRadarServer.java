package thredds.server.radar;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TestRadarServer {

  @Parameterized.Parameters
  public static java.util.Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            {"/radarServer/catalog.xml"},
            {"/radarServer/nexrad/level2/IDD/dataset.xml"},
            {"/radarServer/nexrad/level2/CCS039/dataset.xml"},
            {"/radarServer/nexrad/level3/IDD/stations.xml"},
            {"/radarServer/terminal/level3/IDD/dataset.xml"},
            {"/thredds/radarServer/terminal/level3/IDD/stations.xml"},
    });
  }

  String xmlEncoding = "application/xml;charset=UTF-8";
  String path;
  public TestRadarServer(String path) {
    this.path = TestWithLocalServer.server + path;
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
      assert (httpUriResolver.getResponseStatusCode() == 200) : " status = " +  httpUriResolver.getResponseStatusCode();
      assert (httpUriResolver.getResponseContentType().equals(xmlEncoding)) : " status = " +  httpUriResolver.getResponseContentType();
      // assertTrue(httpUriResolver.getResponseHeaderValue("Content-").equals(ContentType.xml.getContentHeader()));

      InputStream is = httpUriResolver.getResponseBodyAsInputStream();

      InputStreamReader isr = new InputStreamReader(is, "UTF-8");
      int cnt = 1;
      while (isr.ready()) {
        char[] c = new char[1000];
        int num = isr.read(c);
        System.out.println(cnt + "[" + num + "]" + new String(c));
        cnt++;
      }

    } catch (IOException e) {
      fail("Failed to read catalog [" + path + "]: " + e.getMessage());
    }

  }


}
