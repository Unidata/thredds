/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.junit.Assert;
import thredds.util.ContentType;
import ucar.httpservices.*;
import ucar.nc2.util.IO;

import java.io.File;
import java.io.InputStream;

/**
 * Describe
 *
 * @author caron
 * @since 10/15/13
 */
public class TestWithLocalServer {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestWithLocalServer.class);
  public static String server = "http://localhost:8081/thredds/";

  public static String withPath(String path) {
    return server + StringUtils.stripStart(path, "/\\");  // Remove leading slashes from path.
  }

  public static byte[] getContent(String endpoint, int expectCode, ContentType expectContentType) {
    return getContent(null, endpoint, new int[] {expectCode}, expectContentType);
  }

  public static byte[] getContent(String endpoint, int[] expectCodes, ContentType expectContentType) {
    return getContent(null, endpoint, expectCodes, expectContentType);
  }

  public static byte[] getContent(Credentials cred, String endpoint, int[] expectCodes, ContentType expectContentType) {
    System.out.printf("req = '%s'%n", endpoint);
    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      if (cred != null) {
        session.setCredentials(cred);
      }

      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();

      if (expectCodes == null) {
        Assert.assertEquals(200, statusCode);
      } else if (expectCodes.length == 1) {
        Assert.assertEquals(expectCodes[0], statusCode);
      } else {
        boolean ok = false;
        for (int expectCode : expectCodes)
          if (expectCode == statusCode) ok = true;
        Assert.assertTrue(ok);
      }

      if (statusCode != 200) {
        logger.warn("statusCode = {} '{}'", statusCode, method.getResponseAsString());
        return null;
      }

      if (expectContentType != null) {
        Header header = method.getResponseHeader(ContentType.HEADER);
        Assert.assertEquals(expectContentType.getContentHeader().toLowerCase(), header.getValue().toLowerCase());
      }

      return method.getResponseAsBytes();

    } catch (HTTPException e) {
      logger.error("Problem with HTTP request", e);
      assert false;
    }

    return null;
  }

  public static void saveContentToFile(String endpoint, int expectCode, ContentType expectContentType, File saveTo) {
    logger.debug("req = '{}'", endpoint);
    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        logger.warn("statusCode = {} '{}'", statusCode, method.getResponseAsString());
        Assert.assertEquals(expectCode, statusCode);
        return;
      }

      Assert.assertEquals(expectCode, statusCode);

      if (expectContentType != null) {
        Header header = method.getResponseHeader(ContentType.HEADER);
        Assert.assertEquals(expectContentType.getContentHeader(), header.getValue());
      }

      InputStream content = method.getResponseAsStream(); // closing method may close stream ??
      IO.appendToFile(content, saveTo.getAbsolutePath());

    } catch (Exception e) {
      logger.error("Problem with HTTP request", e);
      assert false;
    }
  }

}
