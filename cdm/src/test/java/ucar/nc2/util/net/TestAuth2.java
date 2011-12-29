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
  //static private String serverName = "motherlode.ucar.edu:9080";
  static private String serverName = "localhost:8080";
  static private String[] urls = new String[] {
          "http://" + serverName + "/thredds/admin/collection?trigger=true&collection=NCEP-GFS-Puerto_Rico",
          //"https://" + serverName + "/thredds/admin/collection?trigger=true&collection=NCEP-GFS-Puerto_Rico",
  };

  @Test
  public void testAuth2() throws Exception
  {
    boolean pass = true;
    for(String url: urls) {
        HTTPSession session = new HTTPSession(url);
        if(true)
            session.setCredentialsProvider(new CredentialsProvider() {
              public Credentials getCredentials(AuthScheme authScheme, String host, int port, boolean isproxy) throws CredentialsNotAvailableException {
                UsernamePasswordCredentials creds = new UsernamePasswordCredentials("", "");
                System.out.printf("getCredentials called: creds=|%s| host=%s port=%d isproxy=%b authscheme=%s%n",
                                  creds.toString(),host,port,isproxy,authScheme);
                return creds;
              }
        });
        session.setUserAgent("tdmRunner");
        HTTPSession.setGlobalUserAgent("TDM v4.3");
        HTTPMethod m = null;
        try {
          System.out.printf("url %s%n", url);
          m = HTTPMethod.Get(session, url);
          int status = m.execute();
          String s = m.getResponseAsString();
          System.out.printf("Trigger response = %d == %s%n", status, s);
        } catch (HTTPException e) {
          System.err.println("Fail: "+url);
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
