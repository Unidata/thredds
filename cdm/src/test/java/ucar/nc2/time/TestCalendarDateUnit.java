package ucar.nc2.time;

import junit.framework.TestCase;
import ucar.nc2.units.DateFormatter;
import ucar.units.*;

import java.util.Date;

/**
 * Test CalendarDateUnit
 *
 * @author caron
 * @since 3/25/11
 */
public class TestCalendarDateUnit extends TestCase {
  DateFormatter df = new DateFormatter();
  UnitFormat format = UnitFormatManager.instance();

  public TestCalendarDateUnit( String name) {
    super(name);
  }

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
  public void testW3cIso() {
    testOne("secs since 1997");
    testOne("secs since 1997-07");
    testOne("secs since 1997-07-16");
    testOne("secs since 1997-07-16T19:20+01:00");
    testOne("secs since 1997-07-16T19:20:30+01:00");
  }

  // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
  public void testUdunits() {
    testOne("secs since 1992-10-8 15:15:42.5 -6:00");
    testOne("secs since 1992-10-8 15:15:42.5 +6");
    testOne("secs since 1992-10-8 15:15:42.534");
    testOne("secs since 1992-10-8 15:15:42");
    testOne("secs since 1992-10-8 15:15");
    testOne("secs since 1992-10-8 15");
    testOne("secs since 1992-10-8T15");
    testOne("secs since 1992-10-8");
    testOne("secs since 199-10-8");
    testOne("secs since 19-10-8");
    testOne("secs since 1-10-8");
    testOne("secs since +1101-10-8");
    testOne("secs since -1101-10-8");
    testOne("secs since 1992-10-8T7:00 -6:00");
    testOne("secs since 1992-10-8T7:00 +6:00");
    testOne("secs since 1992-10-8T7 -6:00");
    testOne("secs since 1992-10-8T7 +6:00");
    testOne("secs since 1992-10-8 7 -6:00");
    testOne("secs since 1992-10-8 7 +6:00");
    testOne("secs since 0000-01-01"); // Coards climatology

    testOne("days since 1992");
    testOne("hours since 2011-02-09T06:00:00Z");
    testOne("seconds since 1968-05-23 00:00:00 UTC");
    testOne("seconds since 1968-05-23 00:00:00 GMT");
  }

  public void testProblem()  {
    testOne("msecs since 1970-01-01T00:00:00Z");
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

  public void testOne(String s) {

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

    System.out.printf("%s == %s == %s%n", s, cdu, df.toDateTimeStringISO(base));

    if (!base.equals(cdu.getBaseDate())) {
      System.out.printf("  BAD %s == %d != %d (diff = %d)%n", s, cdu.getBaseDate().getTime(), base.getTime(), cdu.getBaseDate().getTime() - base.getTime());
    }
  }


}
