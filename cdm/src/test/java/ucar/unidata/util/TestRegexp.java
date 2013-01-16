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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 18, 2008
 */
public class TestRegexp {
	
  public TestRegexp( ) {
    
  }

  // for one-off testing
  @Test
  public void test() {
    testOne("AG.*\\.nc", "AG2006001_2006003_ssta.nc", true);
    testOne("AG.*\\.nc", "AG2006001_2006003_ssta.hdf", false); 
    testOne("AG.*\\.nc", "PS2006001_2006003_ssta.nc", false);
  }
  
  @Test
  public void test2() {
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/AG2006001_2006003_ssta.nc", true);
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/AG2006001_2006003_ssta.hdf", false);
    testOne(".*/AG.*\\.nc$", "C:/data/roy/caron/PS2006001_2006003_ssta.nc", false);
  }
 
  @Test
  public void test3() {
    testOne(".*JU[CM]E00 EGRR.*", "JUCE00 EGRR", true);
    testOne(".*JU[^CM]E00 EGRR.*", "JUCE00 EGRR", false);
  }

  @Test
  public void test4() {
    testMatch(".*(J.....) (....) .*", "WMO JUBE99 EGRR 030000", true);
    testMatch(".*([IJ].....) (....) .*", "WMO IUBEs9 sssR 030000", true);
  }

  @Test
  public void test5() {
    testMatch("(.*)\\(see Note.*", "Software identification (see Note 2)", true);
  }

  @Test
  public void testEcmwfTable() {
    testOneLine("2 msl MSL Mean sea level pressure Pa");
    testOneLine("3 3 None Pressure tendency Pa s**-1");
//    testOneLine("4 pv PV Potential vorticity K m**2 kg**-1 s**-1");
    testOneLine("21 21 None Radar spectra (1) -");
  }

  
  private void testOneLine(String line) {
    testMatch("([^\\s]*) ([^\\s]*) ([^\\s]*) ([^\\*-]*) ([^\\s]*)?", line, true);
  }
  
  @Test
  public void testSplit() {
    String[] split = "what is  it".split("[ ]+");
    for (String s : split)
      System.out.println("("+s+")");
  }

