/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

import junit.framework.TestCase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.Credentials;

import org.junit.Test;
import ucar.nc2.util.net.HTTPException;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPSession;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestAuth2 extends TestCase
{
  static class Data
  {
      String url;
      CredentialsProvider provider;
      public Data(String u,CredentialsProvider p) {this.url=u; this.provider=p;}
  }
    static private Data[] cases = new Data[] {
      new Data("http://motherlode.ucar.edu:8080/thredds/dodsC/restrict/testdata/testData.nc.html",
               new CredentialsProvider() {
                   public Credentials getCredentials(AuthScheme sch, String h, int p, boolean pr)
                           throws CredentialsNotAvailableException {
                     UsernamePasswordCredentials creds
                       = new UsernamePasswordCredentials("tiggeUser","tigge");
                     System.out.printf("getCredentials called: creds=|%s| host=%s port=%d isproxy=%b authscheme=%s%n",
                                  creds.toString(),h,p,pr,sch);
                     return creds;
                   }
               }),
      new Data("https://motherlode.ucar.edu:8443/dts/b31.dds",null),
    };

  @Test
  public void testAuth2() throws Exception
  {
    boolean pass = true;
    for(Data data: cases) {
        HTTPSession session = new HTTPSession(data.url);
        if(data.provider != null)
            session.setCredentialsProvider(data.provider);
        session.setUserAgent("tdmRunner");
        HTTPSession.setGlobalUserAgent("TDM v4.3");
        HTTPMethod m = null;
        try {
          System.out.printf("url %s%n", data.url);
          m = HTTPMethod.Get(session);
          int status = m.execute();
          String s = m.getResponseAsString();
          System.out.printf("Trigger response = %d == %s%n", status, s);
          if(status != 200  && status != 404)
              pass = false;
        } catch (HTTPException e) {
          System.err.println("Fail: "+data.url);
          ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
          e.printStackTrace(new PrintStream(bos));
          e.printStackTrace();
          pass = false;
        } finally {
          if (session != null) session.close();
        }
    }
    if(pass)
      junit.framework.Assert.assertTrue("testAuth2", true);
    else
      junit.framework.Assert.assertTrue("testAuth2", false);
  }

}
