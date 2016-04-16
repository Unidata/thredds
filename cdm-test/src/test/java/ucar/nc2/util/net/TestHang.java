/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

/**
 * Re: github issue https://github.com/Unidata/thredds/issues/431
 * Test that a single HTTPSession with many method invocations
 * does not hang properly releases the connections.
 */

package ucar.nc2.util.net;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestHang
{

    static final int NCONN = 25;

    static private HTTPSession session;

    //  static private String server = "https://rdavm.ucar.edu:8443";
//  static private String url = server + "/thredds/admin/collection/trigger?trigger=never&collection=";
    static protected final String server = "http://remotetest.unidata.ucar.edu";

    static protected final String url = server + "/dts/test.%02d";

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters()
    {
        List<Object[]> result = new ArrayList<>();
        for(int i = 0; i < NCONN; i++) {
            result.add(new Object[]{String.format(url, i)});
        }
        return result;
    }

    @BeforeClass
    public static void setupCorrect() throws HTTPException
    {
        HTTPSession sess = HTTPFactory.newSession(server);
        session = sess;
    }

    ///////////

    String fullUrl;

    public TestHang(String ds)
    {
        this.fullUrl = url + ds;
    }

    @Test
    public void testSession() throws Exception
    {

        try (HTTPMethod m = HTTPFactory.Get(session, fullUrl)) {
            System.out.printf("Connecting to %s%n", fullUrl);
            int status = m.execute();
            System.out.printf("    return from %s status= %d%n", fullUrl, status);
            Assert.assertTrue("Bad return status: " + status, status == 200 || status == 404);
        }
    }
}
