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

  public void testGhcnm() {
    String m = "101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999";
    String p = "(\\d{11})(\\d{4})TAVG([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)?(.)?(.)?.*";
    testMatch(p, m, true);
  }

  public void testGhcnm2() {
    String m = "101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999";
    String p = "(\\d{11})(\\d{4})TAVG(([ \\-\\d]{5})(.)(.)(.)){3}.*";
    testMatch(p, m, true);
  }

  public void testGhcnmStn() {
//              1         2         3         4         5         6         7         8         9         10        11        12
//              0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
    //String m = "10160490000  35.6000   -0.6000   90.0 ORAN/ES SENIA       ALGERIA      98U  492HIxxCO10A 6WARM CROPS      B";
    String m = "20558362000  31.2000  121.4000    7.0 SHANGHAI            CHINA        23U10980FLxxCO25A 1PADDYLANDS      C";
    String p = "(\\d{11}) ([ \\.\\-\\d]{8}) ([ \\.\\-\\d]{9}) ([ \\.\\-\\d]{6}) (.{30}) ([ \\-\\d]{4})(.)([ \\-\\d]{5})(..)(..)(..)([ \\-\\d]{2})(.)(..)(.{16})(.).*";
    testMatch(p, m, true);
  }

  public void testIgraStn() {
    String m = "JN  01001  JAN MAYEN                            70.93   -8.67    9 GL   1963 2007";
    //String m = "ID  96845  SURAKARTA PANASAN                    -7.87  110.92  104      1973 1993";
    String p = "([A-Z]{2})  (\\d{5})  (.{35}) ([ \\.\\-\\d]{6}) ([ \\.\\-\\d]{7}) ([ \\-\\d]{4}) (.)(.)(.)  ([ \\d]{4}) ([ \\d]{4})$";
    testMatch(p, m, true);
  }

  public void testIgraPorHead() {
    String m = "#0309119891109069999  11";
    String p = "#(\\d{5})(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{4})([ \\d]{4})$";
    testMatch(p, m, true);
  }

  public void testIgraPor() {
    String m = "20 99200 -9999    44    48-9999-9999";
    String p = "(\\d{2})([ \\-\\d]{6})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})([ \\-\\d]{5})([ \\-\\d]{5})$";
    testMatch(p, m, true);
  }

  // test pattern ps against match, show result
  private void testMatch(String ps, String match, boolean expect) {
    System.out.printf("match %s against %s%n%n", ps, match);

    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);

    /* boolean found = false;
    while (matcher.find()) {
        System.out.printf(" found the text \"%s\" starting at index %d and ending at index %d.%n",
            matcher.group(), matcher.start(), matcher.end());
        found = true;
    }
    if(!found)
        System.out.printf("No match found.%n"); */

    System.out.printf("%n matches = %s %n", matcher.matches());
    for (int i=1; i<=matcher.groupCount(); i++)
      System.out.printf(" group %d == '%s'%n",i,matcher.group(i));
  }

}
