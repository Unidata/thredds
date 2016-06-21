/* Copyright */
package thredds.server.services;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.httpservices.HTTPSession;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NotJenkins;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Admin services, needs authentications
 *
 * @author caron
 * @since 7/6/2015
 */
@RunWith(Parameterized.class)
@Category({NeedsCdmUnitTest.class, NotJenkins.class})
public class TestAdminDebug
{

    static final boolean show = false;

    static final String DFALTHOST = "localhost:8080";
    static final String DFALTUSERPWD = "caron:secret666";
    static final String BADUSERPWD = "bad:worse";

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters()
    {
        List<Object[]> result = new ArrayList<>(10);
        result.add(new Object[]{"admin/debug?General/showTdsContext"});
        result.add(new Object[]{"admin/dir/content/thredds/logs/"});
        result.add(new Object[]{"admin/dir/logs/"});
        result.add(new Object[]{"admin/dir/catalogs/"});
        result.add(new Object[]{"admin/spring/showControllers"});
        return result;
    }

    ///////////////////////////////

    //List<Object[]> result = null;
    String path = null;
    String url = null;
    UsernamePasswordCredentials cred = null;

    public TestAdminDebug(String path)
    {
	this.path = path;
        String userpwd = System.getProperty("userpwd");
        String hostport = System.getProperty("host");
        if(userpwd == null) userpwd = DFALTUSERPWD;
        if(hostport == null) hostport = DFALTHOST;
        String url = "https://" + userpwd + "@" + hostport + "/" + path;
        setup(url);
    }

    void setup(String url)
    {
        //this.result = getTestParameters();
        this.url = url;
        URL u = null;
        try {
            u = new URL(url);
            if(u.getUserInfo() == null)
                throw new Exception("No user:password specified");
            cred = new UsernamePasswordCredentials(u.getUserInfo());
            HTTPSession.setGlobalCredentials(cred);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOpenHtml()
    {
        String endpoint = TestWithLocalServer.withPath(this.path);
        byte[] response = TestWithLocalServer.getContent(cred, endpoint, new int[]{200}, ContentType.html);
        if(show && response != null)
            System.out.printf("%s%n", new String(response, CDM.utf8Charset));
    }

    @Test
    public void testOpenHtmlFail()
    {
        String endpoint = TestWithLocalServer.withPath(this.path);
        byte[] response = TestWithLocalServer.getContent(cred, endpoint, new int[]{200}, ContentType.html);
        if(show && response != null)
            System.out.printf("%s%n", new String(response, CDM.utf8Charset));
    }
}
