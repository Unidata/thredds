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

package ucar.unidata.util;

import junit.framework.TestCase;
import java.util.regex.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 18, 2008
 */
public class TestRegexp extends TestCase {
  public TestRegexp( String name ) {
    super( name );
  }

  // for one-off testing
  public void test() {
    testOne("AG.*\\.nc", "AG2006001_2006003_ssta.nc", true);
    testOne("AG.*\\.nc", "AG2006001_2006003_ssta.hdf", false); 
    testOne("AG.*\\.nc", "PS2006001_2006003_ssta.nc", false);
  }

  public void test2() {
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/AG2006001_2006003_ssta.nc", true);
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/AG2006001_2006003_ssta.hdf", false);
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/PS2006001_2006003_ssta.nc", false);
  }

  public void test3() {
    testOne(".*JU[CM]E00 EGRR.*", "JUCE00 EGRR", true);
    testOne(".*JU[^CM]E00 EGRR.*", "JUCE00 EGRR", false);
  }

  public void test4() {
    testMatch(".*(J.....) (....) .*", "WMO JUBE99 EGRR 030000", true);
    testMatch(".*([IJ].....) (....) .*", "WMO IUBEs9 sssR 030000", true);
  }

  public void test5() {
    testMatch("(.*)\\(see Note.*", "Software identification (see Note 2)", true);
  }

  public void testSplit() {
    String[] split = "what is  it".split("[ ]+");
    for (String s : split)
      System.out.println("("+s+")");
  }

  public void testEnd() {
    testMatch(".*\\.nc", "yomama.nc", true);
    testMatch(".*\\.nc", "yomamanc", false);
    testMatch(".*\\.nc", "yomama.nc.stuff", false);
    testMatch(".*\\.nc$", "yomama.nc.stuff", false);
  }

  // test pattern ps against match, test expected result
  private void testOne(String ps, String match, boolean expect) {
    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);
    System.out.printf(" match %s against %s = %s %n", ps, match, matcher.matches());
    assert matcher.matches() == expect;
  }

  // test pattern ps against match, show result
  private void testMatch(String ps, String match, boolean expect) {
    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);
    System.out.printf(" match %s against %s = %s %n", ps, match, matcher.matches());
    for (int i=1; i<=matcher.groupCount(); i++)
      System.out.println(" "+i+ " "+matcher.group(i));
  }

}
