/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

import junit.framework.TestCase;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.io.File;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 16, 2008
 */
public class TestDatasetURL extends TestCase {

  public TestDatasetURL(String name) {
    super(name);
  }

  public void testBlanks() {
    testResolve("file:/test/me/", "blank in dir", "file:/test/me/blank in dir");
  }

  public void testMisc() {
    test("file:test/dir");
    test("file:/test/dir");
    test("file://test/dir");
    test("file:///test/dir");

    test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache");
    test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache");
    test("http://localhost:8080/thredds/catalog.html?hi=lo");
  }

  public void testResolve() {
    testResolve("http://test/me/", "wanna", "http://test/me/wanna");
    testResolve("http://test/me/", "/wanna", "http://test/wanna");
    testResolve("file:/test/me/", "wanna", "file:/test/me/wanna");
    testResolve("file:/test/me/", "/wanna", "/wanna");  // LOOK doesnt work for URI.resolve() directly.

    testResolve("file://test/me/", "http:/wanna", "http:/wanna");
    testResolve("file://test/me/", "file:/wanna", "file:/wanna");
    testResolve("file://test/me/", "C:/wanna", "C:/wanna");
    testResolve("http://test/me/", "file:wanna", "file:wanna");
  }

  public void testReletiveFile() throws MalformedURLException, URISyntaxException {
    URL url = new URL("file:src/test/data/ncml/nc/");

    test("src/test/data/ncml/nc/");
    URI uri = new URI("src/test/data/ncml/nc/");

    test("file:/src/test/data/ncml/nc/");
    uri = new URI("file:/src/test/data/ncml/nc/");
    new File(uri); // ok

    test("file:src/test/data/ncml/nc/");
    uri = new URI("file:src/test/data/ncml/nc/");
  }

  public void testDods() throws URISyntaxException {
    String uriString = "http://test.opendap.org:8080/dods/dts/test.53.dods?types[0:1:9]";
    URI uri = new URI(uriString);
  }


  private void test(String uriS) {
    System.out.println(uriS);
    //uriS = URLEncoder.encode(uriS, "UTF-8");
    //System.out.println(uriS);

    try {
      URI uri = URI.create(uriS);
      System.out.println(" scheme=" + uri.getScheme());
      System.out.println(" getSchemeSpecificPart=" + uri.getSchemeSpecificPart());
      System.out.println(" getAuthority=" + uri.getAuthority());
      System.out.println(" getPath=" + uri.getPath());
      System.out.println(" getQuery=" + uri.getQuery());
      System.out.println(" isAbsolute=" + uri.isAbsolute());
      System.out.println(" isOpaque=" + uri.isOpaque());
      System.out.println();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private void testResolve(String base, String rel, String result) {
    System.out.println("\nbase= " + base);
    System.out.println("rel= " + rel);
    System.out.println("resolve= " + URLnaming.resolve(base, rel));
    if (result != null)
      assert URLnaming.resolve(base, rel).equals(result);
  }


}
