// $Id: NetworkUtils.java,v 1.5 2006/02/13 19:51:37 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

/**
 * @author caron
 * @version $Revision: 1.5 $ $Date: 2006/02/13 19:51:37 $
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
   *
   * eg : <pre>
   * base:     file://my/guide/collections/designfaq.ncml
   * ref:      sub/my.nc
   * resolved: file://my/guide/collections/sub/my.nc
   * </pre>
   * @param baseUrl base URL as a Strng
   * @param relativeUrl reletive URL, as a String
   * @return the resolved URL as a String
   */
  public static String resolve( String baseUrl, String relativeUrl) {
    if ((baseUrl == null) || (relativeUrl == null))
      return relativeUrl;

    URI refURI = URI.create(relativeUrl);
    if (refURI.isAbsolute())
      return relativeUrl;

    // deal with a file URL
    if (baseUrl.startsWith("file:")) {
      if ((relativeUrl.length() > 0) && (relativeUrl.charAt(0) == '#'))
        return baseUrl+relativeUrl;
      int pos = baseUrl.lastIndexOf('/');
      if (pos > 0) {
        return baseUrl.substring(0,pos+1) + relativeUrl;
      }
    }

    //otherwise let the URI class resolve it
    URI baseURI = URI.create(baseUrl);
    URI resolvedURI = baseURI.resolve(refURI);
    return resolvedURI.toASCIIString();
  }
  
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

  public static void main(String args[]) {
    test("file:test/dir");
    test("file:/test/dir");
    test("file://test/dir");
    test("file:///test/dir");

    test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache");
    test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache");
  }

}