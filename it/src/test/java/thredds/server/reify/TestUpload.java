/* Copyright 2016, University Corporation for Atmospheric Research
   See the LICENSE.txt file for more information.
*/

package thredds.server.reify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.httpservices.*;
import ucar.unidata.util.test.category.NotJenkins;
import ucar.unidata.util.test.category.NotTravis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Random;

@Category({NotJenkins.class, NotTravis.class}) // must call explicitly in intellij
public class TestUpload extends TestReify
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String DEFAULTUPURL = TestOnLocalServer.withHttpPath(UPPREFIX);

    //////////////////////////////////////////////////
    // Type Decls

    //////////////////////////////////////////////////

    static class TestCase extends AbstractTestCase
    {
        static String uploaddir = null;

        static public void setUploadDir(String dir)
        {
            uploaddir = dir;
        }

        //////////////////////////////////////////////////

        // The form fields
        public String status;
        public byte[] file;
        public boolean overwrite;
        public String target;

        public String filename;

        //////////////////////////////////////////////////

        protected TestCase(String filename, boolean overwrite, String target)
        {
            super(null);
            this.filename = HTTPUtil.canonicalpath(filename);
            this.status = "";
            this.overwrite = overwrite;
            this.target = HTTPUtil.canonicalpath(HTTPUtil.nullify(target));
            try {
                this.file = HTTPUtil.readbinaryfile(new File(filename));
            } catch (IOException ioe) {
                throw new IllegalArgumentException(ioe);
            }
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append("{");
            buf.append("file=");
            buf.append(this.filename);
            buf.append(",");
            buf.append("overwrite=");
            buf.append(this.overwrite);
            buf.append(",");
            buf.append("target=");
            buf.append(this.target);
            buf.append("}");
            return buf.toString();
        }

        public String
        makeURL()
        {
            return DEFAULTUPURL;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected boolean once = false;

    protected String uploaddir = null;

    //////////////////////////////////////////////////

    void doonce()
            throws Exception
    {
        if(once)
            return;
        once = true;
        HTTPMethod.TESTING = true;
        HTTPSession.TESTING = true;
        getServerProperties(DEFAULTUPURL);
        if(notimplemented)
            return;

        this.uploaddir = this.serverprops.get("uploaddir");
        if(this.uploaddir == null)
            throw new Exception("Cannot get upload directory");

        File dir = makedir(this.uploaddir, true);
        TestCase.setUploadDir(this.uploaddir);
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
    testUpload()
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

        org.apache.http.HttpEntity entity = buildPostContent(test);
        String url = test.makeURL();
        String sresult = null;
        try (HTTPMethod m = HTTPFactory.Post(url)) {
            m.setRequestContent(entity);
            int code = callserver(m);
            switch (code) {
            case 200:
                // Collect the output
                byte[] byteresult = m.getResponseAsBytes();
                sresult = new String(byteresult, UTF8);
                break;
            case 401:
            case 403:
                Assert.assertTrue(String.format("Access failure: %d", code), code == 200);
                break;
            default:
                sresult = m.getResponseAsString();
                Assert.fail(String.format("httpcode=%d%n%s%n", code,sresult));
                break;
            }

        }
        if(prop_visual) {
            visual("TestUpload:", sresult);
        }

        if(prop_diff) {
            // Verify that the file exists
            String targetpath = test.target;
            File src = new File(test.filename);
            if(targetpath == null) {
                // extract the basename
                targetpath = src.getName();
            }
            StringBuilder buf = new StringBuilder();
            buf.append(this.uploaddir);
            buf.append("/");
            buf.append(targetpath);
            String abstarget = HTTPUtil.canonicalpath(buf.toString());
            File targetfile = new File(abstarget);
            Assert.assertTrue("***Fail: Upload file not created: " + abstarget, targetfile.exists());
            Assert.assertTrue("***Fail: Upload file not readable: " + abstarget, targetfile.canRead());
            // Do a byte for byte comparison
            byte[] srcbytes = readbinaryfile(src.getAbsolutePath());
            byte[] targetbytes = readbinaryfile(targetfile.getAbsolutePath());
            Assert.assertTrue(
                    String.format("***Fail: Upload file (%d bytes) and Original file (%d bytes) differ in size",
                            targetbytes.length, srcbytes.length),
                    targetbytes.length == srcbytes.length);
            for(int i = 0; i < srcbytes.length; i++) {
                if(srcbytes[i] != targetbytes[i])
                    Assert.fail("***Fail: Upload file and Source file differ at byte " + i);
            }
            System.err.println("***Pass: Upload file exists and Source and uploaded files are identical");
        }
    }

    //////////////////////////////////////////////////
    // Test cases

    protected void
    defineAllTestCases()
    {
        // Create a filein the uploadir; we will give it
        // a different target name when uploading
        File testfile = new File(this.uploaddir, "srcfile.txt");
        testfile.delete();
        try {
            testfile.createNewFile();
        } catch (IOException ioe) { /*ignore*/}
        Assert.assertTrue("Cannot write testfile: " + testfile.getAbsolutePath(),
                            testfile.canWrite());

        try {
            FileWriter fw = new FileWriter(testfile);
            // Write random ascii characters
            byte[] bytes = new byte[1000];
            char[] text = new char[bytes.length];
            new Random().nextBytes(bytes);
            for(int i = 0; i < text.length; i++) {
                text[i] = (char) (((int) bytes[i]) & 0x7f); // force to be 7 bits
            }
            fw.write(text);
            fw.close();
            alltestcases.add(
                    new TestCase(/*file=*/testfile.getAbsolutePath(), true,/*target=*/"target.txt")
            );
        } catch (IOException ioe) {
            Assert.assertTrue(String.format("Cannot write testfile: %s err=%s%n",
                    testfile.getAbsolutePath(), ioe.getMessage()),false);
        }


    }

    protected org.apache.http.HttpEntity
    buildPostContent(TestCase tc)
            throws IOException
    {
        HTTPFormBuilder builder = new HTTPFormBuilder();
        builder.add("status", tc.status);
        builder.add("file", tc.file, tc.filename);
        if(tc.overwrite)
            builder.add("overwrite", "true");
        if(tc.target != null)
            builder.add("target", tc.target);
        return builder.build();
    }
}
