/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.util;



import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.File;

/**
 * Networking utilities.
 *
 * @author caron
 */
public class URLnaming {

  public static String escapeQuery(String urlString) throws URISyntaxException {
    urlString = urlString.trim();
      URI uri = new URI(urlString);
      return uri.toASCIIString();

  }

  /* old way

    public static String escapeQuery(String urlString) {
    urlString = urlString.trim();
    int posQ = urlString.indexOf("?");
    if ((posQ > 0) && (posQ < urlString.length() - 2)) {
      String query = urlString.substring(posQ);
      if (query.indexOf("%") < 0) { // assume that its not already encoded...
        String path = urlString.substring(0,posQ);
        try {
          urlString = path + URIUtil.encodeQuery( query);
        } catch (URIException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }
    return urlString;
  }
   */

  /**
   * This augments URI.resolve(), by also dealing with file: URIs.
   * If baseURi is not a file: scheme, then URI.resolve is called.
   * Otherwise the last "/" is found in the base, and the ref is appended to it.
   * <p> For file: baseURLS: only reletive URLS not starting with / are supported. This is
   * apparently different from the behavior of URI.resolve(), so may be trouble,
   * but it allows NcML absolute location to be specified without the file: prefix.
   * <p/>
   * Example : <pre>
   * base:     file://my/guide/collections/designfaq.ncml
   * ref:      sub/my.nc
   * resolved: file://my/guide/collections/sub/my.nc
   * </pre>
   *
   * @param baseUri     base URI as a Strng
   * @param relativeUri reletive URI, as a String
   * @return the resolved URI as a String
   */
  public static String resolve(String baseUri, String relativeUri) {
    if ((baseUri == null) || (relativeUri == null))
      return relativeUri;

    if (relativeUri.startsWith("file:"))
      return relativeUri;

    // deal with a base file URL
    if (baseUri.startsWith("file:")) {

      // the case where the reletiveURL is absolute.
      // unfortunately, we may get an Exception
      try {
        URI reletiveURI = URI.create(relativeUri);
        if (reletiveURI.isAbsolute())
          return relativeUri;
      } catch (Exception e) {
        // empty
      }

      if ((relativeUri.length() > 0) && (relativeUri.charAt(0) == '#'))
        return baseUri + relativeUri;

      if ((relativeUri.length() > 0) && (relativeUri.charAt(0) == '/'))
        return relativeUri;

      int pos = baseUri.lastIndexOf('/');
      if (pos > 0) {
        return baseUri.substring(0, pos + 1) + relativeUri;
      }
    }

    // non-file URLs

    //relativeUri = canonicalizeRead(relativeUri);
    URI reletiveURI = URI.create(relativeUri);
    if (reletiveURI.isAbsolute())
      return relativeUri;

    //otherwise let the URI class resolve it
    URI baseURI = URI.create(baseUri);
    URI resolvedURI = baseURI.resolve(reletiveURI);
    return resolvedURI.toASCIIString();
  }

  /// try to figure out if we need to add file: to the location when reading
  static public String canonicalizeRead(String location) {
    try {
      URI refURI = URI.create(location);
      if (refURI.isAbsolute())
        return location;
    } catch (Exception e) {
      return "file:" + location;
    }
    return location;
  }

  /// try to figure out if we need to add file: to the location when writing
  static public String canonicalizeWrite(String location) {
    try {
      URI refURI = URI.create(location);
      if (refURI.isAbsolute())
        return location;
    } catch (Exception e) {
      //return "file:" + location;
    }
    return "file:" + location;
  }

  public static String resolveFile(String baseDir, String filepath) {
    if (baseDir == null) return filepath;
    File file = new File(filepath);
    if (file.isAbsolute()) return filepath;
    return baseDir + filepath;
  }

  ///////////////////////////////////////////////////////////////////

  /* private static void initProtocolHandler() {
    // test setting the http protocol handler
    try {
      new java.net.URL(null, "http://motherlode.ucar.edu:8080/", new sun.net.www.protocol.http.Handler());
    } catch (java.net.MalformedURLException e) {
      e.printStackTrace();
    }

  } */

  private static void test(String uriS) {
    System.out.println(uriS);
    //uriS = URLEncoder.encode(uriS, "UTF-8");
    //System.out.println(uriS);

    URI uri = URI.create(uriS);
    System.out.println(" scheme=" + uri.getScheme());
    System.out.println(" getSchemeSpecificPart=" + uri.getSchemeSpecificPart());
    System.out.println(" getAuthority=" + uri.getAuthority());
    System.out.println(" getPath=" + uri.getPath());
    System.out.println(" getQuery=" + uri.getQuery());
    System.out.println();
  }

  public static void main(String args[]) {
    testResolve("file:/test/me/", "blank in dir", "file:/test/me/blank in dir");
  }

  public static void main2(String args[]) {
    test("file:test/dir");
    test("file:/test/dir");
    test("file://test/dir");
    test("file:///test/dir");

    test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache");
    test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache");
    test("http://localhost:8080/thredds/catalog.html?hi=lo");
  }

  private static void testResolve(String base, String rel, String result) {
    System.out.println("\nbase= " + base);
    System.out.println("rel= " + rel);
    System.out.println("resolve= " + resolve(base, rel));
    if (result != null)
      assert resolve(base, rel).equals(result);
  }

  public static void main3(String args[]) {
    testResolve("http://test/me/", "wanna", "http://test/me/wanna");
    testResolve("http://test/me/", "/wanna", "http://test/wanna");
    testResolve("file:/test/me/", "wanna", "file:/test/me/wanna");
    testResolve("file:/test/me/", "/wanna", "/wanna");  // LOOK doesnt work for URI.resolve() directly.

    testResolve("file://test/me/", "http:/wanna", "http:/wanna");
    testResolve("file://test/me/", "file:/wanna", "file:/wanna");
    testResolve("file://test/me/", "C:/wanna", "C:/wanna");
    testResolve("http://test/me/", "file:wanna", "file:wanna");
  }


  public static void main4(String args[]) {
    try {
      URL url = new URL("file:src/test/data/ncml/nc/");
      URI uri = new URI("file:src/test/data/ncml/nc/");
      File f = new File(uri);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main5(String args[]) throws URISyntaxException {
    String uriString = "http://test.opendap.org:8080/dods/dts/test.53.dods?types[0:1:9]";
    URI uri = new URI(uriString);
  }


}