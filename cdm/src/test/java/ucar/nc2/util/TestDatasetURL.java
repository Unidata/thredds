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

import junit.framework.TestCase;
import ucar.unidata.util.test.TestDir;

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

    //test("file:C:/Program Files (x86)/Apache Software Foundation/Tomcat 5.0/content/thredds/cache");  // fail on blank char
    //test("file:C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 5.0\\content\\thredds\\cache"); // fail on blank char
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
    new URL("file:src/test/data/ncml/nc/");

    test("src/test/data/ncml/nc/");
    URI uri = new URI("src/test/data/ncml/nc/");

    test("file:/src/test/data/ncml/nc/");
    uri = new URI("file:/src/test/data/ncml/nc/");
    new File(uri); // ok

    test("file:src/test/data/ncml/nc/");
    uri = new URI("file:src/test/data/ncml/nc/");
  }

  public void testDods() throws URISyntaxException {
    String uriString = "http://"+ TestDir.dap2TestServer+"/dts/test.53.dods?types[0:1:9]";
    new URI(uriString);
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
      assert false;
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
