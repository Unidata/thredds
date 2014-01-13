package ucar.unidata.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.util.net.HTTPFactory;
import ucar.nc2.util.net.HTTPSession;
import ucar.nc2.util.net.HttpClientManager;

import java.io.File;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 1/8/14
 */
public class TestHttp {

  @Before
  public void init() {
    CredentialsProvider provider = new MyProvider();
    HTTPSession.setGlobalCredentialsProvider(provider);
  }

  @Test
  public void testHttpSession() throws IOException {
    String prefix = "http://motherlode.ucar.edu:8081";

    HTTPSession session = HTTPFactory.newSession(prefix);

    String url1 = prefix + "/thredds/admin/log/access/";
    String contents = HttpClientManager.getContentAsString(session, url1);
    System.out.printf("%s%n", contents);

    File localFile = new File("C:/temp/testDownload.log");

    String url2 = url1 +"access.2013-12-26.log";
    HttpClientManager.copyUrlContentsToFile(session, url2, localFile);

   }

  private class MyProvider implements CredentialsProvider {

    @Override
    public void setCredentials(AuthScope authScope, Credentials credentials) {
       ;
    }

    @Override
    public Credentials getCredentials(AuthScope authScope) {
      System.out.printf("Password was asked for%n");
      return new UsernamePasswordCredentials("name", "password");
    }

    @Override
    public void clear() {

    }
  }
}
