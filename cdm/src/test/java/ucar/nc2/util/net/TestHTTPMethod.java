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

import org.junit.Test;
import ucar.nc2.util.UnitTestCommon;

import java.io.InputStream;

import static junit.framework.Assert.assertTrue;

public class TestHTTPMethod extends UnitTestCommon
{

    //////////////////////////////////////////////////
    // Constants

    static final String baseurl = "http://thredds-test.ucar.edu:8081/dts";
    static String relativebaseline = "/cdm/src/test/data/ucar/nc2/util/net";

    static final String testcase = "test.01.dds";

    static final int EXPECTED = 20000;

    static {HTTPSession.TESTING = true;}

    //////////////////////////////////////////////////

    // Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    String datadir = null;
    String threddsroot = null;

    public TestHTTPMethod()
    {
        super();
        setTitle("HTTP Method tests");
    }

    public void
    testGetStream() throws Exception
    {
        HTTPSession session = null;
        HTTPMethod method = null;

        String url = baseurl + "/" + testcase;
        String baseline = threddsRoot + relativebaseline + "/" + testcase;

        System.out.println("*** Testing: HTTPMethod");
        System.out.println("*** URL: " + url);

        System.out.println("*** Testing: HTTPMethod.getResponseBodyAsStream");
        session = HTTPFactory.newSession(url);
        method = HTTPFactory.Get(session);
        method.execute();
        InputStream stream = method.getResponseBodyAsStream();
        // Read the whole thing
        byte[] buffer = new byte[EXPECTED];
        int count = stream.read(buffer);
        stream.close(); /* should close the method also */
        try {
            method.execute();
            pass = false;
        } catch (HTTPException he) {
            pass = true;
        }
        assertTrue("TestHTTPMethod.testGetStream", pass);
        session.close();
    }
    public void
    testGetStreamPartial() throws Exception
    {
        HTTPSession session = null;
        HTTPMethod method = null;

        String url = baseurl + "/" + testcase;
        String baseline = threddsRoot + relativebaseline + "/" + testcase;

        System.out.println("*** Testing: HTTPMethod");
        System.out.println("*** URL: " + url);

        System.out.println("*** Testing: HTTPMethod.getResponseBodyAsStream partial read");
        session = HTTPFactory.newSession(url);
        method = HTTPFactory.Get(session);
        method.execute();
        InputStream stream = method.getResponseBodyAsStream();
        byte[] buffer = new byte[EXPECTED];
        int count = stream.read(buffer,0,10); // partial read
        method.close();
        try {
            count = stream.read(buffer);
            pass = false;
        } catch (Throwable t) {
            pass = true;
        }
        assertTrue("TestHTTPMethod.testGetStreamPartial", pass);
        session.close();
    }
}
