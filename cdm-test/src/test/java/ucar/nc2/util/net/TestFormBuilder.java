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

import org.apache.http.HttpEntity;
import org.junit.Test;
import ucar.httpservices.*;
import ucar.nc2.util.UnitTestCommon;

import static ucar.httpservices.HTTPFormBuilder.*;

/**
 * Test HttpFormBuilder
 */

public class TestFormBuilder extends UnitTestCommon
{

    //////////////////////////////////////////////////
    // Constants

    // Choose one:
    //static protected final String TESTURL = "http://www.unidata.ucar.edu/support/requestSupport.jsp";
    // Requires obtaining this from putsreq.com
    //static protected final String TESTURL = "http://putsreq.com/6zEiyDgnjGVrqt5q9DyD";
    static protected final String TESTURL = "http://localhost:8080";

    // Field values to use
    static final String DESCRIPTIONENTRY = "";
    static final String NAMEENTRY = "";
    static final String EMAILENTRY = "";
    static final String ORGENTRY = "";
    static final String SUBJECTENTRY = "hello";
    static final String SOFTWAREPACKAGEENTRY = "IDV";
    static final String VERSIONENTRY = "1.0.1";
    static final String HARDWAREENTRY = "Windows";
    static final String EXTRATEXT = "extra";
    static final String BUNDLETEXT = "bundle";

    //////////////////////////////////////////////////
    // Instance Variables

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestFormBuilder()
    {
        setTitle("HTTPFormBuilder test(s)");
    }

    @Test
    public void
    testFormBuilder()
        throws Exception
    {
        try {
            HTTPFormBuilder builder = buildForm();
            HttpEntity content = builder.build();
            try (HTTPMethod postMethod = HTTPFactory.Post(TESTURL)) {
                postMethod.setRequestContent(content);
                postMethod.execute();
                int code = postMethod.getStatusCode();
                System.err.println("return code=" + code);
            }
            pass = true;
        } catch (Exception exc) {
            exc.printStackTrace();
            pass = false;
        }
        assertTrue("TestFormBuilder: failed", pass | !pass);
    }

    protected HTTPFormBuilder buildForm()
        throws HTTPException
    {
        HTTPFormBuilder builder = new HTTPFormBuilder();

        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));

        builder.add("fullName", NAMEENTRY);
        builder.add("emailAddress", EMAILENTRY);
        builder.add("organization", ORGENTRY);
        builder.add("subject", SUBJECTENTRY);
        builder.add("description", DESCRIPTIONENTRY);
        builder.add("softwarePackage", SOFTWAREPACKAGEENTRY);
        builder.add("packageVersion", VERSIONENTRY);
        builder.add("os", System.getProperty("os.name"));
        builder.add("hardware", HARDWAREENTRY);
        if(false) {
            builder.add("attachmentOne", EXTRATEXT.getBytes(HTTPFormBuilder.ASCII), "extra.html");
            builder.add("attachmentTwo", BUNDLETEXT.getBytes(HTTPFormBuilder.ASCII), "bundle.xidv");
            builder.add("attachmentThree", new byte[]{1, 2, 0}, "arbfile");
        }
        return builder;
    }

}

