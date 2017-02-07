package thredds.server.services;

import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.nc2.constants.CDM;

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
    private static Logger logger = LoggerFactory.getLogger(TestAdminDebug.class);

    private static String urlPrefix =  "https://localhost:8443/thredds/";
    private static Credentials goodCred = new UsernamePasswordCredentials("tds", "secret666");
    private static Credentials badCred = new UsernamePasswordCredentials("bad", "worse");

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

    String path;

    public TestAdminDebug(String path) {
        this.path = path;
    }

    @Test public void testOpenHtml() {
        String endpoint = urlPrefix + path;
        byte[] response = TestWithLocalServer.getContent(goodCred, endpoint, new int[] { 200 }, ContentType.html);
        if (response != null) {
            logger.debug(new String(response, CDM.utf8Charset));
        }
    }

    @Test public void testOpenHtmlFail() {
        String endpoint = urlPrefix + path;
        byte[] response = TestWithLocalServer.getContent(badCred, endpoint, new int[] { HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_FORBIDDEN }, ContentType.html);

        if (response != null) {
            logger.debug(new String(response, CDM.utf8Charset));
        }
    }
}