  @Test
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
    //assert matcher.matches() == expect;
    assertEquals(expect, matcher.matches() );
  }

  @Test
  public void testGhcnm() {
    String m = "101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999";
    String p = "(\\d{11})(\\d{4})TAVG([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)"+
            "([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)(.)(.)([ \\-\\d]{5})(.)?(.)?(.)?.*";
    testMatch(p, m, true);
  }
  
  @Test
  public void testGhcnm2() {
    String m = "101603550001932TAVG 1010  1  980  1-9999    1420  1 1840  1-9999    2290  1-9999    2440  1-9999   -9999   -9999";
    String p = "(\\d{11})(\\d{4})TAVG(([ \\-\\d]{5})(.)(.)(.)){3}.*";
    testMatch(p, m, true);
  }

  @Test
  public void testGhcnmStn() {
//              1         2         3         4         5         6         7         8         9         10        11        12
//              0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
    //String m = "10160490000  35.6000   -0.6000   90.0 ORAN/ES SENIA       ALGERIA      98U  492HIxxCO10A 6WARM CROPS      B";
    String m = "20558362000  31.2000  121.4000    7.0 SHANGHAI            CHINA        23U10980FLxxCO25A 1PADDYLANDS      C";
    String p = "(\\d{11}) ([ \\.\\-\\d]{8}) ([ \\.\\-\\d]{9}) ([ \\.\\-\\d]{6}) (.{30}) ([ \\-\\d]{4})(.)([ \\-\\d]{5})(..)(..)(..)([ \\-\\d]{2})(.)(..)(.{16})(.).*";
    testMatch(p, m, true);
  }

  @Test
  public void testIgraStn() {
    String m = "JN  01001  JAN MAYEN                            70.93   -8.67    9 GL   1963 2007";
    //String m = "ID  96845  SURAKARTA PANASAN                    -7.87  110.92  104      1973 1993";
    String p = "([A-Z]{2})  (\\d{5})  (.{35}) ([ \\.\\-\\d]{6}) ([ \\.\\-\\d]{7}) ([ \\-\\d]{4}) (.)(.)(.)  ([ \\d]{4}) ([ \\d]{4})$";
    testMatch(p, m, true);
  }

  @Test
  public void testIgraPorHead() {
    String m = "#0309119891109069999  11";
    String p = "#(\\d{5})(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{4})([ \\d]{4})$";
    testMatch(p, m, true);
  }

  @Test
  public void testIgraPor() {
    String m = "20 99200 -9999    44    48-9999-9999";
    String p = "(\\d{2})([ \\-\\d]{6})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})(.)([ \\-\\d]{5})([ \\-\\d]{5})([ \\-\\d]{5})$";
    testMatch(p, m, true);
  }

  // "(\\w*)\\s*since\\s*([\\+\\-\\d]*)[ T]?([\\.\\:\\d]*)([ \\+\\-]\\S*)?$"
  @Test
  public void testDate() {
    String m = "secs since 1997-07-16T19:20+01:00";
    //           1                  2                  3             4
    String p = "(\\w*)\\s*since\\s*"+"([\\+\\-\\d]+)([ T]([\\.\\:\\d]*)([ \\+\\-]\\S*)?Z?)?$";
    testMatch(p, m, true);
  }

  @Test
  public void testIsoDate() {
    String p = CalendarDateFormatter.isodatePatternString;
    //String m = "2012-05-03 10:03:29Z";
    String m = "2012-04-27t08:00:00-0600";
    testMatch(p, m, true);
  }

  @Test
  public void testIsoDate2() {
    String p = "([\\+\\-\\d]+)([ t])([\\.\\:\\d]*)(([ \\+\\-]\\S*)?z?)?$";
    //String m = "2012-05-03 10:03:29Z";
    String m = "2012-05-03 10:03:29+03";
    testMatch(p, m, true);
  }

  @Test
  public void testFrag() {
    String m = "2011-02-09T06:00:00Z";
    String p = "([\\+\\-\\d]+)([ T]([\\.\\:\\d]*)([ \\+\\-]\\S*)?Z?)?$";
    testMatch(p, m, true);
  }

  /*

       Timestamp: one of
             DATE
             DATE CLOCK
             DATE CLOCK CLOCK
             DATE CLOCK INT
             DATE CLOCK ID
             TIMESTAMP
             TIMESTAMP INT
             TIMESTAMP ID


     ID: one of
             <id>
             "%"
             "'"
             "\""
             degree sign
             greek mu character

     <id>:
             <alpha> <alphanum>*

     <alpha>:
             [A-Za-z_]
             ISO-8859-1 alphabetic characters
             non-breaking space

     <alphanum>: one of
             <alpha>
             <digit>

     <digit>:
             [0-9]


     DATE:
             <year> "-" <month> ("-" <day>)?

     <year>:
             [+-]?[0-9]{1,4}

     <month>:
             "0"?[1-9]|1[0-2]

     <day>:
             "0"?[1-9]|[1-2][0-9]|"30"|"31"

     CLOCK:
             <hour> ":" <minute> (":" <second>)?

     TIMSTAMP:
             <year> (<month> <day>?)? "T" <hour> (<minute> <second>?)?

     <hour>:
             [+-]?[0-1]?[0-9]|2[0-3]

     <minute>:
             [0-5]?[0-9]

     <second>:
             (<minute>|60) (\.[0-9]*)?

       DATE:
             <year> "-" <month> ("-" <day>)?
     <year>:
             [+-]?[0-9]{1,4}
     <month>:
             "0"?[1-9]|1[0-2]
     <day>:
             "0"?[1-9]|[1-2][0-9]|"30"|"31"
     CLOCK:
             <hour> ":" <minute> (":" <second>)?
     TIMSTAMP:
             <year> (<month> <day>?)? "T" <hour> (<minute> <second>?)?
     <hour>:
             [+-]?[0-1]?[0-9]|2[0-3]
     <minute>:
             [0-5]?[0-9]
     <second>:
             (<minute>|60) (\.[0-9]*)?
*/
  @Test
  public void testUdunit() {
    String m = "3 secs since 1991-01-01T03:12";
    String p = "(\\d*) (\\w*) since ([+-]?[0-9]{1,4})\\-([0-9]{1,2})\\-([0-9]{1,2})[T ]([+-]?[0-9]{1,2}):([0-9]{1,2})(:([0-9]{1,2}))?.*$";
    testMatch(p, m, true);
  }
  
  @Test
  public void testUdunit2() {
    String m = "hours since 1900-1-1 0:0:0";
    String p = "(\\d*)\\s*(\\w*)\\s*since\\s*(.*)$";
    testMatch(p, m, true);
  }

   public void testCalendarDate() {
     String m = "sec since 1970-1-1 00:00:00Z";
     //String m = "1422175657634555 microsecs since 1970-1-1T0:0:0Z";
     String p = "(\\w*)\\s*since\\s*([\\+\\-\\d]+)([ t]([\\.\\:\\d]*)([ \\+\\-]\\S*)?z?)?$";
     testMatch(CalendarDateUnit.udunitPatternString, m, true);
   }

  // {7, "Geopotential height", "gpm", "ZGEO"},
   @Test
   public void testNclGrib1Table() {
    String m = "{7, \"Geopotential height\", \"gpm\", \"ZGEO\"},";
    //String p = "\\{(\\d*)\\,\\s*\"([^\"]*)\".*";
    String p = "\\{(\\d*)\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\"\\,\\s*\"([^\"]*)\".*";
    testMatch(p, m, true);
  }
  /////////////////////////////////////////////////////////

  // test pattern ps against match, show result
  private void testMatch(String ps, String match, boolean expect) {
    System.out.printf("%npattern %s%n", ps);
    System.out.printf("match    %s%n", match);

    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);

    if (matcher.matches() != expect)
        System.out.printf("No match found.%n");
    
    assertTrue( matcher.matches() == expect);
    
    System.out.printf("matches = %s %n", matcher.matches());
    if (matcher.matches()) {
      for (int i=1; i<=matcher.groupCount(); i++)
        System.out.printf(" group %d == '%s'%n",i,matcher.group(i));
    }
  }

}
