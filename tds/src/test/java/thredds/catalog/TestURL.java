package thredds.catalog;

import junit.framework.*;
import java.net.*;

/* Test opening catalogs from web */

public class TestURL extends TestCase {
  //static String u = "http://adde.ucar.edu/pointdata?select='id TXKF'&param=day time t td psl";
  static String u = "http://adde.ucar.edu/pointdata?group=rtptsrc&descr=01hr&param=LAT&select=\"ida AZCN;day 07-june-2004;time 00:00 22:00\"&num=all&compress=true";

  public TestURL( String name) {
    super(name);
  }

  public void testURL() {
    doAll("http://adde.ucar.edu/pointdata?select='id%20TXKF'");
    doAll("http://adde.ucar.edu/test%20test2");
  }

  public void doAll( String s) {
    doURIencoded(s);
    doURI(s);
    doURL(s);
  }

  public void doURL(String u) {
    try {
      URL url = new URL(u);
      System.out.println("TestURL host = " + url.getHost());
      System.out.println("TestURL file = " + url.getFile());
      assert true;
    } catch (Exception e) {
      System.out.println("URL exception = "+e.getMessage());
      //assert false;
    }
  }

  public void doURI(String u) {
    try {
      URI uri = new URI(u);
      System.out.println("TestURI uri = " + uri.toString());
      assert true;
    } catch (Exception e) {
      System.out.println("URI exception = "+e.getMessage());
      //assert false;
    }
  }

  public void doURIencoded(String u) {
    try {
      String encoded = URLEncoder.encode(u);
      URI uri = new URI(encoded);
      System.out.println("TestURI uri encoded = " + uri.toString());
      assert true;
    } catch (Exception e) {
      System.out.println("URI exception = "+e.getMessage());
      //assert false;
    }
  }


  public static void main(String[] args) {
    TestURL fake = new TestURL("dummy");
    fake.doAll(u);
  }

}
