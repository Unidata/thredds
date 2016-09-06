/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ucar.httpservices.HTTPUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;

public class TestDownload extends TestReify
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DEFAULTSERVER = "localhost:8081";
    static protected final String DEFAULTREIFYURL = "http://" + DEFAULTSERVER + THREDDSPREFIX + SERVLETPREFIX;

    //////////////////////////////////////////////////
    // Type Decls

    static class TestCase extends AbstractTestCase
    {
        static public String server = DEFAULTSERVER;
        static String svcdir = null;

        static public void setServerDir(String dir)
        {
            svcdir = dir;
        }

        //////////////////////////////////////////////////

        protected TestCase(String cmd, String url, String target, String... params)
        {
            super(cmd, url, target, params);
        }


        @Override
        public Map<String, String>
        getReply()
        {
            // Compute expected reply
            String replytarget = this.target;
            replytarget = HTTPUtil.canonjoin(svcdir, replytarget);
            if(replytarget == null) replytarget = "";
            Map<String, String> map = new HashMap<String, String>();
            map.put("download", replytarget);
            return map;
        }

        public String
        makeURL()
        {
            StringBuilder b = new StringBuilder();
            b.append("http://");
            b.append(server);
            b.append("/thredds");
            b.append(SERVLETPREFIX);
            b.append("/?");
            String params = ReifyUtils.toString(this.params, true,
                    "request", "format", "target", "url", "testinfo");
            b.append(params);
            return b.toString();
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected Map<String,String> serverprops = null;
    protected String serverdir = null;
    protected String serveruser = null;

    //////////////////////////////////////////////////
    // Junit test methods

    @Before
    public void setup()
            throws Exception
    {
        this.serverprops = getServerProperties(DEFAULTREIFYURL);
        this.serverdir = this.serverprops.get("downloaddir");
        this.serveruser = this.serverprops.get("username");
        if(this.serverdir != null) {
            File dir = new File(this.serverdir);
            UserPrincipal owner = Files.getOwner(dir.toPath());
            // Change permissions to allow read/write by anyone
            dir.setExecutable(true, false);
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            // clear out the download dir
            deleteTree(this.serverdir, false);
        }
        TestCase.setServerDir(this.serverdir);
        //NetcdfFile.registerIOProvider(Nc4Iosp.class);
        defineAllTestCases();
        prop_visual = true;
    }

    @Test
    public void
    testDownload()
            throws Exception
    {
        super.doAllTests();
    }

    //////////////////////////////////////////////////
    // Primary test method

    @Override
    public void doOneTest(AbstractTestCase tc)
            throws Exception
    {
        TestCase test = (TestCase) tc;
        System.out.println("Testcase: " + test.toString());
        String url = test.makeURL();

        int[] codep = new int[1];
        String s = callserver(url, codep);
        if(codep[0] != 200)
            Assert.assertTrue(String.format("httpcode=%d msg=%s", codep[0], s), false);
        Map<String, String> result = ReifyUtils.parseMap(s, ';', true);

        if(prop_visual) {
            String decoded = ReifyUtils.urlDecode(url);
            String recvd = ReifyUtils.toString(result, false);
            visual("TestReify.url:", decoded);
            visual("TestReify.sent:", url);
            visual("TestReify.received:", recvd);
        }

        if(prop_diff) {
            boolean pass = true;
            Map<String, String> testreply = test.getReply();
            String comparison = replyCompare(result, testreply);
            if(comparison != null) {
                System.err.println(comparison);
                Assert.fail("***Fail: return value mismatc");
            }
            // Verify that the file exists
            String filename = testreply.get("download");
            Assert.assertTrue("***Fail: No download file returned", filename != null);
            File f = new File(filename);
            Assert.assertTrue("***Fail: Download file does not exist: " + filename, f.exists());
            System.err.println("***Pass: Reply is identical and download file exists");
        }
    }

    //////////////////////////////////////////////////
    // Test cases

    protected void
    defineAllTestCases()
    {
        alltestcases.add(
                new TestCase("download",
                        "http://host:80/thredds/fileServer/localContent/testData.nc",
                        "nc3/testData.nc3", //baseline
                        "format=netcdf3"
                ));
        alltestcases.add(
                new TestCase("download",
                        "http://host:80/thredds/fileServer/localContent/testData.nc",
                        "nc4/testData.nc4",
                        "format=netcdf4"
                ));
        alltestcases.add(
                new TestCase("download",
                        "http://host:80/thredds/dodsC/localContent/testData.nc",
                        "testData.nc3", //baseline
                        "format=netcdf3"
                ));
        alltestcases.add(
                new TestCase("download",
                        "http://host:80/thredds/dodsC/localContent/testData.nc",
                        "testData.nc4",
                        "format=netcdf4"
                ));
    }

}
