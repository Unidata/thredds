package ucar.nc2.time;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

/**
 * Test CalendarDateFormatter
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

  @Test
  public void shouldBeSameTime() {

    String isoCET = "2012-04-27T16:00:00+0200";
    Date cetDate = CalendarDateFormatter.isoStringToDate(isoCET);
    String isoMST = "2012-04-27T08:00:00-0600";
    Date mstDate = CalendarDateFormatter.isoStringToDate(isoMST);
    String isoUTC = "2012-04-27T14:00Z";
    Date utcDate = CalendarDateFormatter.isoStringToDate(isoUTC);
    assertEquals(mstDate.getTime(), cetDate.getTime()); //This passes -> times with offset are ok
    assertEquals(mstDate.getTime(), utcDate.getTime()); //This fails!!
  }

  @Test
  public void shouldHandleOffsetWithoutColon() {

    String isoCET = "2012-04-27T16:00:00+0200";
    Date cetDate = CalendarDateFormatter.isoStringToDate(isoCET);//We get 2012-04-19T02:00:00-0600 and is
    String isoMST = "2012-04-27T08:00:00-0600";
    Date mstDate = CalendarDateFormatter.isoStringToDate(isoMST); //Fails here, unable to create a date with 600 hours of offset!!!
    String isoUTC = "2012-04-27T14:00Z";
    Date utcDate = CalendarDateFormatter.isoStringToDate(isoUTC);

    assertEquals(mstDate.getTime(), cetDate.getTime()); //This fails because offset
    assertEquals(mstDate.getTime(), utcDate.getTime()); //This fails!!
  }


  private void testBase(String s) {
    try {
      CalendarDate result = CalendarDateFormatter.isoStringToCalendarDate(null, s);
      System.out.printf("%s == %s%n", s, result);
    } catch (Exception e) {
      System.out.printf("FAIL %s%n", s);
      e.printStackTrace();
      CalendarDateFormatter.isoStringToCalendarDate(null, s);
    }
  }

}
