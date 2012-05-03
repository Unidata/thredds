package ucar.nc2.time;

import org.junit.Test;

/**
 * Describe
 *
 * @author caron
 * @since 5/3/12
 */
public class TestCalendarDateFormatter {

  @Test
  public void testW3cIso() {
    testBase("1997");
    testBase("1997-07");
    testBase("1997-07-16");
    testBase("1997-07-16T19:20+01:00");
    testBase("1997-07-16T19:20:30+01:00");
  }

  @Test
  public void testChangeoverDate() {
    testBase("1997-01-01");
    testBase("1582-10-16");
    testBase("1582-10-15");
    testBase("1582-10-01");
    testBase("1582-10-02");
    testBase("1582-10-03");
    testBase("1582-10-04");
    //testBase("1582-10-14"); // fail
    //testBase("1582-10-06"); // fail
  }


  // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
  @Test
  public void testUdunits() {
    testBase("1992-10-8 15:15:42.5 -6:00");
    testBase("1992-10-8 15:15:42.5 +6");
    testBase("1992-10-8 15:15:42.534");
    testBase("1992-10-8 15:15:42");
    testBase("1992-10-8 15:15");
    testBase("1992-10-8 15");
    testBase("1992-10-8T15");
    testBase("1992-10-8");
    testBase("199-10-8");
    testBase("19-10-8");
    testBase("1-10-8");
    testBase("+1101-10-8");
    testBase("-1101-10-8");
    testBase("1992-10-8T7:00 -6:00");
    testBase("1992-10-8T7:00 +6:00");
    testBase("1992-10-8T7 -6:00");
    testBase("1992-10-8T7 +6:00");
    testBase("1992-10-8 7 -6:00");
    testBase("1992-10-8 7 +6:00");
  }

  private void testBase(String s) {
    try {
      CalendarDate result = CalendarDateFormatter.isoStringToCalendarDate(s);
      System.out.printf("%s == %s%n", s, result);
    } catch (Exception e) {
      System.out.printf("FAIL %s%n", s);
      e.printStackTrace();
      CalendarDateFormatter.isoStringToCalendarDate(s);
    }
  }

}
