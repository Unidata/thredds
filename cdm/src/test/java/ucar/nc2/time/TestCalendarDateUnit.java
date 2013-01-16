package ucar.nc2.time;

import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.units.*;
import org.junit.Test;

import java.util.Date;

/**
 * Test CalendarDateUnit
 *
 * @author caron
 * @since 3/25/11
 */
public class TestCalendarDateUnit {
  DateFormatter df = new DateFormatter();
  UnitFormat format = UnitFormatManager.instance();

  /*
  http://www.w3.org/TR/NOTE-datetime.html
     Year:
      YYYY (eg 1997)
   Year and month:
      YYYY-MM (eg 1997-07)
   Complete date:
      YYYY-MM-DD (eg 1997-07-16)
   Complete date plus hours and minutes:
      YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
   Complete date plus hours, minutes and seconds:
      YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
   Complete date plus hours, minutes, seconds and a decimal fraction of a
second
      YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
   */
  @Test
  public void testW3cIso() {
    testBase("secs since 1997");
    testBase("secs since 1997-07");
    testBase("secs since 1997-07-16");
    testBase("secs since 1997-07-16T19:20+01:00");
    testBase("secs since 1997-07-16T19:20:30+01:00");
  }

  @Test
  public void testChangeoverDate() {
    testBase("secs since 1997-01-01");
    testBase("secs since 1582-10-16");
    testBase("secs since 1582-10-15");
    testBase("secs since 1582-10-01");
    testBase("secs since 1582-10-02");
    testBase("secs since 1582-10-03");
    testBase("secs since 1582-10-04");
    //testBase("secs since 1582-10-14"); // fail
    //testBase("secs since 1582-10-06"); // fail
  }

  @Test
  public void testyearZero() {
    testCalendar(null, "secs since 0000-01-01");
    testCalendar(null, "secs since 0001-01-01");
    testCalendar(null, "secs since -0001-01-01");
    testCalendar("gregorian", "secs since 0001-01-01");
    testCalendar("gregorian", "secs since -0001-01-01");
  }

  @Test
  public void problem() {
    testCalendarOnly(null, "sec since 1970-1-1 00:00:00Z");
  }

  // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
  @Test
  public void testUdunits() {
    testBase("secs since 1992-10-8 15:15:42.5 -6:00");
    testBase("secs since 1992-10-8 15:15:42.5 +6");
    testBase("secs since 1992-10-8 15:15:42.534");
    testBase("secs since 1992-10-8 15:15:42");
    testBase("secs since 1992-10-8 15:15");
    testBase("secs since 1992-10-8 15");
    testBase("secs since 1992-10-8T15");
    testBase("secs since 1992-10-8");
    testBase("secs since 199-10-8");
    testBase("secs since 19-10-8");
    testBase("secs since 1-10-8");
    testBase("secs since +1101-10-8");
    testBase("secs since -1101-10-8");
    testBase("secs since 1992-10-8T7:00 -6:00");
    testBase("secs since 1992-10-8T7:00 +6:00");
    testBase("secs since 1992-10-8T7 -6:00");
    testBase("secs since 1992-10-8T7 +6:00");
    testBase("secs since 1992-10-8 7 -6:00");
    testBase("secs since 1992-10-8 7 +6:00");

    testBase("days since 1992");
    testBase("hours since 2011-02-09T06:00:00Z");
    testBase("seconds since 1968-05-23 00:00:00");
    testBase("seconds since 1968-05-23 00:00:00 UTC");
    testBase("seconds since 1968-05-23 00:00:00 GMT");
  }

  @Test
  public void testProblem()  {
    testBase("seconds since 1968-05-23 00:00:00 UTC");
  }

  private void testCoord(String unit) {
    for (Calendar cal : Calendar.values())
      testCoord(cal, unit);
  }

  private void testCoord(Calendar cal, String unit) {
    if (cal == Calendar.gregorian) {
      Date date = DateUnit.getStandardDate(unit);
      System.out.printf("     udunits %s == %s%n", unit, CalendarDateFormatter.toDateTimeStringISO(date));
      //assert date.equals(cd.getDateTime().toDate());
    }

    CalendarDate cd = CalendarDate.parseUdunits(cal.toString(), unit);
    System.out.printf("CalendarDate (%10s) %s == %s%n", cal, unit, CalendarDateFormatter.toDateTimeStringISO(cd));

    if (cal == Calendar.none)
      System.out.println("HEY");
  }

  public void testUU() throws Exception {
    Unit uu = testUU("msecs since 1970-01-01T00:00:00Z");
  }


  public Unit testUU(String s) throws Exception {
    Unit u = format.parse(s);
    assert u instanceof TimeScaleUnit;
    TimeScaleUnit tu = (TimeScaleUnit) u;
    Date base = tu.getOrigin();

    System.out.printf("%s == %s%n", s, df.toDateTimeStringISO(base));
    return u;
  }

