/* Copyright */
package thredds.server.opendap;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;

/**
 * test that TDS filters work
 *
 * @author caron
 * @since 4/19/2015
 */
public class TestOpendapFilters {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testIllegalRequest() throws IOException, InvalidRangeException {
    String url = TestOnLocalServer.withHttpPath("/dodsC/scanLocal/testWrite.nc.dds?");
    String esc = url + URLEncoder.encode("<bad>\nworse", CDM.UTF8);
    try (HTTPSession session = HTTPFactory.newSession(esc)) {
      HTTPMethod method = HTTPFactory.Get(session);
      method.execute();
      Assert.assertEquals(400, method.getStatusCode());
    }

  }
}
