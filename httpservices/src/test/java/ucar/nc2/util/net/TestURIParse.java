/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.junit.Assert;
import org.junit.Test;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPUtil;
import ucar.unidata.util.test.UnitTestCommon;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test HTTPUtil.parseToURI on a variety of input cases.
 */

public class TestURIParse extends UnitTestCommon
{
    static public boolean DEBUG = false;
    static public boolean DOCARON = false;

    static final String CARON = "http://localhost:8081/thredds/cdmremote/scanCdmUnitTests/formats/hdf5/grid_1_3d_xyz_aug.h5?req=data&var=HDFEOS_INFORMATION/StructMetadata\\.0";

    static final String[] filetests = {};

    static final String[] httptests = {
            "http://ucar.edu:8081/dts/test\\/fake\\.01",
            CARON,
    };

    //////////////////////////////////////////////////

    // Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    String datadir = null;
    String threddsroot = null;

    public TestURIParse()
    {
        setTitle("HTTPUtil.parseToURI tests");
    }

    @Test
    public void
    testParse() throws Exception
    {
        pass = true;
        for(int i = 0; i < httptests.length; i++) {
            boolean passthis = true;
            URI uri = null;
            try {
                uri = HTTPUtil.parseToURI(httptests[i]);
            } catch (URISyntaxException use) {
                System.err.println("Parse error: " + use.getMessage());
                if(DEBUG) use.printStackTrace(System.err);
                uri = null;
                passthis = false;
            }
	    String raw = dumpraw(uri);
            if(DEBUG) System.err.printf("raw=     |%s|%n", raw);
            System.err.printf("Test A: "
				+ "input :: actual%n"
                                    + "\t   |%s|%n"
                                    + "\t:: |%s|%n",
                                    httptests[i],dump(uri));
            if(!httptests[i].equals(dump(uri))) {
                passthis = false;
            }
	    // Second test is for idempotence of %xx form.
            try {
                uri = HTTPUtil.parseToURI(raw);
            } catch (URISyntaxException use) {
                System.err.println("Parse error: " + use.getMessage());
                if(DEBUG) use.printStackTrace(System.err);
                uri = null;
                passthis = false;
            }
            System.err.printf("Test B: "
				+ "input :: actual%n"
                                    + "\t   |%s|%n"
                                    + "\t:: |%s|%n",
                                    raw,dumpraw(uri));
            if(!raw.equals(dumpraw(uri))) {
                passthis = false;
            }
            System.err.println(passthis ? "Pass" : "Fail");
            if(!passthis) pass = false;
        }
        Assert.assertTrue("TestMisc.testURX", pass);
    }

    // Temporary to test Caron's case specifically
    @Test
    public void
    testCaron()
    {
        if(!DOCARON) return;
        try {
            try (HTTPMethod m = HTTPFactory.Get(CARON)) {
                int code = m.execute();
                Assert.assertTrue("Unexpected return code: " + code, code == 200);
            }
        } catch (Exception use) {
            use.printStackTrace();
            Assert.assertTrue("URISyntaxException", false);
        }
    }


    static protected boolean
    uriCompare(URI uri1, URI uri2)
    {
        boolean ok = true;
        ok = ok && uriPartCompare(uri1.getScheme(), uri2.getScheme());
        ok = ok && uriPartCompare(uri1.getHost(), uri2.getHost());
        ok = ok && (uri1.getPort() == uri2.getPort());
        ok = ok && uriPartCompare(uri1.getPath(), uri2.getPath());
        ok = ok && uriPartCompare(uri1.getQuery(), uri2.getQuery());
        ok = ok && uriPartCompare(uri1.getFragment(), uri2.getFragment());
        return ok;
    }

    static protected boolean
    uriCompareRaw(URI uri1, URI uri2)
    {
        boolean ok = true;
        ok = ok && uriPartCompare(uri1.getScheme(), uri2.getScheme());
        ok = ok && uriPartCompare(uri1.getHost(), uri2.getHost());
        ok = ok && (uri1.getPort() == uri2.getPort());
        ok = ok && uriPartCompare(uri1.getRawPath(), uri2.getRawPath());
        ok = ok && uriPartCompare(uri1.getRawQuery(), uri2.getRawQuery());
        ok = ok && uriPartCompare(uri1.getRawFragment(), uri2.getRawFragment());
        return ok;
    }

    static protected boolean
    uriPartCompare(String s1, String s2)
    {
        if(s1 == s2) return true;
        if(s1 == null || s2 == null) return false;
        return (s1.equals(s2));
    }

    static protected String
    dump(URI uri)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(uri.getScheme()).append("://");
        buf.append(uri.getHost());
        if(uri.getPort() >= 0) buf.append(':').append(uri.getPort());
        if(uri.getPath() != null) buf.append(uri.getPath());
        if(uri.getQuery() != null) buf.append('?').append(uri.getQuery());
        if(uri.getFragment() != null) buf.append('#').append(uri.getFragment());
        return buf.toString();
    }

    static protected String
    dumpraw(URI uri)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(uri.getScheme()).append("://");
        buf.append(uri.getHost());
        if(uri.getPort() >= 0) buf.append(':').append(uri.getPort());
        if(uri.getRawPath() != null) buf.append(uri.getRawPath());
        if(uri.getRawQuery() != null) buf.append('?').append(uri.getRawQuery());
        if(uri.getRawFragment() != null) buf.append('#').append(uri.getRawFragment());
        return buf.toString();
    }


}
