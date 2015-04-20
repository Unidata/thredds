/* Copyright */
package thredds.server.opendap;

import org.junit.Assert;
import org.junit.Test;
import thredds.TestWithLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Describe
 *
 * @author caron
 * @since 4/19/2015
 */
public class TestOpendapFilters {

  @Test
  public void testIllegalRequest() throws IOException, InvalidRangeException {
    String url = TestWithLocalServer.withPath("/dodsC/scanLocal/testWrite.nc.dds?");
    String esc = url + URLEncoder.encode("<bad>\\worse", CDM.UTF8);
    try (HTTPSession session = new HTTPSession(esc)) {
      HTTPMethod method = HTTPFactory.Get(session);
      method.execute();
      Assert.assertEquals(400, method.getStatusCode());
    }

  }
}
