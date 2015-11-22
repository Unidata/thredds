/* Copyright */
package thredds.server.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.httpservices.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.NotJenkins;

/**
 * Test Admin services, needs authentications
 *
 * @author caron
 * @since 7/6/2015
 */
@RunWith(Parameterized.class)
@Category({NeedsCdmUnitTest.class, NotJenkins.class})
public class TestAdminDebug {
  static Credentials cred = new UsernamePasswordCredentials("caron", "secret666");
  static Credentials credBad = new UsernamePasswordCredentials("bad", "worse");

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

  private static final boolean show = false;

  private class MYCP implements CredentialsProvider {
    public Credentials getCredentials(AuthScope scope) {
      return new UsernamePasswordCredentials("caron", "secret666");
    }

    public void setCredentials(AuthScope scope, Credentials creds) {
    }

    public void clear() {
    }
  }

  ///////////////////////////////

  String url;

  public TestAdminDebug(String url) {
    this.url = url;
    HTTPCachingProvider.clearCache(); // clear for each test

/*    try {
      HTTPSession.setGlobalCredentialsProvider(new MYCP(), HTTPAuthSchemes.BASIC);
    } catch (HTTPException e) {
      e.printStackTrace();
    } */

  }

  @Test
  public void testOpenHtml() {
    String endpoint = TestWithLocalServer.withPath(url);
    //String response = TestWithLocalServer.testWithHttpGet(provider, endpoint, ContentType.html);
    byte[] response = TestWithLocalServer.getContent(cred, endpoint, null, ContentType.html);
    if (show && response != null)
      System.out.printf("%s%n", new String(response, CDM.utf8Charset));
  }

  @Test
  public void testOpenHtmlFail() {
    String endpoint = TestWithLocalServer.withPath(url);
    //String response = TestWithLocalServer.testWithHttpGet(provider, endpoint, ContentType.html);
    byte[] response = TestWithLocalServer.getContent(credBad, endpoint, new int[]{401, 403}, ContentType.html);
    if (show && response != null)
      System.out.printf("%s%n", new String(response, CDM.utf8Charset));
  }
}
