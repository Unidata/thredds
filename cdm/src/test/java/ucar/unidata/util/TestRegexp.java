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


  private void testOne(String ps, String match, boolean expect) {
    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);
    System.out.printf(" match %s against %s = %s %n", ps, match, matcher.matches());
    assert matcher.matches() == expect;
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

  private void testMatch(String ps, String match, boolean expect) {
    Pattern pattern = Pattern.compile(ps);
    Matcher matcher = pattern.matcher(match);
    System.out.printf(" match %s against %s = %s %n", ps, match, matcher.matches());
    for (int i=1; i<=matcher.groupCount(); i++)
      System.out.println(" "+i+ " "+matcher.group(i));
  }

}
