/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class TestDownload extends TestReify
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DEFAULTDOWNURL = TestOnLocalServer.withHttpPath(DOWNPREFIX);

    //////////////////////////////////////////////////
    // Type Decls

    static class TestCase extends AbstractTestCase
    {
        static String downloaddir = null;

        static public void setDownloadDir(String dir)
        {
            downloaddir = dir;
        }

        //////////////////////////////////////////////////

        protected String target;
        protected Command cmd;
        protected Map<String, String> params = new HashMap<>();

        //////////////////////////////////////////////////

        protected TestCase(String cmd, String url, String target, String... params)
        {
            super(url);
            this.target = HTTPUtil.canonicalpath(target);
            this.cmd = Command.parse(cmd);
            this.params.put("request", this.cmd.name().toLowerCase());
            if(this.url != null) this.params.put("url", this.url);
            if(this.target != null) this.params.put("target", this.target);

            for(int i = 0; i < params.length; i++) {
                String[] pieces = params[i].trim().split("[=]");
                if(pieces.length == 1)
                    this.params.put(pieces[0].trim().toLowerCase(), "");
                else
                    this.params.put(pieces[0].trim().toLowerCase(), pieces[1].trim());
            }
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(this.getURL());
            boolean first = true;
            for(Map.Entry<String, String> entry : this.params.entrySet()) {
                buf.append(String.format("%s%s=%s", first ? "?" : "&",
                        entry.getKey(), entry.getValue()));
            }
            return buf.toString();
        }

        public Command getCommand()
        {
            return this.cmd;
        }

        public Map<String, String> getParams()
        {
            return this.params;
        }

        public Map<String, String>
        getReply()
        {
            // Compute expected reply
            String replytarget = this.target;
            replytarget = HTTPUtil.canonjoin(downloaddir, replytarget);
            if(replytarget == null) replytarget = "";
            Map<String, String> map = new HashMap<String, String>();
            map.put("download", replytarget);
            return map;
        }

        public String
        makeURL()
        {
            StringBuilder pathBuilder = new StringBuilder();
            pathBuilder.append(DOWNPREFIX);
            pathBuilder.append("/?");
            String params = mapToString(this.params, true,
                    "request", "format", "target", "url", "testinfo");
            pathBuilder.append(params);

            return TestOnLocalServer.withHttpPath(pathBuilder.toString());
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean once = false;

    protected String downloaddir = null;

    //////////////////////////////////////////////////

    void doonce()
            throws Exception
    {
        if(once)
            return;
        once = true;

        getServerProperties(DEFAULTDOWNURL);
        if(notimplemented)
            return; // apparently not implemented

        HTTPMethod.TESTING = true;
        this.downloaddir = this.serverprops.get("downloaddir");
        if(this.downloaddir == null)
            throw new Exception("Cannot get download directory");

        File dir = makedir(this.downloaddir, true);
        TestCase.setDownloadDir(this.downloaddir);
    }

    @Before
    public void setup()
            throws Exception
    {
        if(!once)
            doonce();
        defineAllTestCases();
    }

    //////////////////////////////////////////////////
    // Junit test methods

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
        System.err.println("Testcase: " + test.toString());
        String url = test.makeURL();
        String s = null;
        try (HTTPMethod m = HTTPFactory.Get(url)) {
            int code = callserver(m);
            s = m.getResponseAsString();
            Assert.assertTrue(String.format("httpcode=%d msg=%s", code, s), code == 200);
        }
        Map<String, String> result = parseMap(s, ';', true);
        if(prop_visual) {
            String decoded = urlDecode(url);
            String recvd = mapToString(result, false);
            visual("TestReify.url:", decoded);
            visual("TestReify.sent:", url);
            visual("TestReify.received:", recvd);
        }

        if(prop_diff) {
            boolean pass = true;
            Map<String, String> testreply = test.getReply();
            String comparison = replyCompare(result, testreply);
            Assert.assertTrue("***Fail: return value mismatch: "+comparison,comparison == null);
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
