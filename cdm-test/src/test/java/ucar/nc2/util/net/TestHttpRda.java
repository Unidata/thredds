/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.util.net;

import org.apache.http.auth.UsernamePasswordCredentials;
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
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 2/17/2016.
 */

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestHttpRda
{
    static private HTTPSession session;
    static private String user = "tdm";
    static private String pass = "tdsTrig";
    static private String name = "TestHttpRda";

    static private String server = "https://rdavm.ucar.edu:8443";
    static private String url = server + "/thredds/admin/collection/trigger?trigger=never&collection=";

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters()
    {
        List<Object[]> result = new ArrayList<>();

        result.add(new Object[]{"ds628.1_anl_isentrop125"});
        result.add(new Object[]{"ds083.2_Grib1"});
        result.add(new Object[]{"ds083.2_Grib2"});
        result.add(new Object[]{"ds628.1_anl_column"});
        result.add(new Object[]{"ds628.1_anl_column125"});
        result.add(new Object[]{"ds628.1_anl_land"});
        result.add(new Object[]{"ds628.1_anl_isentrop"});


        return result;
    }

    // @BeforeClass
    public static void setupWrong() throws HTTPException
    {
        try (HTTPSession sess = HTTPFactory.newSession(server)) {
            if(user != null && pass != null)
                sess.setCredentials(new UsernamePasswordCredentials(user, pass));
            sess.setUserAgent(name);
            session = sess;
        } // LOOK closed here. introduced apparently 1/11/16 or 1/16/16
    }

    @BeforeClass
    public static void setupCorrect() throws HTTPException
    {
        HTTPSession sess = HTTPFactory.newSession(server);
        if(user != null && pass != null)
            sess.setCredentials(new UsernamePasswordCredentials(user, pass));
        sess.setUserAgent(name);
        session = sess;
    }

    ///////////
    String fullUrl;

    public TestHttpRda(String ds)
    {
        this.fullUrl = url + ds;
        //HTTPSession.setGlobalThreadCount(2);  does not work
        HTTPSession.setGlobalConnectionTimeout(15000);
    }

    @Test
    public void testSession() throws Exception
    {
        try (HTTPMethod m = HTTPFactory.Get(session, fullUrl)) {
            System.out.printf("send trigger to %s%n", fullUrl);
            System.out.flush();
            int status = m.execute();
            System.out.printf("    return from %s status= %d%n", fullUrl, status);

            Assert.assertEquals(200, status);
        }
    }
}
