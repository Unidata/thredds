package thredds.server.services;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.httpservices.HTTPSession;
import ucar.nc2.constants.CDM;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Admin services, needs authentication
 *
 * @author caron
 * @since 7/6/2015
 */
@RunWith(Parameterized.class)
public class TestAdminDebug {
    static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestWithLocalServer.class);

    static final String DEFAULT_HOST = "localhost:8443";

    // Defined in it/src/test/resources/auth/keystore
    static final String DEFAULT_USER = "tds";
    static final String DEFAULT_PASSWORD = "secret666";
    static final String DEFAULT_USERPWD = DEFAULT_USER + ":" + DEFAULT_PASSWORD;

    @Parameterized.Parameters(name = "{0}") public static List<Object[]> getTestParameters() {
        List<Object[]> result = new ArrayList<>(10);
        result.add(new Object[] { "admin/debug?General/showTdsContext" });
        result.add(new Object[] { "admin/dir/content/thredds/logs/" });
        result.add(new Object[] { "admin/dir/logs/" });
        result.add(new Object[] { "admin/dir/catalogs/" });
        result.add(new Object[] { "admin/spring/showControllers" });
        return result;
    }

    ///////////////////////////////

    String url = null;
    UsernamePasswordCredentials cred = null;

    public TestAdminDebug(String path) {
        String url = "https://" + DEFAULT_USERPWD + "@" + DEFAULT_HOST + "/thredds/" + path;
        setup(url);
    }

    void setup(String url) {
        //this.result = getTestParameters();
        this.url = url;
        try {
            URL u = new URL(url);
            if (u.getUserInfo() == null) {
                throw new Exception("No user:password specified");
            }
            cred = new UsernamePasswordCredentials(u.getUserInfo());
            HTTPSession.setGlobalCredentials(cred);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test public void testOpenHtml() {
        byte[] response = TestWithLocalServer.getContent(cred, url, new int[] { 200 }, ContentType.html);
        if (response != null) {
            logger.debug(new String(response, CDM.utf8Charset));
        }
    }

    @Test public void testOpenHtmlFail() {
        byte[] response = TestWithLocalServer.getContent(cred, url, new int[] { 200 }, ContentType.html);
        if (response != null) {
            logger.debug(new String(response, CDM.utf8Charset));
        }
    }
}