  private void testBase(String s) {

    Date base = null;
    try {
      Unit u = format.parse(s);
      assert u instanceof TimeScaleUnit : s;
      TimeScaleUnit tu = (TimeScaleUnit) u;
      base = tu.getOrigin();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    CalendarDateUnit cdu = CalendarDateUnit.of(null, s);

    System.out.printf("%s == %s (joda %s) == %s (udunit) %n", s, cdu, cdu.getCalendar(), df.toDateTimeStringISO(base));

    if (!base.equals(cdu.getBaseDate())) {
      System.out.printf("  BAD compare CDU and udunits '%s' == %d != %d (diff = %d)%n", s, cdu.getBaseDate().getTime(), base.getTime(), cdu.getBaseDate().getTime() - base.getTime());
    }
  }

  private void testCalendar(String cal, String s) {

    Date base = null;
    try {
      Unit u = format.parse(s);
      assert u instanceof TimeScaleUnit : s;
      TimeScaleUnit tu = (TimeScaleUnit) u;
      base = tu.getOrigin();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    CalendarDateUnit cdu;
    try {
     cdu = CalendarDateUnit.of(cal, s);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.printf("%s == %s (joda %s) == %s (udunit) %n", s, cdu, cdu.getCalendar(), df.toDateTimeStringISO(base));
    if (!base.equals(cdu.getBaseDate()))
      System.out.printf("  BAD diff = %d%n", cdu.getBaseDate().getTime() - base.getTime());
  }

  private void testCalendarOnly(String cal, String s) {

    CalendarDateUnit cdu;
    try {
     cdu = CalendarDateUnit.of(cal, s);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.printf("%s == %s (joda %s) %n", s, cdu, cdu.getCalendar());
  }

  @Test
  public void testCoords() {
    boolean test = false;
    testCoords("days", test);
    testCoords("hours", test);
    testCoords("months", test);
    testCoords("years", test);
  }

  private void testCoords(String unitP, boolean test) {
    String unit = unitP + " since 2009-02-29";
    CalendarDateUnit cdu = null;
    try {
      cdu = CalendarDateUnit.of(null, unit);
    } catch (Exception e) {
      System.out.printf("%s%n", e);
      return;
    }

    for (int i=0; i<13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      if (test) testDate(i + " "+ unit);
    }
    System.out.printf("%n");
  }

  private void testDate(String udunits) {

    Date uddate = DateUnit.getStandardDate(udunits);
    CalendarDate cd = CalendarDate.parseUdunits(null, udunits);

    if (!uddate.equals(cd.toDate())) {
      System.out.printf("  BAD %s == %s != %s (diff = %d)%n", udunits, df.toDateTimeString(uddate), cd, cd.toDate().getTime() - uddate.getTime());
    }
  }

  @Test
  public void testCoordsByCalendarField() {
    boolean test = false;
    testCoordsByCalendarField("calendar days", test);
    testCoordsByCalendarField("calendar hours", test);
    testCoordsByCalendarField("calendar months", test);
    testCoordsByCalendarField("calendar years", test);
  }

  private void testCoordsByCalendarField(String unitP, boolean test) {
    String unit = unitP + " since 2008-02-29";
    CalendarDateUnit cdu = CalendarDateUnit.of(null, unit);
    for (int i=0; i<15; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%2d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      if (test) testDate(i + " "+ unit);
    }

    for (int i=0; i<13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i*10);
      System.out.printf("%2d %s == %s%n", i*10, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      if (test) testDate(i + " "+ unit);
    }
    System.out.printf("%n");
  }

  @Test
  public void testMinMax() {
    CalendarDate max = CalendarDate.of(Long.MAX_VALUE);
    System.out.printf("CalendarDate%n");
    System.out.printf(" max = %s%n", max);
    CalendarDate min = CalendarDate.of(Long.MIN_VALUE);
    System.out.printf(" min = %s%n", min);

    System.out.printf("%nDate%n");
    Date d = new Date(Long.MAX_VALUE);
    System.out.printf("max = %s%n", d);
    Date ds = new Date(Long.MIN_VALUE);
    System.out.printf("min = %s%n", ds);
  }

  @Test
  public void testBig() {
    CalendarDateUnit cdu = CalendarDateUnit.of(null, "years since 1970-01-01");
    long val = 50*1000*1000;
    CalendarDate cd = cdu.makeCalendarDate(val);
    System.out.printf("%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));

    cdu = CalendarDateUnit.of(null, "calendar years since 1970-01-01");
    cd = cdu.makeCalendarDate(val);
    System.out.printf("%n%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
  }

}
