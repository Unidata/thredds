/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

import java.lang.invoke.MethodHandles;
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
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
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
