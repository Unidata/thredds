package ucar.nc2.time;

import org.joda.time.DateTime;
import org.junit.Test;

/**
 * Describe
 *
 * @author caron
 * @since 12/4/12
 */
public class TestCalendarDate {

  @Test
  public void testDateTimeFields() {
    //   public static CalendarDate of(Calendar cal, int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute) {
    CalendarDate cd;
    try {
      cd = CalendarDate.of(null, 1,1,1,1,1,1);
    } catch (Exception e) {
      assert false;
    }
    try {
      cd = CalendarDate.of(null, 1,0,1,1,1,1);
    } catch (Exception e) {
      System.out.printf("%s%n", e.getMessage()); // monthOfYear must be in the range [1,12]
      assert true;
    }
    try {
      cd = CalendarDate.of(null, 1,1,0,1,1,1); // dayOfMonth must be in the range [1,31]
    } catch (Exception e) {
      System.out.printf("%s%n", e.getMessage());
      assert true;
    }
    cd = CalendarDate.of(null, 1,1,1,0,1,1);
    cd = CalendarDate.of(null, 1,1,1,1,0,1);
    cd = CalendarDate.of(null, 1,1,1,1,1,0);
  }

}
