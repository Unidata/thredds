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

package opendap.test;

import org.junit.Test;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.util.EscapeStrings;

public class TestEncode {
  @Test
  public void testURLEncode() throws Exception {
    String legal = "";
    String illegal = "";
    // Check for url legal/illegal characters
    for (char c : " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray()) {
      String url = "http://localhost:8080/thredds/" + c;
      try {
        try (HTTPMethod cmd = HTTPFactory.Get(url)) {
          legal += c;
        }
      } catch (Exception e) {
        //System.err.printf("fail: c=|%c|\t%s\n", c, e.toString());
        illegal += c;
      }
    }
      System.out.println("legal url characters = |"+legal+"|");
      System.out.println("illegal url characters = |"+illegal+"|");
  }

  @Test
  public void testQueryEncode() throws Exception {
    String legal = "";
    String illegal = "";
    // Check for url legal/illegal characters
    for (char c : " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray()) {
      String url = "http://localhost:8080/thredds/?" + c;
      try {
        try (HTTPMethod cmd = HTTPFactory.Get(url)) {
          legal += c;
        }
      } catch (Exception e) {
        //System.err.printf("fail: c=|%c|\t%s\n", c, e.toString());
        illegal += c;
      }
    }
      System.out.println("legal query characters = |"+legal+"|");
      System.out.println("illegal query characters = |"+illegal+"|");
    }

  @Test
  public void testOGC() {
      EscapeStrings.testOGC();
  }

  public static void testB(String x) {
    System.out.printf("org ==   %s%n", x);
    System.out.printf("esc ==   %s%n", EscapeStrings.backslashEscape(x, ".\\"));
    System.out.printf("unesc == %s%n%n", EscapeStrings.backslashUnescape(EscapeStrings.backslashEscape(x, ".\\")));
    assert x.equals(EscapeStrings.backslashUnescape(EscapeStrings.backslashEscape(x, ".\\")));
  }

  @Test
  public void testBackslashEscape() {
    testB("var.name");
    testB("var..name");
    testB("var..na\\me");
    testB(".var..na\\me");
    testB(".var.\\.na\\me");
  }
}
