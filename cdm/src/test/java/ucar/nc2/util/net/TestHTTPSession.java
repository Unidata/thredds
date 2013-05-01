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

import static junit.framework.Assert.assertTrue;

public class TestHTTPSession extends UnitTestCommon
{
  //////////////////////////////////////////////////

  // Define the test sets

  int passcount = 0;
  int xfailcount = 0;
  int failcount = 0;
  boolean verbose = true;
  boolean pass = false;

  String datadir = null;
  String threddsroot = null;

  public TestHTTPSession()
  {
    super();
    setTitle("HTTP Session tests");
    HTTPSession.TESTING = true;
  }

  @Test
  public void
  testAgent() throws Exception {
    String globalagent = "TestUserAgent123global";
    String sessionagent = "TestUserAgent123session";
    String url =
            "http://thredds-test.ucar.edu:8081/dts/test.01.dds";

    System.out.println("*** Testing: User Agent");
    System.out.println("*** URL: " + url);
    System.out.println("Test: HTTPSession.setGlobalUserAgent(" + globalagent + ")");
    HTTPSession.setGlobalUserAgent(globalagent);
    HTTPSession session = new HTTPSession(url);
    HTTPMethod method = HTTPMethod.Get(session);
    method.execute();
    System.out.println("Validate by examining localserver log output");

    System.out.println("Test: HTTPSession.setUserAgent(" + sessionagent + ")");
    session.setUserAgent(sessionagent);
    method = HTTPMethod.Get(session);
    method.execute();
    System.out.println("Validate by examining localserver log output");

    assertTrue("TestHTTPSession.testAgent", true);
  }
}
