/* Copyright */
package thredds.tds;

import org.junit.Assert;
import org.junit.Test;
import thredds.TestWithLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;

/**
 * Describe
 *
 * @author caron
 * @since 4/21/2015
 */
public class TestRestrictNoAuth {

  @Test
  public void testFailNoAuth() {
    String endpoint = TestWithLocalServer.withPath("/dodsC/testRestrictedDataset/testData2.nc.dds");
    System.out.printf("testRestriction req = '%s'%n", endpoint);

    try (HTTPSession session = new HTTPSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();

      Assert.assertEquals(401, statusCode);

    } catch (ucar.httpservices.HTTPException e) {

      System.out.printf("Should return 401 err=%s%n", e.getMessage());
      assert false;

    } catch (Exception e) {

      e.printStackTrace();
      assert false;
    }
  }
}
