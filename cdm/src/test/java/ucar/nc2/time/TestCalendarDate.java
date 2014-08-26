package ucar.nc2.time;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ucar.nc2.time.CalendarPeriod.Field;

/**
 * CalendarDate testing
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

   @Test
   public void testAddReturnsClosestDate() {  // from https://github.com/jonescc
     String baseDate = "1950-01-01";
     double valueInMillisecs = 2025829799999.99977;
     String expectedResult = "2014-03-13T02:30:00Z";

     assertAddReturnsExpectedDate(baseDate, valueInMillisecs, Field.Millisec, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_SECOND, Field.Second, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_MINUTE, Field.Minute, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_HOUR, Field.Hour, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_DAY, Field.Day, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_MONTH, Field.Month, expectedResult);
     assertAddReturnsExpectedDate(baseDate, valueInMillisecs/CalendarDate.MILLISECS_IN_YEAR, Field.Year, expectedResult);
   }

   private void assertAddReturnsExpectedDate(String baseDate, double value, Field units, String expectedResult) {
     CalendarDate base = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, baseDate);
     CalendarDate result = base.add(value, units);
     assertEquals(units.toString(), expectedResult, CalendarDateFormatter.toDateTimeStringISO(result));
   }

}
