/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.util;

import java.net.URI;
import java.io.File;

/**
 * Networking utilities.
 *
 * @author caron
 */
public class NetworkUtils {

  public static void initProtocolHandler() {
    // test setting the http protocol handler
    try {
      new java.net.URL( null, "http://motherlode.ucar.edu:8080/", new sun.net.www.protocol.http.Handler());
    } catch (java.net.MalformedURLException e) {
      e.printStackTrace();
    }

  }

  /**
   * This augments URI.resolve(), by also dealing with base file: URIs.
   * If baseURL is not a file: scheme, then URI.resolve is called.
   * Otherwise the last "/" is found in the base, and the ref is appended to it.
   * <p> For file: baseURLS: only reletive URLS not starting with / are supported. This is
   * apparently different from the behavior of URI.resolve(), so may be trouble,
   * but it allows NcML absolute location to be specified without the file: prefix.
   *
   * Example : <pre>
   * base:     file://my/guide/collections/designfaq.ncml
   * ref:      sub/my.nc
   * resolved: file://my/guide/collections/sub/my.nc
   * </pre>
   * @param baseUrl base URL as a Strng
   * @param relativeUrl reletive URL, as a String
   * @return the resolved URL as a String
   */
  public static String resolve(String baseUrl, String relativeUrl) {
    if ((baseUrl == null) || (relativeUrl == null))
      return relativeUrl;

    relativeUrl = canonicalize( relativeUrl);
    URI refURI = URI.create(relativeUrl);
    if (refURI.isAbsolute())
      return relativeUrl;

    // deal with a base file URL
    if (baseUrl.startsWith("file:")) {
      if ((relativeUrl.length() > 0) && (relativeUrl.charAt(0) == '#'))
        return baseUrl + relativeUrl;

    if ((relativeUrl.length() > 0) && (relativeUrl.charAt(0) == '/'))
      return relativeUrl;

      int pos = baseUrl.lastIndexOf('/');
      if (pos > 0) {
        return baseUrl.substring(0, pos + 1) + relativeUrl;
      }
    }

    //otherwise let the URI class resolve it
    URI baseURI = URI.create(baseUrl);
    URI resolvedURI = baseURI.resolve(refURI);
    return resolvedURI.toASCIIString();
  }

  /// try to figure out if we need to add file: to the location
  static public String canonicalize(String location) {
    try {
      URI refURI = URI.create(location);
      if (refURI.isAbsolute())
        return location;
    } catch (Exception e)  {
      return "file:" + location;
    }
    return location;
  }

  public static String resolveFile( String baseDir, String filepath) {
    if (baseDir == null) return filepath;
    File file = new File(filepath);
    if (file.isAbsolute()) return filepath;
    return baseDir + filepath;
  }
  
  ///////////////////////////////////////////////////////////////////

  private static void test(String uriS) {
    System.out.println(uriS);
    //uriS = URLEncoder.encode(uriS, "UTF-8");
    //System.out.println(uriS);

    URI uri = URI.create(uriS);
    System.out.println(" scheme="+uri.getScheme());
    System.out.println(" getSchemeSpecificPart="+uri.getSchemeSpecificPart());
    System.out.println(" getAuthority="+uri.getAuthority());
    System.out.println(" getPath="+uri.getPath());
    System.out.println();
  }

  public static void main2(String args[]) {
    test("file:test/dir");
    test("file:/test/dir");
    test("file://test/dir");
    test("file:///test/dir");

    test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache");
    test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache");
  }

  private static void testResolve(String base, String rel, String result) {
    System.out.println("\nbase= "+base);
    System.out.println("rel= "+rel);
    System.out.println("resolve= "+resolve(base,rel));
    if (result != null)
      assert resolve(base,rel).equals(result);
  }
  public static void main(String args[]) {
    testResolve( "http://test/me/", "wanna", "http://test/me/wanna");
    testResolve( "http://test/me/", "/wanna", "http://test/wanna");
    testResolve( "file:/test/me/", "wanna", "file:/test/me/wanna");
    testResolve( "file:/test/me/", "/wanna", "/wanna");  // LOOK doesnt work for URI.resolve() directly.

    testResolve( "file://test/me/", "http:/wanna", "http:/wanna");
    testResolve( "file://test/me/", "file:/wanna", "file:/wanna");
    testResolve( "file://test/me/", "C:/wanna", "C:/wanna");
    testResolve( "http://test/me/", "file:wanna", "file:wanna");
  }


}