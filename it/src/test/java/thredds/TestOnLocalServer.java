/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.auth.Credentials;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.util.ContentType;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.IO;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Utilities for running tests against the embedded TDS that is stood up during :it:test.
 *
 * @author caron
 * @since 10/15/13
 */
public class TestOnLocalServer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The URL of the local TDS, including context path and trailing slash, but excluding protocol prefix.
   * Its format is: {@code <hostname>:<port>/<contextPath>/}.
   */
  public static final String server = "localhost:8081/thredds/";

  /**
   * Construct a URL using the specified protocol and path. Its format will be: {@code <protocol>://<server>/<path>}.
   *
   * @param protocol  the protocol to prepend to {@link #server}. Any trailing slashes or colons will be removed.
   * @param path      the path to append to {@link #server}. Any leading slashes will be removed.
   * @return  a URL using the specified protocol and path.
   */
  public static String withProtocolAndPath(String protocol, String path) {
    StringBuilder sb = new StringBuilder();
    sb.append(StringUtils.stripEnd(protocol, "/\\:"));  // Remove trailing slashes and colon from protocol.
    sb.append("://");
    sb.append(server);
    sb.append(StringUtils.stripStart(path, "/\\"));     // Remove leading slashes from path.

    return sb.toString();
  }

  /**
   * Construct an HTTP URL using the specified path. Its format will be: {@code http://<server>/<path>}.
   *
   * @param path  the path to append to {@link #server}. Any leading slashes will be removed.
   * @return  an HTTP URL using the specified path.
   */
  public static String withHttpPath(String path) {
    return withProtocolAndPath("http", path);
  }

  /**
   * Construct a DODS URL using the specified path. Its format will be: {@code dods://<server>/<path>}.
   *
   * @param path  the path to append to {@link #server}. Any leading slashes will be removed.
   * @return  a DODS URL using the specified path.
   */
  public static String withDodsPath(String path) {
    return withProtocolAndPath("dods", path);
  }

  public static byte[] getContent(String endpoint, int expectCode, ContentType expectContentType) {
    return getContent(null, endpoint, new int[] {expectCode}, expectContentType);
  }

  public static byte[] getContent(String endpoint, int[] expectCodes, ContentType expectContentType) {
    return getContent(null, endpoint, expectCodes, expectContentType);
  }

  public static byte[] getContent(Credentials cred, String endpoint, int[] expectCodes, ContentType expectContentType) {
    logger.debug("req = '{}'", endpoint);

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
        Assert.assertTrue(String.format(
                "Expected one of %s, but got %s.", Arrays.toString(expectCodes), statusCode), ok);
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
