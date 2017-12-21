package ucar.nc2.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.units.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * Test CalendarDateUnit
 *
 * @author caron
 * @since 3/25/11
 */
@RunWith(Parameterized.class)
public class TestCalendarDateUnit {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Parameterized.Parameters(name="{0}: {2}")
  public static Collection params() {
    Object[][] data = new Object[][] {
            {"W3CISO", null, "secs since 1997"},
            {"W3CISO", null, "secs since 1997"},
            {"W3CISO", null, "secs since 1997-07"},
            {"W3CISO", null, "secs since 1997-07-16"},
            {"W3CISO", null, "secs since 1997-07-16T19:20+01:00"},
            {"W3CISO", null, "secs since 1997-07-16T19:20:30+01:00"},
            {"ChangeoverDate", null, "secs since 1997-01-01"},
            {"ChangeoverDate", null, "secs since 1582-10-16"},
            {"ChangeoverDate", null, "secs since 1582-10-15"},
//            {"ChangeoverDate", null, "secs since 1582-10-01"},
//            {"ChangeoverDate", null, "secs since 1582-10-02"},
//            {"ChangeoverDate", null, "secs since 1582-10-03"},
//            {"ChangeoverDate", null, "secs since 1582-10-04"},
//            {"yearZero", null, "secs since 0000-01-01"},
//            {"yearZero", null, "secs since 0001-01-01"},
//            {"yearZero", null, "secs since -0001-01-01"},
//            {"yearZero", "gregorian", "secs since 0001-01-01"},
//            {"yearZero", "gregorian", "secs since -0001-01-01"},
            {"Problem", null, "days since 2008-01-01 0:00:00 00:00"},
            {"Problem", null, "seconds since 1968-05-23 00:00:00 UTC"},
            {"Problem", null, "seconds since 1970-01-01 00 UTC"},
            // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
            {"UDUnits", null, "secs since 1992-10-8 15:15:42.5 -6:00"},
            {"UDUnits", null, "secs since 1992-10-8 15:15:42.5 +6"},
//            {"UDUnits", null, "secs since 1992-10-8 15:15:42.534"},
            {"UDUnits", null, "secs since 1992-10-8 15:15:42"},
            {"UDUnits", null, "secs since 1992-10-8 15:15"},
            {"UDUnits", null, "secs since 1992-10-8 15"},
            {"UDUnits", null, "secs since 1992-10-8T15"},
            {"UDUnits", null, "secs since 1992-10-8"},
//            {"UDUnits", null, "secs since 199-10-8"},
//            {"UDUnits", null, "secs since 19-10-8"},
//            {"UDUnits", null, "secs since 1-10-8"},
//            {"UDUnits", null, "secs since +1101-10-8"},
//            {"UDUnits", null, "secs since -1101-10-8"},
            {"UDUnits", null, "secs since 1992-10-8T7:00 -6:00"},
            {"UDUnits", null, "secs since 1992-10-8T7:00 +6:00"},
            {"UDUnits", null, "secs since 1992-10-8T7 -6:00"},
            {"UDUnits", null, "secs since 1992-10-8T7 +6:00"},
            {"UDUnits", null, "secs since 1992-10-8 7 -6:00"},
            {"UDUnits", null, "secs since 1992-10-8 7 +6:00"},
            {"UDUnits", null, "days since 1992"},
            {"UDUnits", null, "hours since 2011-02-09T06:00:00Z"},
            {"UDUnits", null, "seconds since 1968-05-23 00:00:00"},
            {"UDUnits", null, "seconds since 1968-05-23 00:00:00 UTC"},
            {"UDUnits", null, "seconds since 1968-05-23 00:00:00 GMT"},
            {"UDUnits", null, "msecs since 1970-01-01T00:00:00Z"},
    };
    return Arrays.asList(data);
  }

  @Parameterized.Parameter
  public String category;

  @Parameterized.Parameter(value=1)
  public String calendar;

  @Parameterized.Parameter(value=2)
  public String datestring;

  private Date getBase(String s) throws Exception {
    Unit u = format.parse(s);
    assert u instanceof TimeScaleUnit : s;
    TimeScaleUnit tu = (TimeScaleUnit) u;
    return tu.getOrigin();
  }

  @Test
  public void testBase() throws Exception {
    Date base = getBase(datestring);

    CalendarDateUnit cdu = CalendarDateUnit.of(calendar, datestring);

    Assert.assertEquals("Difference (ms): " + (base.getTime() - cdu.getBaseDate().getTime()),
            cdu.getBaseDate(), base);
    Assert.assertEquals("Difference (ms): " + (CalendarDate.of(base).getMillis() - cdu.getBaseCalendarDate().getMillis()),
            cdu.getBaseCalendarDate(), CalendarDate.of(base));
  }

//  @Test
  public void testCoords() {
    boolean test = false;
    testCoords("days", test);
    testCoords("hours", test);
    testCoords("months", test);
    testCoords("years", test);
  }

  private void testCoords(String unitP, boolean test) {
    String unit = unitP + " since 2008-02-29";
    CalendarDateUnit cdu = CalendarDateUnit.of(null, unit);

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

//  @Test
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

//  @Test
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
