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
