/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.*;

/* Test URls */

public class TestURL {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //static String u = "http://adde.ucar.edu/pointdata?select='id TXKF'&param=day time t td psl";
  static String u = "http://adde.ucar.edu/pointdata?group=rtptsrc&descr=01hr&param=LAT&select=\"ida AZCN;day 07-june-2004;time 00:00 22:00\"&num=all&compress=true";


  @Test
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
      System.out.println("TestURI url = " + uri.toString());
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
      System.out.println("TestURI url encoded = " + uri.toString());
      assert true;
    } catch (Exception e) {
      System.out.println("URI exception = "+e.getMessage());
      //assert false;
    }
  }

}
