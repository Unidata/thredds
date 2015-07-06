/* Copyright */
package thredds.server.services;

import org.apache.http.client.CredentialsProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.httpservices.HTTPBasicProvider;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Admin services, needs authentications
 *
 * @author caron
 * @since 7/6/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestAdminDebug {
    static CredentialsProvider provider = new HTTPBasicProvider("caron", "secret666");

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters() {

      List<Object[]> result = new ArrayList<>(10);
      result.add(new Object[]{"admin/debug?General/showTdsContext"});
      result.add(new Object[]{"admin/dir/content/thredds/logs/"});
      result.add(new Object[]{"admin/dir/logs/"});
      result.add(new Object[]{"admin/dir/catalogs/"});
      result.add(new Object[]{"admin/spring/showControllers"});

      return result;
    }
    private static final boolean show = true;

    String url;

    public TestAdminDebug(String url) {
      this.url = url;
    }

    @Test
    public void testOpenHtml() {
      String endpoint = TestWithLocalServer.withPath(url);
      String response = TestWithLocalServer.testWithHttpGet(provider, endpoint, ContentType.html);
      if (show)
        System.out.printf("%s%n", response);
    }
}
